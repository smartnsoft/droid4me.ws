package com.smartnsoft.ws.retrofit

import android.support.annotation.WorkerThread
import com.smartnsoft.droid4me.cache.Values
import com.smartnsoft.droid4me.log.Logger
import com.smartnsoft.droid4me.log.LoggerFactory
import com.smartnsoft.droid4me.ws.WebServiceClient
import okhttp3.Authenticator
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Call
import retrofit2.Converter
import retrofit2.Retrofit
import java.io.File
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 *
 * @author David Fournier
 * @since 2017.10.12
 */

abstract class RetrofitWebServiceCaller<out API>(api: Class<API>, baseUrl: String, private val connectTimeout: Long = CONNECT_TIMEOUT, private val readTimeout: Long = READ_TIMEOUT, private val writeTimeout: Long = WRITE_TIMEOUT, private val cacheSize: Long = CACHE_SIZE, private val defaultCachePolicy: CachePolicy = CachePolicy.ONLY_NETWORK, private val defaultCacheRetentionTimeInSeconds: Int? = null, private val converterFactories: Array<Converter.Factory> = emptyArray())
{

  enum class CachePolicy
  {

    ONLY_CACHE, // Fetch in cache or fail
    CACHE_THEN_NETWORK, // Fetch in cache then fetch to network then cache or fetch to network then cache or fail
    ONLY_NETWORK, // Fetch to network or fail then cache
    NETWORK_THEN_CACHE, // Fetch to network then cache or fetch to cache or fail
    SERVER, // Use the cache control of the server headers
    NO_CACHE // Fetch to network or fail
  }

  companion object
  {

    private const val ONLY_CACHE_UNSATISFIABLE_ERROR_CODE = 504
    const val CONNECT_TIMEOUT = 10 * 1000L        // 10 s
    const val READ_TIMEOUT = 10 * 1000L        // 10 s
    const val WRITE_TIMEOUT = 10 * 1000L        // 10 s
    const val CACHE_SIZE = 10 * 1024 * 1024L // 10 Mb
  }

  data class CachePolicyTag(val cachePolicy: CachePolicy, val cacheRetentionPolicyInSeconds: Int?, val customKey: String? = null)

  // This class is instantiated only once and does not leak as RetrofitWebServiceCaller is a Singleton.
  // So it is OK to declare it `inner`, to pass the `isConnected` boolean.
  inner class CacheInterceptor(private val shouldReturnErrorResponse: Boolean = false) : Interceptor
  {

    private val log: Logger by lazy { LoggerFactory.getInstance(CacheInterceptor::class.java) }

    override fun intercept(chain: Interceptor.Chain): Response
    {
      val cachePolicyTag = chain.request().tag() as? CachePolicyTag
      val cachePolicy = cachePolicyTag?.cachePolicy

      if (cachePolicy == CachePolicy.ONLY_CACHE)
      {
        debug("Call of ${chain.request().method()} to ${chain.request().url()} with cache policy ${cachePolicy.name} failed to find a cached response. Failing.")
        throw Values.CacheException(Exception())
      }

      val response = chain.proceed(chain.request())

      if (cachePolicy != null)
      {
        val firstTry = when (cachePolicy)
        {
          CachePolicy.ONLY_CACHE         ->
          {
            debug("Call of ${chain.request().method()} to ${chain.request().url()} with cache policy ${cachePolicy.name} failed to find a cached response. Failing.")
            throw Values.CacheException(Exception())
          }
          CachePolicy.CACHE_THEN_NETWORK,
          CachePolicy.ONLY_NETWORK,
          CachePolicy.NETWORK_THEN_CACHE ->
          {
            response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", CacheControl.Builder()
                    .maxAge(cachePolicyTag.cacheRetentionPolicyInSeconds ?: Integer.MAX_VALUE, TimeUnit.SECONDS)
                    .build().toString())
                .build()
          }
          CachePolicy.NO_CACHE           ->
          {
            response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", CacheControl.Builder().noStore().build().toString())
                .build()
          }
          CachePolicy.SERVER             -> response
        }

        // Fails fast if the connectivity is known to be lost
        if (hasConnectivity().not())
        {
          if (cachePolicy == CachePolicy.NO_CACHE || cachePolicy == CachePolicy.ONLY_NETWORK)
          {
            val errorMessage = "Call of ${chain.request().method()} to ${chain.request().url()} with cache policy ${cachePolicy.name} failed because the network is not connected."
            debug(errorMessage)

            throw WebServiceClient.CallException(errorMessage, UnknownHostException())
          }
        }

        val secondRequestBuilder = chain.request().newBuilder()
        when
        {
          firstTry.code() == ONLY_CACHE_UNSATISFIABLE_ERROR_CODE && cachePolicy == CachePolicy.CACHE_THEN_NETWORK ->
          {
            debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${cachePolicy.name} failed to find a cached response. Trying call to network.")

            // Fails fast if the connectivity is known to be lost
            if (hasConnectivity().not())
            {
              val errorMessage = "Call of ${chain.request().method()} to ${chain.request().url()} with cache policy ${cachePolicy.name} failed because the network is not connected."
              debug(errorMessage)

              throw WebServiceClient.CallException(errorMessage, UnknownHostException())
            }

            secondRequestBuilder.cacheControl(CacheControl.FORCE_NETWORK)
          }
          firstTry.isSuccessful.not() && cachePolicy == CachePolicy.NETWORK_THEN_CACHE                            ->
          {
            debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${cachePolicy.name} failed to find a network response. Trying call to cache.")

            secondRequestBuilder.cacheControl(CacheControl.FORCE_CACHE)
          }
          firstTry.code() == ONLY_CACHE_UNSATISFIABLE_ERROR_CODE && cachePolicy == CachePolicy.ONLY_CACHE         ->
          {
            debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${cachePolicy.name} failed to find a cached response. Failing.")

            throw Values.CacheException(firstTry.message(), Exception())
          }
          firstTry.isSuccessful.not()                                                                             ->
          {
            debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy $cachePolicy failed to find a response. Failing.")

            return onStatusCodeNotOk(firstTry)
          }
          else                                                                                                    ->
          {
            debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy $cachePolicy successful.")

            return firstTry
          }
        }

        chain.proceed(secondRequestBuilder.build()).also { secondTry ->
          return when
          {
            cachePolicy == CachePolicy.NETWORK_THEN_CACHE && secondTry.code() == ONLY_CACHE_UNSATISFIABLE_ERROR_CODE ->
            {
              debug("Second call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${cachePolicy.name} failed to find a cached response. Failing.")

              onStatusCodeNotOk(secondTry)
            }
            secondTry.isSuccessful.not()                                                                             ->
            {
              debug("Second call of ${secondTry.request().method()} to ${secondTry.request().url()} with cache policy ${cachePolicy.name} failed to find a network response. Failing.")

              onStatusCodeNotOk(secondTry)
            }
            else                                                                                                     ->
            {
              debug("Second call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${cachePolicy.name} was successful.")

              secondTry
            }
          }
        }
      }

      throw IllegalStateException("Cache Policy is malformed")
    }

    @Throws(WebServiceClient.CallException::class)
    fun onStatusCodeNotOk(response: Response): Response
    {
      if (shouldReturnErrorResponse.not())
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
    return emptyList()
  }

  open fun setupNetworkInterceptors(): List<Interceptor>
  {
    return listOf(CacheInterceptor())
  }

  fun cacheDir(cacheDir: File)
  {
    this.cacheDir = cacheDir
  }

  @WorkerThread
  protected fun <T : Any> execute(clazz: Class<T>, call: Call<T>?, withCachePolicy: CachePolicy = defaultCachePolicy, withCacheRetentionTimeInSeconds: Int? = defaultCacheRetentionTimeInSeconds, customKey: String? = null): T?
  {
    // Redo the request so you can tune the cache
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${withCachePolicy.name}")

      val newRequest = request.newBuilder()
          .tag(CachePolicyTag(withCachePolicy, withCacheRetentionTimeInSeconds, customKey))
          .build()

      val responseBody = httpClient.newCall(newRequest).execute().body()?.string()

      return mapResponseToObject(responseBody, clazz)
    } ?: return null
  }

  protected fun <T : Any> executeResponse(call: Call<T>?, withCachePolicy: CachePolicy = defaultCachePolicy, withCacheRetentionTimeInSeconds: Int? = defaultCacheRetentionTimeInSeconds, customKey: String? = null): Response?
  {
    // Redo the request so you can tune the cache
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${withCachePolicy.name}")
      val newRequest = request.newBuilder()
          .tag(CachePolicyTag(withCachePolicy, withCacheRetentionTimeInSeconds, customKey))
          .build()

      return httpClient.newCall(newRequest).execute()
    } ?: return null
  }

  @WorkerThread
  protected fun <T : Any> execute(call: Call<T>?, withCachePolicy: CachePolicy = defaultCachePolicy, withCacheRetentionTimeInSeconds: Int? = defaultCacheRetentionTimeInSeconds, customKey: String? = null): String?
  {
    // Redo the request so you can tune the cache
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${withCachePolicy.name}")
      val newRequest = request.newBuilder()
          .tag(CachePolicyTag(withCachePolicy, withCacheRetentionTimeInSeconds, customKey))
          .build()

      val response: Response? = {
        httpClient.newCall(newRequest).execute()
      }()

      return response?.body()?.string()
    } ?: return null
  }

  private fun debug(message: String)
  {
    if (log.isDebugEnabled)
    {
      log.debug(message)
    }
  }

}