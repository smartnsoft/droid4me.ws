package com.smartnsoft.wstest

import android.support.annotation.WorkerThread
import com.fasterxml.jackson.databind.ObjectMapper
import com.smartnsoft.droid4me.cache.Values
import com.smartnsoft.droid4me.log.LoggerFactory
import com.smartnsoft.droid4me.ws.WebServiceClient
import okhttp3.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit


/**
 * The class description here.
 *
 * @author David Fournier
 * @since 2017.10.12
 */

private val ONLY_CACHE_UNSATISFIABLE_ERROR_CODE = 504

abstract class RetrofitWebServiceCaller<API>(api: Class<API>, baseUrl: String, connectTimeout: Long = CONNECT_TIMEOUT, readTimeout: Long = READ_TIMEOUT, writeTimeout: Long = WRITE_TIMEOUT, cacheSize: Long = CACHE_SIZE, defaultCachePolicy: CachePolicy = CachePolicy.ONLY_NETWORK) {

  data class CachePolicyTag(val cachePolicy: CachePolicy, val cacheRetentionPolicyInSeconds: Int?)

  inner class CacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val firstRequestBuilder = chain.request().newBuilder()
      val cachePolicyTag = chain.request().tag()
      if (cachePolicyTag is CachePolicyTag) {
        val cachePolicy = cachePolicyTag.cachePolicy
        when (cachePolicy) {
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
          CachePolicy.SERVER -> {/* Do nothing */
          }
          CachePolicy.NO_CACHE -> firstRequestBuilder.cacheControl(CacheControl.Builder().noStore().build())
        }

        val firstTry = chain.proceed(firstRequestBuilder.build())
        val secondRequestBuilder = chain.request().newBuilder()

        if (firstTry.code() == ONLY_CACHE_UNSATISFIABLE_ERROR_CODE && cachePolicy == CachePolicy.CACHE_THEN_NETWORK) {
          debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${CachePolicy.CACHE_THEN_NETWORK} failed to find a cached response. Trying call to network.")
          secondRequestBuilder.cacheControl(CacheControl.FORCE_NETWORK)
        }
        else if (firstTry.isSuccessful == false && cachePolicy == CachePolicy.NETWORK_THEN_CACHE) {
          debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${CachePolicy.NETWORK_THEN_CACHE} failed to find a network response. Trying call to cache.")
          secondRequestBuilder.cacheControl(CacheControl.FORCE_CACHE)
        }
        else if (firstTry.code() == ONLY_CACHE_UNSATISFIABLE_ERROR_CODE && cachePolicy == CachePolicy.ONLY_CACHE) {
          debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${CachePolicy.ONLY_CACHE} failed to find a cached response. Failing.")
          throw Values.CacheException(firstTry.message(), Exception())
        }
        else if (firstTry.isSuccessful == false) {
          debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${cachePolicy} failed to find a response. Failing.")
          throw WebServiceClient.CallException(firstTry.message(), firstTry.code())
        }
        else {
          debug("Call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${cachePolicy} successful.")
          return firstTry
        }

        val secondTry = chain.proceed(secondRequestBuilder.build())

        if (cachePolicy == CachePolicy.NETWORK_THEN_CACHE && secondTry.code() == ONLY_CACHE_UNSATISFIABLE_ERROR_CODE) {
          debug("Second call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${CachePolicy.NETWORK_THEN_CACHE} failed to find a cached response. Failing.")
          throw Values.CacheException(firstTry.message(), Exception())
        }
        else if (secondTry.isSuccessful == false) {
          debug("Second call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${CachePolicy.CACHE_THEN_NETWORK} failed to find a network response. Failing.")
          throw WebServiceClient.CallException(firstTry.message(), firstTry.code())
        }
        else {
          debug("Second call of ${firstTry.request().method()} to ${firstTry.request().url()} with cache policy ${cachePolicy} was successful.")
          return secondTry
        }
      }
      throw IllegalStateException("Cache Policy is malformed")
    }
  }

  enum class CachePolicy {
    ONLY_CACHE, // Fetch in cache or fail
    CACHE_THEN_NETWORK, // Fetch in cache then fetch to network then cache or fetch to network then cache or fail
    ONLY_NETWORK, // Fetch to network or fail then cache
    NETWORK_THEN_CACHE, // Fetch to network then cache or fetch to cache or fail
    SERVER, // Use the cache control of the server headers
    NO_CACHE // Fetch to network or fail
  }

  companion object {
    const val CONNECT_TIMEOUT = 10 * 1000L        // 10 s
    const val READ_TIMEOUT = 10 * 1000L        // 10 s
    const val WRITE_TIMEOUT = 10 * 1000L        // 10 s
    const val CACHE_SIZE = 10 * 1024 * 1024L // 10 Mb
  }

  val connectTimeout: Long
  val readTimeout: Long
  val writeTimeout: Long
  val cacheSize: Long
  val defaultCachePolicy: CachePolicy
  val log = LoggerFactory.getInstance(RetrofitWebServiceCaller::class.java)

  private val mapper = ObjectMapper()

  private var cache: Cache? = null

  private var httpClient: OkHttpClient

  protected val service: API

  init {
    this.connectTimeout = connectTimeout
    this.readTimeout = readTimeout
    this.writeTimeout = writeTimeout
    this.cacheSize = cacheSize
    this.defaultCachePolicy = defaultCachePolicy

    this.httpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
        .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
        .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
        .addInterceptor(CacheInterceptor())
        .build()

    this.service = Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .client(this.httpClient)
        .addConverterFactory(JacksonConverterFactory.create())
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
        .create(api)
  }

  var cacheDir: File? = null
    set(cacheDir) {
      cacheDir?.let { cacheDirectory ->
        this.cache = Cache(cacheDirectory, cacheSize)
        cacheDirectory.setReadable(true)
        this.httpClient = this.httpClient.newBuilder().cache(cache).build()
      }
    }

  @WorkerThread
  protected fun <T : Any> execute(clazz: Class<T>, call: Call<T>?, withCachePolicy: CachePolicy = defaultCachePolicy, withCacheRetentionTimeInSeconds: Int? = null): T? {
    // Redo the request so you can tune the cache
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${withCachePolicy.name}")
      val newRequest = request.newBuilder()
          .tag(CachePolicyTag(withCachePolicy, withCacheRetentionTimeInSeconds))
          .build()

      val responseBody = httpClient.newCall(newRequest).execute().body()?.string()
      return mapper.readValue(responseBody, clazz)
    } ?: return null
  }

  @WorkerThread
  protected fun <T : Any> execute(call: Call<T>?, withCachePolicy: CachePolicy = defaultCachePolicy, withCacheRetentionTimeInSeconds: Int? = null): String? {
    // Redo the request so you can tune the cache
    call?.request()?.let { request ->
      debug("Starting execution of call ${request.method()} to ${request.url()} with cache policy ${withCachePolicy.name}")
      val newRequest = request.newBuilder()
          .tag(CachePolicyTag(withCachePolicy, withCacheRetentionTimeInSeconds))
          .build()

      val response: Response? = {
        try {
          (httpClient.newCall(newRequest)).execute()
        }
        catch (exception: Exception) {
          null
        }
      }()
      return response?.body()?.string()
    } ?: return null
  }

  fun debug(message: String) {
    if (log.isDebugEnabled) {
      log.debug(message)
    }
  }

}