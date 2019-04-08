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

abstract class RetrofitWebServiceCaller<out API>(api: Class<API>, baseUrl: String, private val connectTimeout: Long = CONNECT_TIMEOUT, private val readTimeout: Long = READ_TIMEOUT, private val writeTimeout: Long = WRITE_TIMEOUT, private val cacheSize: Long = CACHE_SIZE, val defaultCachePolicy: CachePolicy = CachePolicy.ONLY_NETWORK, protected val converterFactories: Array<Converter.Factory> = emptyArray())
{

  data class CachePolicyTag(val cachePolicy: CachePolicy, val cacheRetentionPolicyInSeconds: Int?)

  // This class is instantiated only once and does not leak as RetrofitWebServiceCaller is a Singleton.
  // So it is OK to declare it `inner`, to pass the `isConnected` boolean.
  inner class CacheInterceptor(val shouldReturnErrorResponse: Boolean = false) : Interceptor
  {

    val log: Logger by lazy { LoggerFactory.getInstance(CacheInterceptor::class.java) }

    override fun intercept(chain: Interceptor.Chain): Response
    {
      val firstRequestBuilder = chain.request().newBuilder()
      val cachePolicyTag = chain.request().tag()
      if (cachePolicyTag is CachePolicyTag)
      {
        val cachePolicy = cachePolicyTag.cachePolicy
        when (cachePolicy)
        {
          CachePolicy.ONLY_CACHE,
          CachePolicy.CACHE_THEN_NETWORK -> firstRequestBuilder.cacheControl(
              CacheControl.Builder()
                  .noCache()
                  .maxAge(cachePolicyTag.cacheRetentionPolicyInSeconds ?: Integer.MAX_VALUE, TimeUnit.SECONDS)
                  .build())
          CachePolicy.ONLY_NETWORK,
          CachePolicy.NETWORK_THEN_CACHE -> firstRequestBuilder.cacheControl(
              CacheControl.Builder()
                  .noCache()
                  .maxAge(cachePolicyTag.cacheRetentionPolicyInSeconds ?: Integer.MAX_VALUE, TimeUnit.SECONDS)
                  .build())
          CachePolicy.SERVER             ->
          {/* Do nothing */
          }
          CachePolicy.NO_CACHE           -> firstRequestBuilder.cacheControl(CacheControl.Builder().noStore().build())
        }

        // Fails fast if the connectivity is known to be lost
        if (!hasConnectivity())
        {
          if (cachePolicy == CachePolicy.NO_CACHE || cachePolicy == CachePolicy.ONLY_NETWORK)
          {
            val errorMessage = "Call of ${chain.request().method()} to ${chain.request().url()} with cache policy ${CachePolicy.CACHE_THEN_NETWORK} failed because the network is not connected."
            debug(errorMessage)
            throw WebServiceClient.CallException(errorMessage, UnknownHostException())
          }
        }

        val firstTry = chain.proceed(firstRequestBuilder.build())
        val secondRequestBuilder = chain.request().newBuilder()

        if (firstTry.code() == ONLY_CACHE_UNSATISFIABLE_ERROR_CODE && cachePolicy == CachePolicy.CACHE_THEN_NETWORK)
        {
          debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${CachePolicy.CACHE_THEN_NETWORK} failed to find a cached response. Trying call to network.")
          // Fails fast if the connectivity is known to be lost
          if (!hasConnectivity())
          {
            val errorMessage = "Call of ${chain.request().method()} to ${chain.request().url()} with cache policy ${CachePolicy.CACHE_THEN_NETWORK} failed because the network is not connected."
            debug(errorMessage)
            throw WebServiceClient.CallException(errorMessage, UnknownHostException())
          }
          secondRequestBuilder.cacheControl(CacheControl.FORCE_NETWORK)
        }
        else if (!firstTry.isSuccessful && cachePolicy == CachePolicy.NETWORK_THEN_CACHE)
        {
          debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${CachePolicy.NETWORK_THEN_CACHE} failed to find a network response. Trying call to cache.")
          secondRequestBuilder.cacheControl(CacheControl.FORCE_CACHE)
        }
        else if (firstTry.code() == ONLY_CACHE_UNSATISFIABLE_ERROR_CODE && cachePolicy == CachePolicy.ONLY_CACHE)
        {
          debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${CachePolicy.ONLY_CACHE} failed to find a cached response. Failing.")
          throw Values.CacheException(firstTry.message(), Exception())
        }
        else if (!firstTry.isSuccessful)
        {
          debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy $cachePolicy failed to find a response. Failing.")
          return onStatusCodeNotOk(firstTry)
        }
        else
        {
          debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy $cachePolicy successful.")
          return firstTry
        }

        val secondTry = chain.proceed(secondRequestBuilder.build())

        if (cachePolicy == CachePolicy.NETWORK_THEN_CACHE && secondTry.code() == ONLY_CACHE_UNSATISFIABLE_ERROR_CODE)
        {
          debug("Second call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${CachePolicy.NETWORK_THEN_CACHE} failed to find a cached response. Failing.")
          return onStatusCodeNotOk(secondTry)
        }
        else if (!secondTry.isSuccessful)
        {
          debug("Second call of ${secondTry.request().method()} to ${secondTry.request().url()} with cache policy ${CachePolicy.CACHE_THEN_NETWORK} failed to find a network response. Failing.")
          return onStatusCodeNotOk(secondTry)
        }
        else
        {
          debug("Second call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy $cachePolicy was successful.")
          return secondTry
        }
      }
      throw IllegalStateException("Cache Policy is malformed")
    }

    fun debug(message: String)
    {
      if (log.isDebugEnabled)
      {
        log.debug(message)
      }
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

  }

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

    serviceBuilder.build().create(api)
  }

  open fun hasConnectivity(): Boolean
  {
    return isConnected
  }

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

    val interceptorList = setupAppInterceptors()
    interceptorList.forEach { interceptor ->
      okHttpClientBuilder.addInterceptor(interceptor)
    }

    val networkInterceptorList = setupNetworkInterceptors()
    networkInterceptorList.forEach { interceptor ->
      okHttpClientBuilder.addNetworkInterceptor(interceptor)
    }

    getAuthenticator()?.also { authenticator ->
      okHttpClientBuilder.authenticator(authenticator)
    }

    cacheDir?.let { cacheDirectory ->
      val cache = Cache(cacheDirectory, cacheSize)
      cacheDirectory.setReadable(true)
      okHttpClientBuilder.cache(cache).build()
    }

    return okHttpClientBuilder.build()
  }

  open fun getAuthenticator(): Authenticator?
  {
    return null
  }

  open fun setupAppInterceptors(): List<Interceptor>
  {
    return listOf(CacheInterceptor())
  }

  open fun setupNetworkInterceptors(): List<Interceptor>
  {
    return emptyList()
  }

  fun cacheDir(cacheDir: File)
  {
    this.cacheDir = cacheDir
  }

  @WorkerThread
  protected fun <T : Any> execute(clazz: Class<T>, call: Call<T>?, withCachePolicy: CachePolicy = defaultCachePolicy, withCacheRetentionTimeInSeconds: Int? = null): T?
  {
    // Redo the request so you can tune the cache
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${withCachePolicy.name}")
      val newRequest = request.newBuilder()
          .tag(CachePolicyTag(withCachePolicy, withCacheRetentionTimeInSeconds))
          .build()

      val responseBody = httpClient.newCall(newRequest).execute().body()?.string()
      return mapResponseToObject(responseBody, clazz)
    } ?: return null
  }

  protected fun <T : Any> executeResponse(call: Call<T>?, withCachePolicy: CachePolicy = defaultCachePolicy, withCacheRetentionTimeInSeconds: Int? = null): Response?
  {
    // Redo the request so you can tune the cache
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${withCachePolicy.name}")
      val newRequest = request.newBuilder()
          .tag(CachePolicyTag(withCachePolicy, withCacheRetentionTimeInSeconds))
          .build()

      return httpClient.newCall(newRequest).execute()
    } ?: return null
  }

  @WorkerThread
  protected fun <T : Any> execute(call: Call<T>?, withCachePolicy: CachePolicy = defaultCachePolicy, withCacheRetentionTimeInSeconds: Int? = null): String?
  {
    // Redo the request so you can tune the cache
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${withCachePolicy.name}")
      val newRequest = request.newBuilder()
          .tag(CachePolicyTag(withCachePolicy, withCacheRetentionTimeInSeconds))
          .build()

      val response: Response? = {
        (httpClient.newCall(newRequest)).execute()
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