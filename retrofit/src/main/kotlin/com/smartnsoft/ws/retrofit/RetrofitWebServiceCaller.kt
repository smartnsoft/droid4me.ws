package com.smartnsoft.ws.retrofit

import android.support.annotation.WorkerThread
import com.smartnsoft.droid4me.cache.Values
import com.smartnsoft.droid4me.log.Logger
import com.smartnsoft.droid4me.log.LoggerFactory
import com.smartnsoft.droid4me.ws.WebServiceClient
import okhttp3.*
import retrofit2.Call
import retrofit2.Converter
import retrofit2.Retrofit
import java.io.File
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 *
 * @author David Fournier
 * @since 2017.10.12
 */

@Suppress("UNCHECKED_CAST")
abstract class RetrofitWebServiceCaller<out API>(api: Class<API>,
                                                 baseUrl: String,
                                                 private val connectTimeout: Long = CONNECT_TIMEOUT,
                                                 private val readTimeout: Long = READ_TIMEOUT,
                                                 private val writeTimeout: Long = WRITE_TIMEOUT,
                                                 private val useBuiltInCache: Boolean = true,
                                                 private val defaultCachePolicyType: CachePolicyType = CachePolicyType.ONLY_NETWORK,
                                                 private val defaultCacheRetentionTimeInSeconds: Int? = null,
                                                 private val defaultAllowedTimeExpiredCacheInSeconds: Int? = null,
                                                 private val converterFactories: Array<Converter.Factory> = emptyArray())
{

  enum class CachePolicyType
  {
    ONLY_NETWORK, // Fetch to network, or fail
    ONLY_CACHE, // Fetch in cache, or fail
    NETWORK_THEN_CACHE, // Fetch to network, then fetch in cache, or fail
    CACHE_THEN_NETWORK, // Fetch in cache, then fetch to network, or fail
    SERVER // Use the cache control of the server headers
  }

  companion object
  {
    private const val CUSTOM_CACHE_URL_PREFIX = "https://customkeycache.smart/"

    private const val ONLY_CACHE_UNSATISFIABLE_ERROR_CODE = 504

    const val CONNECT_TIMEOUT = 10 * 1000L        // 10 s

    const val READ_TIMEOUT = 10 * 1000L        // 10 s

    const val WRITE_TIMEOUT = 10 * 1000L        // 10 s

    const val CACHE_SIZE = 10 * 1024 * 1024L // 10 Mb
  }

  // This class is instantiated only once and does not leak as RetrofitWebServiceCaller is a Singleton.
  inner class CachePolicy(val cachePolicyType: CachePolicyType = defaultCachePolicyType,
                          val cacheRetentionPolicyInSeconds: Int? = defaultCacheRetentionTimeInSeconds,
                          val allowedTimeExpiredCacheInSeconds: Int? = defaultAllowedTimeExpiredCacheInSeconds,
                          val customKey: String? = null)

  // This class is instantiated only once and does not leak as RetrofitWebServiceCaller is a Singleton.
  // So it is OK to declare it `inner`, to pass the `isConnected` boolean.
  inner class AppCacheInterceptor(private val shouldReturnErrorResponse: Boolean = false)
    : Interceptor
  {
    private val log: Logger by lazy {
      LoggerFactory.getInstance(AppCacheInterceptor::class.java)
    }

    override fun intercept(chain: Interceptor.Chain): Response?
    {
      val originalRequestUrl = chain.request().url()
      val cachePolicy = chain.request().tag() as? RetrofitWebServiceCaller<API>.CachePolicy
      val cachePolicyType = cachePolicy?.cachePolicyType

      if (cachePolicyType != null)
      {
        val request = buildRequest(chain.request(), cachePolicy)
        var firstTry: Response? = null

        try
        {
          firstTry = chain.proceed(request)
        }
        catch (exception: java.lang.Exception)
        {
          val errorMessage = "Call of ${chain.request().method()} to ${chain.request().url()} with cache policy ${cachePolicyType.name} failed."

          debug(errorMessage)
        }

        // Fails fast if the connectivity is known to be lost
        if (hasConnectivity().not())
        {
          if (cachePolicyType == CachePolicyType.ONLY_NETWORK)
          {
            val errorMessage = "Call of ${chain.request().method()} to ${chain.request().url()} with cache policy ${cachePolicyType.name} failed because the network is not connected."

            debug(errorMessage)

            throw WebServiceClient.CallException(errorMessage, UnknownHostException())
          }
        }

        val secondRequest: Request
        when
        {
          (firstTry == null || firstTry.code() == ONLY_CACHE_UNSATISFIABLE_ERROR_CODE) && cachePolicyType == CachePolicyType.CACHE_THEN_NETWORK ->
          {
            debug("Call of ${request.method()} to ${request.url()} with cache policy ${cachePolicyType.name} failed to find a cached response. Trying call to network.")

            // Fails fast if the connectivity is known to be lost
            if (hasConnectivity().not())
            {
              val errorMessage = "Call of ${chain.request().method()} to ${chain.request().url()} with cache policy ${cachePolicyType.name} failed because the network is not connected."

              debug(errorMessage)

              throw WebServiceClient.CallException(errorMessage, UnknownHostException())
            }

            secondRequest = buildNetworkRequest(request, cachePolicy, originalRequestUrl)
          }
          (firstTry == null || firstTry.isSuccessful.not()) && cachePolicyType == CachePolicyType.NETWORK_THEN_CACHE                            ->
          {
            debug("Call of ${request.method()} to ${request.url()} with cache policy ${cachePolicyType.name} failed to find a network response. Trying call to cache.")

            secondRequest = buildCacheRequest(request, cachePolicy)
          }
          (firstTry == null || firstTry.code() == ONLY_CACHE_UNSATISFIABLE_ERROR_CODE) && cachePolicyType == CachePolicyType.ONLY_CACHE         ->
          {
            debug("Call of ${request.method()} to ${request.url()} with cache policy ${cachePolicyType.name} failed to find a cached response. Failing.")

            throw Values.CacheException(firstTry?.message(), Exception())
          }
          (firstTry == null || firstTry.isSuccessful.not())                                                                                     ->
          {
            debug("Call of ${request.method()} to ${request.url()} with cache policy $cachePolicyType failed to find a response. Failing.")

            return onStatusCodeNotOk(firstTry)
          }
          else                                                                                                                                  ->
          {
            debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy $cachePolicyType successful.")

            return firstTry
          }
        }

        chain.proceed(secondRequest).also { secondTry ->
          return when
          {
            cachePolicyType == CachePolicyType.NETWORK_THEN_CACHE && secondTry.code() == ONLY_CACHE_UNSATISFIABLE_ERROR_CODE ->
            {
              debug("Second call of ${request.method()} to ${request.url()} with cache policy ${cachePolicyType.name} failed to find a cached response. Failing.")

              onStatusCodeNotOk(secondTry)
            }
            secondTry.isSuccessful.not()                                                                                     ->
            {
              debug("Second call of ${secondTry.request().method()} to ${secondTry.request().url()} with cache policy ${cachePolicyType.name} failed to find a network response. Failing.")

              onStatusCodeNotOk(secondTry)
            }
            else                                                                                                             ->
            {
              debug("Second call of ${request.method()} to ${request.url()} with cache policy ${cachePolicyType.name} was successful.")

              secondTry
            }
          }
        }
      }

      throw IllegalStateException("Cache Policy is malformed")
    }

    @Throws(WebServiceClient.CallException::class)
    fun onStatusCodeNotOk(response: Response?): Response?
    {
      if (response != null && shouldReturnErrorResponse.not())
      {
        throw WebServiceClient.CallException(response.message(), response.code())
      }
      else
      {
        return response
      }
    }

    private fun debug(message: String)
    {
      if (log.isDebugEnabled)
      {
        log.debug(message)
      }
    }

  }

  inner class NetworkCacheInterceptor : Interceptor
  {
    override fun intercept(chain: Interceptor.Chain): Response?
    {
      val cachePolicy = chain.request().tag() as? RetrofitWebServiceCaller<API>.CachePolicy
      val cachePolicyType = cachePolicy?.cachePolicyType

      if (cachePolicyType != null)
      {
        return rewriteResponse(chain.proceed(chain.request()), cachePolicy)
      }

      throw IllegalStateException("Cache Policy is malformed")
    }
  }

  protected open val log: Logger by lazy {
    LoggerFactory.getInstance(RetrofitWebServiceCaller::class.java)
  }

  protected open val service: API by lazy {
    val serviceBuilder = Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .client(this.httpClient)

    converterFactories.forEach { converterFactory ->
      serviceBuilder.addConverterFactory(converterFactory)
    }

    return@lazy serviceBuilder.build().create(api)
  }

  private var isConnected = true

  private val httpClient: OkHttpClient by lazy {
    computeHttpClient()
  }

  private var cacheDir: File? = null

  private var cacheSize: Long = RetrofitWebServiceCaller.CACHE_SIZE

  abstract fun <T> mapResponseToObject(responseBody: String?, clazz: Class<T>): T?

  open fun hasConnectivity(): Boolean =
      isConnected

  open fun setConnectivity(isConnected: Boolean)
  {
    this.isConnected = isConnected
  }

  open fun computeHttpClient(): OkHttpClient
  {
    val okHttpClientBuilder = OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
        .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
        .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)

    setupAuthenticator()?.also { authenticator ->
      okHttpClientBuilder.authenticator(authenticator)
    }

    setupNetworkInterceptors()?.forEach { interceptor ->
      okHttpClientBuilder.addNetworkInterceptor(interceptor)
    }

    setupAppInterceptors()?.forEach { interceptor ->
      okHttpClientBuilder.addInterceptor(interceptor)
    }

    if (useBuiltInCache)
    {
      okHttpClientBuilder.addNetworkInterceptor(NetworkCacheInterceptor())
      okHttpClientBuilder.addInterceptor(AppCacheInterceptor())
    }

    cacheDir?.also { cacheDirectory ->
      cacheDirectory.setReadable(true)
      okHttpClientBuilder.cache(Cache(File(cacheDirectory, "http-cache"), cacheSize))
    }

    return okHttpClientBuilder.build()
  }

  /**
   * Method to setup an OkHttp3 Authenticator to use.
   *
   * @return    the Authenticator
   */
  open fun setupAuthenticator(): Authenticator?
  {
    return null
  }

  /**
   * Method to setup AppInterceptors (= always intercept).
   *
   * @return    the list of AppInterceptors to use
   */
  open fun setupAppInterceptors(): List<Interceptor>?
  {
    return null
  }

  /**
   * Method to setup NetworkInterceptors (= intercept when a network call is done).
   *
   * @return    the list of NetworkInterceptors to use
   */
  open fun setupNetworkInterceptors(): List<Interceptor>?
  {
    return null
  }

  fun getCacheEntries(): MutableList<Pair<String, String>>
  {
    val cacheUrls = ArrayList<Pair<String, String>>()
    httpClient.cache()?.urls()?.forEach { url ->
      cacheUrls.add(Pair(url, Cache.key(HttpUrl.parse(url) ?: return@forEach)))
    }

    return cacheUrls
  }

  fun getCache(): Cache?
  {
    return httpClient.cache()
  }

  fun setCache(cacheDir: File, cacheSize: Long = CACHE_SIZE)
  {
    this.cacheDir = cacheDir
    this.cacheSize = cacheSize
  }

  @WorkerThread
  protected fun <T : Any> execute(clazz: Class<T>, call: Call<T>?, cachePolicy: CachePolicy = CachePolicy()): T?
  {
    // Redo the request so you can tune the cache
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${cachePolicy.cachePolicyType.name}")

      val newRequest = request.newBuilder().tag(cachePolicy).build()
      val responseBody = httpClient.newCall(newRequest).execute().body()?.string()

      return mapResponseToObject(responseBody, clazz)
    } ?: return null
  }

  protected fun <T : Any> executeResponse(call: Call<T>?, cachePolicy: CachePolicy = CachePolicy()): Response?
  {
    // Redo the request so you can tune the cache
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${cachePolicy.cachePolicyType.name}")

      val newRequest = request.newBuilder().tag(cachePolicy).build()

      return httpClient.newCall(newRequest).execute()
    } ?: return null
  }

  @WorkerThread
  protected fun <T : Any> execute(call: Call<T>?, cachePolicy: CachePolicy = CachePolicy()): String?
  {
    // Redo the request so you can tune the cache
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${cachePolicy.cachePolicyType.name}")

      val newRequest = request.newBuilder().tag(cachePolicy).build()
      val response: Response? = { httpClient.newCall(newRequest).execute() }()

      return response?.body()?.string()
    } ?: return null
  }

  private fun buildRequest(request: Request, cachePolicy: CachePolicy): Request
  {
    val newRequest = when (cachePolicy.cachePolicyType)
    {
      CachePolicyType.ONLY_CACHE,
      CachePolicyType.CACHE_THEN_NETWORK ->
      {
        buildCacheRequest(request, cachePolicy)
      }
      CachePolicyType.ONLY_NETWORK,
      CachePolicyType.NETWORK_THEN_CACHE ->
      {
        buildNetworkRequest(request)
      }
      CachePolicyType.SERVER             -> request
    }

    return newRequest.newBuilder().tag(cachePolicy).build()
  }


  private fun buildNetworkRequest(request: Request, cachePolicy: CachePolicy? = null, originalRequestUrl: HttpUrl? = null): Request
  {
    val cacheControl = CacheControl.Builder()
        .noCache()

    val requestBuilder = request.newBuilder()
        .removeHeader("Pragma")
        .header("Cache-Control", cacheControl.build().toString())

    if (cachePolicy?.customKey != null && originalRequestUrl != null)
    {
      requestBuilder.url(originalRequestUrl)
    }

    return requestBuilder.build()
  }

  private fun buildCacheRequest(request: Request, cachePolicy: CachePolicy): Request
  {
    val cacheControl = CacheControl.Builder().onlyIfCached()

    if (cachePolicy.allowedTimeExpiredCacheInSeconds != null && cachePolicy.allowedTimeExpiredCacheInSeconds > 0)
    {
      cacheControl.maxStale(cachePolicy.allowedTimeExpiredCacheInSeconds, TimeUnit.SECONDS)
    }

    val requestBuilder = request.newBuilder()
        .removeHeader("Pragma")
        .header("Cache-Control", cacheControl.build().toString())

    if (cachePolicy.customKey != null)
    {
      requestBuilder.url(RetrofitWebServiceCaller.CUSTOM_CACHE_URL_PREFIX + cachePolicy.customKey)
    }

    return requestBuilder.build()
  }

  private fun rewriteResponse(response: Response, cachePolicy: CachePolicy): Response
  {
    return when (cachePolicy.cachePolicyType)
    {
      CachePolicyType.ONLY_NETWORK,
      CachePolicyType.NETWORK_THEN_CACHE,
      CachePolicyType.ONLY_CACHE,
      CachePolicyType.CACHE_THEN_NETWORK ->
      {
        val cacheControl = if (cachePolicy.cacheRetentionPolicyInSeconds == null || cachePolicy.cacheRetentionPolicyInSeconds < 0)
        {
          CacheControl.Builder()
              .noStore()
              .build()
              .toString()
        }
        else
        {
          CacheControl.Builder()
              .maxAge(cachePolicy.cacheRetentionPolicyInSeconds, TimeUnit.SECONDS)
              .build()
              .toString()
        }

        val responseBuilder = response.newBuilder()
            .removeHeader("Pragma")
            .header("Cache-Control", cacheControl)
            .header("Date", Date().toString())

        if (cachePolicy.customKey != null)
        {
          val customRequest = response.request().newBuilder().url(RetrofitWebServiceCaller.CUSTOM_CACHE_URL_PREFIX + cachePolicy.customKey).build()
          responseBuilder.request(customRequest)
        }

        responseBuilder.build()
      }
      CachePolicyType.SERVER             -> response
    }
  }

  private fun debug(message: String)
  {
    if (log.isDebugEnabled)
    {
      log.debug(message)
    }
  }

}