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

/**
 * A class where you configure your service
 *
 * @param[api] interface where you implement your retrofit calls, see [Retrofit.create].
 * @param[baseUrl] base url of the API you want to reach.
 * @param[connectTimeout] the connect timeout is applied when connecting a TCP socket to the target host.
 * @param[readTimeout] the read timeout is applied to both the TCP socket and for individual read IO operations including on [Source] of the [Response].
 * @param[writeTimeout] the write timeout is applied for individual write IO operations.
 * @param[builtInCache] class to configure the cache and its default values. You must [setupCache]. If set to null, the built in cache is disabled and you don't need to [setupCache]. See [BuiltInCache].
 * @param[converterFactories] array of converter factory for serialization and deserialization of objects.
 *
 */
@Suppress("UNCHECKED_CAST")
abstract class RetrofitWebServiceCaller<out API>(api: Class<API>,
                                                 baseUrl: String,
                                                 private val connectTimeout: Long = CONNECT_TIMEOUT,
                                                 private val readTimeout: Long = READ_TIMEOUT,
                                                 private val writeTimeout: Long = WRITE_TIMEOUT,
                                                 private val builtInCache: BuiltInCache? = BuiltInCache(),
                                                 private val converterFactories: Array<Converter.Factory> = emptyArray())
{

  /**
   * An enum that define the way the [Call] has to be done. Possible values are:
   *
   *
   * [ONLY_NETWORK] - Fetch to network, or fail.
   *
   * [ONLY_CACHE] - Fetch in [Cache], or fail.
   *
   * [NETWORK_THEN_CACHE] - Fetch to network, then fetch in [Cache], or fail.
   *
   * [CACHE_THEN_NETWORK] - Fetch in [Cache], then fetch to network, or fail.
   *
   * [SERVER] - Use the cache-control of server's headers.
   *
   */
  enum class FetchPolicyType
  {
    ONLY_NETWORK,
    ONLY_CACHE,
    NETWORK_THEN_CACHE,
    CACHE_THEN_NETWORK,
    SERVER
  }

  companion object
  {
    private const val CUSTOM_CACHE_URL_PREFIX = "https://customkeycache.smart/"

    private const val ONLY_CACHE_UNSATISFIABLE_ERROR_CODE = 504

    const val SMART_ORIGINAL_URL_HEADER = "Smart-Original-URL"

    const val SMART_SERVER_DATE_HEADER = "Smart-Server-Date"

    const val PRAGMA_HEADER = "Pragma"

    const val DATE_HEADER = "Date"

    const val CACHE_CONTROL_HEADER = "Cache-Control"

    const val CACHE_PATH = "http-cache"

    const val CONNECT_TIMEOUT = 10 * 1000L                  // 10 seconds

    const val READ_TIMEOUT = 10 * 1000L                     // 10 seconds

    const val WRITE_TIMEOUT = 10 * 1000L                    // 10 seconds

    const val CACHE_SIZE = 10 * 1024 * 1024L                // 10 Mb

    const val DEFAULT_CACHE_TIME_IN_SECONDS = 60 * 60 * 1000  // 1 hour
  }

  /**
   * Class to configure the behavior of the [Cache] and its default values.
   *
   * @param[defaultFetchPolicyType] enum that define the way all the [Call] are done, see [FetchPolicyType].
   * @param[defaultCacheRetentionTimeInSeconds] default time in seconds all the [Call] will cache the [Response] (= maxAge).
   * @param[defaultAllowedTimeExpiredCacheInSeconds] default time in seconds you allow all the cached [Response] to be valid after their expiration (= maxStale).
   * @param[useClientDateForCache] if true, override all the date of the [Response] with the client date. Useful if the server time is misconfigured.
   *
   */
  class BuiltInCache(val defaultFetchPolicyType: FetchPolicyType = FetchPolicyType.NETWORK_THEN_CACHE,
                     val defaultCacheRetentionTimeInSeconds: Int? = RetrofitWebServiceCaller.DEFAULT_CACHE_TIME_IN_SECONDS,
                     val defaultAllowedTimeExpiredCacheInSeconds: Int? = null,
                     val useClientDateForCache: Boolean = true)

  /**
   * Class to configure the behavior of the [Call].
   *
   * @param[fetchPolicyType] enum that define the way the call is done, see [FetchPolicyType].
   * @param[cacheRetentionPolicyInSeconds] time in seconds the call will cache the [Response] (= maxAge).
   * @param[allowedTimeExpiredCacheInSeconds] time in seconds you allow the cached [Response] to be valid after its expiration (= maxStale).
   * @param[useClientDateForCache] if true, override the date of the [Response] with the client date. Useful if the server time is misconfigured.
   * @param[customKey] use this if you want to store the [Response] with a custom key in [Cache] (rather than its url, used by default).
   *
   */
  inner class CachePolicy(val fetchPolicyType: FetchPolicyType = builtInCache?.defaultFetchPolicyType
      ?: FetchPolicyType.ONLY_NETWORK,
                          val cacheRetentionPolicyInSeconds: Int? = builtInCache?.defaultCacheRetentionTimeInSeconds,
                          val allowedTimeExpiredCacheInSeconds: Int? = builtInCache?.defaultAllowedTimeExpiredCacheInSeconds,
                          val useClientDateForCache: Boolean = builtInCache?.useClientDateForCache ?: true,
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
      if (cacheDir == null)
      {
        /**
         * Note: see [setupCache] to fix this exception.
         */
        throw IllegalStateException("If you use the built-in cache, you have to set your cache directory before performing any call!")
      }

      val originalRequestUrl = chain.request().url()
      val cachePolicy = chain.request().tag() as? RetrofitWebServiceCaller<API>.CachePolicy
      val fetchPolicyType = cachePolicy?.fetchPolicyType

      if (fetchPolicyType != null)
      {
        val request = buildRequest(chain.request(), cachePolicy)
        var firstTry: Response? = null

        try
        {
          firstTry = chain.proceed(request)
        }
        catch (exception: java.lang.Exception)
        {
          val errorMessage = "Call of ${chain.request().method()} to ${chain.request().url()} with cache policy ${fetchPolicyType.name} failed."

          debug(errorMessage)
        }

        // Fails fast if the connectivity is known to be lost
        if (hasConnectivity().not())
        {
          if (fetchPolicyType == FetchPolicyType.ONLY_NETWORK)
          {
            val errorMessage = "Call of ${chain.request().method()} to ${chain.request().url()} with cache policy ${fetchPolicyType.name} failed because the network is not connected."

            debug(errorMessage)

            throw WebServiceClient.CallException(errorMessage, UnknownHostException())
          }
        }

        val secondRequest: Request
        when
        {
          (firstTry == null || firstTry.code() == RetrofitWebServiceCaller.ONLY_CACHE_UNSATISFIABLE_ERROR_CODE) && fetchPolicyType == FetchPolicyType.CACHE_THEN_NETWORK ->
          {
            debug("Call of ${request.method()} to ${request.url()} with cache policy ${fetchPolicyType.name} failed to find a cached response. Trying call to network.")

            // Fails fast if the connectivity is known to be lost
            if (hasConnectivity().not())
            {
              val errorMessage = "Call of ${chain.request().method()} to ${chain.request().url()} with cache policy ${fetchPolicyType.name} failed because the network is not connected."

              debug(errorMessage)

              throw WebServiceClient.CallException(errorMessage, UnknownHostException())
            }

            secondRequest = buildNetworkRequest(request, cachePolicy, originalRequestUrl)
          }
          (firstTry == null || firstTry.isSuccessful.not()) && fetchPolicyType == FetchPolicyType.NETWORK_THEN_CACHE                                                     ->
          {
            debug("Call of ${request.method()} to ${request.url()} with cache policy ${fetchPolicyType.name} failed to find a network response. Trying call to cache.")

            secondRequest = buildCacheRequest(request, cachePolicy)
          }
          (firstTry == null || firstTry.code() == RetrofitWebServiceCaller.ONLY_CACHE_UNSATISFIABLE_ERROR_CODE) && fetchPolicyType == FetchPolicyType.ONLY_CACHE         ->
          {
            debug("Call of ${request.method()} to ${request.url()} with cache policy ${fetchPolicyType.name} failed to find a cached response. Failing.")

            throw Values.CacheException(firstTry?.message(), Exception())
          }
          (firstTry == null || firstTry.isSuccessful.not())                                                                                                              ->
          {
            debug("Call of ${request.method()} to ${request.url()} with cache policy $fetchPolicyType failed to find a response. Failing.")

            return onStatusCodeNotOk(firstTry)
          }
          else                                                                                                                                                           ->
          {
            debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy $fetchPolicyType successful.")

            return firstTry
          }
        }

        chain.proceed(secondRequest).also { secondTry ->
          return when
          {
            fetchPolicyType == FetchPolicyType.NETWORK_THEN_CACHE && secondTry.code() == RetrofitWebServiceCaller.ONLY_CACHE_UNSATISFIABLE_ERROR_CODE ->
            {
              debug("Second call of ${request.method()} to ${request.url()} with cache policy ${fetchPolicyType.name} failed to find a cached response. Failing.")

              onStatusCodeNotOk(secondTry)
            }
            secondTry.isSuccessful.not()                                                                                                              ->
            {
              debug("Second call of ${secondTry.request().method()} to ${secondTry.request().url()} with cache policy ${fetchPolicyType.name} failed to find a network response. Failing.")

              onStatusCodeNotOk(secondTry)
            }
            else                                                                                                                                      ->
            {
              debug("Second call of ${request.method()} to ${request.url()} with cache policy ${fetchPolicyType.name} was successful.")

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
      val fetchPolicyType = cachePolicy?.fetchPolicyType

      if (fetchPolicyType != null)
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
        .client(httpClient)

    converterFactories.forEach { converterFactory ->
      serviceBuilder.addConverterFactory(converterFactory)
    }

    return@lazy serviceBuilder.build().create(api)
  }

  private val httpClient: OkHttpClient by lazy {
    computeHttpClient()
  }

  private var isHttpClientInitialized = false

  private var isConnected = true

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

    if (builtInCache != null)
    {
      okHttpClientBuilder.addNetworkInterceptor(NetworkCacheInterceptor())
      okHttpClientBuilder.addInterceptor(AppCacheInterceptor())
    }

    cacheDir?.also { cacheDirectory ->
      cacheDirectory.setReadable(true)
      okHttpClientBuilder.cache(Cache(File(cacheDirectory, RetrofitWebServiceCaller.CACHE_PATH), cacheSize))
    }

    isHttpClientInitialized = true

    return okHttpClientBuilder.build()
  }

  /**
   * Override this method to setup an [Authenticator].
   *
   * @return the [Authenticator] that the [httpClient] builder will use.
   */
  open fun setupAuthenticator(): Authenticator?
  {
    return null
  }

  /**
   * Override this method to setup an app [Interceptor] list (= always intercept call).
   *
   * @return the [Interceptor] list that the [httpClient] builder will use.
   *
   * Pay attention that the order you add them is important.
   */
  open fun setupAppInterceptors(): List<Interceptor>?
  {
    return null
  }

  /**
   * Override this method to setup a network [Interceptor] list (= only intercept network call).
   *
   * @return the [Interceptor] list that the [httpClient] builder will use.
   *
   * Pay attention that the order you add them is important.
   */
  open fun setupNetworkInterceptors(): List<Interceptor>?
  {
    return null
  }

  /**
   * Method to set the [File] and its max size for caching with [httpClient].
   *
   * @param[cacheDir] the [File] to use.
   * @param[cacheSize] the max size for the [Cache] (in [Byte]).
   */
  fun setupCache(cacheDir: File, cacheSize: Long = RetrofitWebServiceCaller.CACHE_SIZE)
  {
    if (isHttpClientInitialized)
    {
      throw IllegalStateException("You must setup your cache file before performing any call!")
    }

    this.cacheDir = cacheDir
    this.cacheSize = cacheSize
  }

  /**
   * Method to get the [Cache] of the [httpClient].
   *
   * @return the [Cache] instance of the [httpClient].
   */
  fun getCache(): Cache?
  {
    return httpClient.cache()
  }

  /**
   * Method to remove an entry from [Cache].
   *
   * @param[urlToRemove] response of the url we want to remove from cache.
   *
   * @return true if the entry is removed, false otherwise.
   */
  fun removeFromCache(urlToRemove: String): Boolean
  {
    val iterator = httpClient.cache()?.urls()
    while (iterator?.hasNext() == true)
    {
      val value = iterator.next()
      if (value == urlToRemove)
      {
        iterator.remove()

        return true
      }
    }

    return false
  }

  /**
   * Method to get the entries (urls) in the [Cache].
   *
   * @return the [String] list of url's entries in [Cache].
   */
  fun getCacheUrls(): MutableList<String>
  {
    val cacheUrls = ArrayList<String>()
    httpClient.cache()?.urls()?.forEach { url ->
      cacheUrls.add(url)
    }

    return cacheUrls
  }

  @WorkerThread
  protected fun <T : Any> executeResponse(call: Call<T>?, cachePolicy: CachePolicy = CachePolicy()): Response?
  {
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${cachePolicy.fetchPolicyType.name}")

      val newRequest = request.newBuilder().tag(if (builtInCache != null) cachePolicy else null).build()
      val response: Response? = httpClient.newCall(newRequest).execute()

      response?.body()?.close()

      return response
    } ?: return null
  }

  @WorkerThread
  protected fun <T : Any> execute(clazz: Class<T>, call: Call<T>?, cachePolicy: CachePolicy = CachePolicy()): T?
  {
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${cachePolicy.fetchPolicyType.name}")

      val newRequest = request.newBuilder().tag(if (builtInCache != null) cachePolicy else null).build()
      val response: Response? = httpClient.newCall(newRequest).execute()
      val responseBody = response?.body()?.string()

      return mapResponseToObject(responseBody, clazz)
    } ?: return null
  }

  @WorkerThread
  protected fun <T : Any> execute(call: Call<T>?, cachePolicy: CachePolicy = CachePolicy()): String?
  {
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${cachePolicy.fetchPolicyType.name}")

      val newRequest = request.newBuilder().tag(if (builtInCache != null) cachePolicy else null).build()
      val response: Response? = httpClient.newCall(newRequest).execute()

      return response?.body()?.string()
    } ?: return null
  }

  private fun buildRequest(request: Request, cachePolicy: CachePolicy): Request
  {
    val newRequest = when (cachePolicy.fetchPolicyType)
    {
      FetchPolicyType.ONLY_CACHE,
      FetchPolicyType.CACHE_THEN_NETWORK ->
      {
        buildCacheRequest(request, cachePolicy)
      }
      FetchPolicyType.ONLY_NETWORK,
      FetchPolicyType.NETWORK_THEN_CACHE ->
      {
        buildNetworkRequest(request)
      }
      FetchPolicyType.SERVER             -> request
    }

    return newRequest.newBuilder().tag(cachePolicy).build()
  }


  private fun buildNetworkRequest(request: Request, cachePolicy: CachePolicy? = null, originalRequestUrl: HttpUrl? = null): Request
  {
    val cacheControl = CacheControl.Builder().noCache()

    val requestBuilder = request.newBuilder()
        .removeHeader(RetrofitWebServiceCaller.PRAGMA_HEADER)
        .header(RetrofitWebServiceCaller.CACHE_CONTROL_HEADER, cacheControl.build().toString())

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
        .removeHeader(RetrofitWebServiceCaller.PRAGMA_HEADER)
        .header(RetrofitWebServiceCaller.CACHE_CONTROL_HEADER, cacheControl.build().toString())

    if (cachePolicy.customKey != null)
    {
      requestBuilder.url(RetrofitWebServiceCaller.CUSTOM_CACHE_URL_PREFIX + cachePolicy.customKey)
    }

    return requestBuilder.build()
  }

  private fun rewriteResponse(response: Response, cachePolicy: CachePolicy): Response
  {
    when (cachePolicy.fetchPolicyType)
    {
      FetchPolicyType.ONLY_NETWORK,
      FetchPolicyType.NETWORK_THEN_CACHE,
      FetchPolicyType.ONLY_CACHE,
      FetchPolicyType.CACHE_THEN_NETWORK ->
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
            .removeHeader(RetrofitWebServiceCaller.PRAGMA_HEADER)
            .header(RetrofitWebServiceCaller.CACHE_CONTROL_HEADER, cacheControl)

        if (cachePolicy.useClientDateForCache)
        {
          response.header(RetrofitWebServiceCaller.DATE_HEADER)?.also { serverDate ->
            responseBuilder.header(RetrofitWebServiceCaller.SMART_SERVER_DATE_HEADER, serverDate)
          }
          responseBuilder.header(RetrofitWebServiceCaller.DATE_HEADER, Date().toString())
        }

        if (cachePolicy.customKey != null)
        {
          responseBuilder.header(RetrofitWebServiceCaller.SMART_ORIGINAL_URL_HEADER, response.request().url().toString())

          val customRequest = response.request().newBuilder().url(RetrofitWebServiceCaller.CUSTOM_CACHE_URL_PREFIX + cachePolicy.customKey).build()
          responseBuilder.request(customRequest)
        }

        return responseBuilder.build()
      }
      FetchPolicyType.SERVER             -> return response
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