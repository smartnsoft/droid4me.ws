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

abstract class RetrofitWebServiceCaller<out API>(api: Class<API>, baseUrl: String, private val connectTimeout: Long = CONNECT_TIMEOUT, private val readTimeout: Long = READ_TIMEOUT, private val writeTimeout: Long = WRITE_TIMEOUT, private val cacheSize: Long = CACHE_SIZE, private val defaultCachePolicy: CachePolicy = CachePolicy.ONLY_NETWORK, private val defaultCacheRetentionTimeInSeconds: Int? = null, private val defaultAllowedTimeExpiredCacheInSeconds: Int? = null, private val converterFactories: Array<Converter.Factory> = emptyArray())
{

  enum class CachePolicy
  {
    ONLY_NETWORK, // Fetch to network, or fail
    ONLY_CACHE, // Fetch in cache, or fail
    NETWORK_THEN_CACHE, // Fetch to network, then fetch in cache, or fail
    CACHE_THEN_NETWORK, // Fetch in cache, then fetch to network, or fail
    SERVER // Use the cache control of the server headers
  }

  companion object
  {
    private const val ONLY_CACHE_UNSATISFIABLE_ERROR_CODE = 504
    const val CONNECT_TIMEOUT = 10 * 1000L        // 10 s
    const val READ_TIMEOUT = 10 * 1000L        // 10 s
    const val WRITE_TIMEOUT = 10 * 1000L        // 10 s
    const val CACHE_SIZE = 10 * 1024 * 1024L // 10 Mb
  }

  data class CachePolicyTag(val cachePolicy: CachePolicy, val cacheRetentionPolicyInSeconds: Int?, val allowedTimeExpiredCacheInSeconds: Int?, val customKey: String?)

  // This class is instantiated only once and does not leak as RetrofitWebServiceCaller is a Singleton.
  // So it is OK to declare it `inner`, to pass the `isConnected` boolean.
  inner class AppInterceptor(private val shouldReturnErrorResponse: Boolean = false) : Interceptor
  {
    private val log: Logger by lazy { LoggerFactory.getInstance(AppInterceptor::class.java) }

    override fun intercept(chain: Interceptor.Chain): Response?
    {
      val originalRequestUrl = chain.request().url()
      val cachePolicyTag = chain.request().tag() as? CachePolicyTag
      val cachePolicy = cachePolicyTag?.cachePolicy

      if (cachePolicy != null)
      {
        val request = buildRequest(chain.request(), cachePolicyTag.cachePolicy, cachePolicyTag.cacheRetentionPolicyInSeconds, cachePolicyTag.allowedTimeExpiredCacheInSeconds, cachePolicyTag.customKey)

        var firstTry: Response? = null

        try
        {
          firstTry = chain.proceed(request)
        }
        catch (exception: java.lang.Exception)
        {
          val errorMessage = "Call of ${chain.request().method()} to ${chain.request().url()} with cache policy ${cachePolicy.name} failed."
          debug(errorMessage)
        }

        // Fails fast if the connectivity is known to be lost
        if (hasConnectivity().not())
        {
          if (cachePolicy == CachePolicy.ONLY_NETWORK)
          {
            val errorMessage = "Call of ${chain.request().method()} to ${chain.request().url()} with cache policy ${cachePolicy.name} failed because the network is not connected."
            debug(errorMessage)

            throw WebServiceClient.CallException(errorMessage, UnknownHostException())
          }
        }

        val secondRequest: Request
        when
        {
          (firstTry == null || firstTry.code() == ONLY_CACHE_UNSATISFIABLE_ERROR_CODE) && cachePolicy == CachePolicy.CACHE_THEN_NETWORK ->
          {
            debug("Call of ${request.method()} to ${request.url()} with cache policy ${cachePolicy.name} failed to find a cached response. Trying call to network.")

            // Fails fast if the connectivity is known to be lost
            if (hasConnectivity().not())
            {
              val errorMessage = "Call of ${chain.request().method()} to ${chain.request().url()} with cache policy ${cachePolicy.name} failed because the network is not connected."
              debug(errorMessage)

              throw WebServiceClient.CallException(errorMessage, UnknownHostException())
            }

            secondRequest = buildNetworkRequest(request, cachePolicyTag, originalRequestUrl)
          }
          (firstTry == null || firstTry.isSuccessful.not()) && cachePolicy == CachePolicy.NETWORK_THEN_CACHE                            ->
          {
            debug("Call of ${request.method()} to ${request.url()} with cache policy ${cachePolicy.name} failed to find a network response. Trying call to cache.")

            secondRequest = buildCacheRequest(request, cachePolicyTag)
          }
          (firstTry == null || firstTry.code() == ONLY_CACHE_UNSATISFIABLE_ERROR_CODE) && cachePolicy == CachePolicy.ONLY_CACHE         ->
          {
            debug("Call of ${request.method()} to ${request.url()} with cache policy ${cachePolicy.name} failed to find a cached response. Failing.")

            throw Values.CacheException(firstTry?.message(), Exception())
          }
          (firstTry == null || firstTry.isSuccessful.not())                                                                             ->
          {
            debug("Call of ${request.method()} to ${request.url()} with cache policy $cachePolicy failed to find a response. Failing.")

            return onStatusCodeNotOk(firstTry)
          }
          else                                                                                                                          ->
          {
            debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy $cachePolicy successful.")

            return firstTry
          }
        }

        chain.proceed(secondRequest).also { secondTry ->
          return when
          {
            cachePolicy == CachePolicy.NETWORK_THEN_CACHE && secondTry.code() == ONLY_CACHE_UNSATISFIABLE_ERROR_CODE ->
            {
              debug("Second call of ${request.method()} to ${request.url()} with cache policy ${cachePolicy.name} failed to find a cached response. Failing.")

              onStatusCodeNotOk(secondTry)
            }
            secondTry.isSuccessful.not()                                                                             ->
            {
              debug("Second call of ${secondTry.request().method()} to ${secondTry.request().url()} with cache policy ${cachePolicy.name} failed to find a network response. Failing.")

              onStatusCodeNotOk(secondTry)
            }
            else                                                                                                     ->
            {
              debug("Second call of ${request.method()} to ${request.url()} with cache policy ${cachePolicy.name} was successful.")

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

  inner class NetworkInterceptor : Interceptor
  {
    override fun intercept(chain: Interceptor.Chain): Response?
    {
      val cachePolicyTag = chain.request().tag() as? CachePolicyTag
      val cachePolicy = cachePolicyTag?.cachePolicy

      if (cachePolicy != null)
      {
        return rewriteResponse(chain.proceed(chain.request()), cachePolicyTag)
      }

      throw IllegalStateException("Cache Policy is malformed")
    }
  }

  protected open val log: Logger by lazy { LoggerFactory.getInstance(RetrofitWebServiceCaller::class.java) }

  protected open val service: API by lazy {
    val serviceBuilder = Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .client(this.httpClient)

    for (converterFactory in converterFactories)
    {
      serviceBuilder.addConverterFactory(converterFactory)
    }

    return@lazy serviceBuilder.build().create(api)
  }

  open fun hasConnectivity(): Boolean =
      isConnected

  open fun setConnectivity(isConnected: Boolean)
  {
    this.isConnected = isConnected
  }

  // Debug: Method to see urls with signature in cache
  fun getCacheAsList(): MutableList<Pair<String, String>>
  {
    val cacheUrls = ArrayList<Pair<String, String>>()
    httpClient.cache()?.urls()?.forEach { url ->
      cacheUrls.add(Pair(url, Cache.key(HttpUrl.parse(url) ?: return@forEach)))
    }

    return cacheUrls
  }

  private var isConnected = true

  private val httpClient: OkHttpClient by lazy {
    computeHttpClient()
  }

  private var cacheDir: File? = null

  abstract fun <T> mapResponseToObject(responseBody: String?, clazz: Class<T>): T?

  open fun computeHttpClient(): OkHttpClient
  {
    val okHttpClientBuilder = OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
        .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
        .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)

    getAuthenticator()?.also { authenticator ->
      okHttpClientBuilder.authenticator(authenticator)
    }

    val networkInterceptorList = setupNetworkInterceptors()
    networkInterceptorList.forEach { interceptor ->
      okHttpClientBuilder.addNetworkInterceptor(interceptor)
    }

    val interceptorList = setupAppInterceptors()
    interceptorList.forEach { interceptor ->
      okHttpClientBuilder.addInterceptor(interceptor)
    }

    cacheDir?.also { cacheDirectory ->
      val httpCacheDirectory = File(cacheDirectory, "http-cache")
      val cache = Cache(httpCacheDirectory, cacheSize)
      cacheDirectory.setReadable(true)
      okHttpClientBuilder.cache(cache)
    }

    return okHttpClientBuilder.build()
  }

  open fun getAuthenticator(): Authenticator?
  {
    return null
  }

  open fun setupAppInterceptors(): List<Interceptor>
  {
    return listOf(AppInterceptor())
  }

  open fun setupNetworkInterceptors(): List<Interceptor>
  {
    return listOf(NetworkInterceptor())
  }

  fun cacheDir(cacheDir: File)
  {
    this.cacheDir = cacheDir
  }

  @WorkerThread
  protected fun <T : Any> execute(clazz: Class<T>, call: Call<T>?, withCachePolicy: CachePolicy = defaultCachePolicy, withCacheRetentionTimeInSeconds: Int? = defaultCacheRetentionTimeInSeconds, withAllowedTimeExpiredCacheInSeconds: Int? = defaultAllowedTimeExpiredCacheInSeconds, withCustomKey: String? = null): T?
  {
    // Redo the request so you can tune the cache
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${withCachePolicy.name}")

      val newRequest = request.newBuilder()
          .tag(CachePolicyTag(withCachePolicy, withCacheRetentionTimeInSeconds, withAllowedTimeExpiredCacheInSeconds, withCustomKey))
          .build()

      val responseBody = httpClient.newCall(newRequest).execute().body()?.string()

      return mapResponseToObject(responseBody, clazz)
    } ?: return null
  }

  protected fun <T : Any> executeResponse(call: Call<T>?, withCachePolicy: CachePolicy = defaultCachePolicy, withCacheRetentionTimeInSeconds: Int? = defaultCacheRetentionTimeInSeconds, withAllowedTimeExpiredCacheInSeconds: Int? = defaultAllowedTimeExpiredCacheInSeconds, withCustomKey: String? = null): Response?
  {
    // Redo the request so you can tune the cache
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${withCachePolicy.name}")

      val newRequest = request.newBuilder()
          .tag(CachePolicyTag(withCachePolicy, withCacheRetentionTimeInSeconds, withAllowedTimeExpiredCacheInSeconds, withCustomKey))
          .build()

      return httpClient.newCall(newRequest).execute()
    } ?: return null
  }

  @WorkerThread
  protected fun <T : Any> execute(call: Call<T>?, withCachePolicy: CachePolicy = defaultCachePolicy, withCacheRetentionTimeInSeconds: Int? = defaultCacheRetentionTimeInSeconds, withAllowedTimeExpiredCacheInSeconds: Int? = defaultAllowedTimeExpiredCacheInSeconds, withCustomKey: String? = null): String?
  {
    // Redo the request so you can tune the cache
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${withCachePolicy.name}")

      val newRequest = request.newBuilder()
          .tag(CachePolicyTag(withCachePolicy, withCacheRetentionTimeInSeconds, withAllowedTimeExpiredCacheInSeconds, withCustomKey))
          .build()

      val response: Response? = {
        httpClient.newCall(newRequest).execute()
      }()

      return response?.body()?.string()
    } ?: return null
  }

  private fun buildRequest(request: Request, withCachePolicy: CachePolicy = defaultCachePolicy, withCacheRetentionTimeInSeconds: Int? = defaultCacheRetentionTimeInSeconds, withAllowedTimeExpiredCacheInSeconds: Int? = defaultAllowedTimeExpiredCacheInSeconds, withCustomKey: String?): Request
  {
    val newRequest = when (withCachePolicy)
    {
      CachePolicy.ONLY_CACHE,
      CachePolicy.CACHE_THEN_NETWORK ->
      {
        buildCacheRequest(request, CachePolicyTag(withCachePolicy, withCacheRetentionTimeInSeconds, withAllowedTimeExpiredCacheInSeconds, withCustomKey))
      }
      CachePolicy.ONLY_NETWORK,
      CachePolicy.NETWORK_THEN_CACHE ->
      {
        buildNetworkRequest(request)
      }
      CachePolicy.SERVER             -> request
    }

    return newRequest.newBuilder()
        .tag(CachePolicyTag(withCachePolicy, withCacheRetentionTimeInSeconds, withAllowedTimeExpiredCacheInSeconds, withCustomKey))
        .build()
  }


  private fun buildNetworkRequest(request: Request, cachePolicyTag: CachePolicyTag? = null, originalRequestUrl: HttpUrl? = null): Request
  {
    val cacheControl = CacheControl.Builder()
        .noCache()

    val requestBuilder = request.newBuilder()
        .removeHeader("Pragma")
        .header("Cache-Control", cacheControl.build().toString())

    if (cachePolicyTag?.customKey != null && originalRequestUrl != null)
    {
      requestBuilder.url(originalRequestUrl)
    }

    return requestBuilder.build()
  }

  private fun buildCacheRequest(request: Request, cachePolicyTag: CachePolicyTag): Request
  {
    val cacheControl = CacheControl.Builder()
        .onlyIfCached()

    if (cachePolicyTag.allowedTimeExpiredCacheInSeconds != null && cachePolicyTag.allowedTimeExpiredCacheInSeconds > 0)
    {
      cacheControl.maxStale(cachePolicyTag.allowedTimeExpiredCacheInSeconds, TimeUnit.SECONDS)
    }

    val requestBuilder = request.newBuilder()
        .removeHeader("Pragma")
        .header("Cache-Control", cacheControl.build().toString())

    if (cachePolicyTag.customKey != null)
    {
      requestBuilder.url("https://customkeycache.smart/" + cachePolicyTag.customKey)
    }

    return requestBuilder.build()
  }

  private fun rewriteResponse(response: Response, cachePolicyTag: CachePolicyTag): Response
  {
    return when (cachePolicyTag.cachePolicy)
    {
      CachePolicy.ONLY_NETWORK,
      CachePolicy.NETWORK_THEN_CACHE,
      CachePolicy.ONLY_CACHE,
      CachePolicy.CACHE_THEN_NETWORK ->
      {
        val cacheControl = if (cachePolicyTag.cacheRetentionPolicyInSeconds == null || cachePolicyTag.cacheRetentionPolicyInSeconds < 0)
        {
          CacheControl.Builder()
              .noStore()
              .build()
              .toString()
        }
        else
        {
          CacheControl.Builder()
              .maxAge(cachePolicyTag.cacheRetentionPolicyInSeconds, TimeUnit.SECONDS)
              .build()
              .toString()
        }

        val responseBuilder = response.newBuilder()
            .removeHeader("Pragma")
            .header("Cache-Control", cacheControl)
            .header("Date", Date().toString())

        if (cachePolicyTag.customKey != null)
        {
          val customRequest = response.request().newBuilder().url("https://customkeycache.smart/" + cachePolicyTag.customKey).build()
          responseBuilder.request(customRequest)
        }

        responseBuilder.build()
      }
      CachePolicy.SERVER             -> response
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