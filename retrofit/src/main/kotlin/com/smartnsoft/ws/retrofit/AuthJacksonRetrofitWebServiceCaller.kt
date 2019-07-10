package com.smartnsoft.ws.retrofit

import android.support.annotation.WorkerThread
import okhttp3.*
import retrofit2.Call
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * @author Anthony Msihid
 * @since 2019.07.10
 */

abstract class AuthJacksonRetrofitWebServiceCaller<out API>
@JvmOverloads
constructor(private val authProvider: AuthProvider,
            api: Class<API>,
            baseUrl: String,
            connectTimeout: Long = CONNECT_TIMEOUT,
            readTimeout: Long = READ_TIMEOUT,
            writeTimeout: Long = WRITE_TIMEOUT,
            builtInCache: BuiltInCache? = BuiltInCache())
  : JacksonRetrofitWebServiceCaller<API>(api,
    baseUrl,
    connectTimeout,
    readTimeout,
    writeTimeout,
    builtInCache)
{
  inner class TokenAuthenticatorInterceptor : Authenticator, Interceptor
  {
    override fun authenticate(route: Route?, response: Response): Request?
    {
      authProvider.apply {
        val accessToken = getAccessToken()

        if (accessToken == null || response.request().header("Authorization") != "${accessToken.tokenType} ${accessToken.accessToken}")
        {
          authProvider.setAccessToken(null)
        }
        else
        {
          val newAccessToken = executeAuth(authService?.refreshToken(getAuthRoute(), accessToken.refreshToken))
          authProvider.setAccessToken(newAccessToken)

          authProvider.getAccessToken()?.apply {
            return response.request().newBuilder()
                .header("Authorization", "${this.tokenType} ${this.accessToken}")
                .build()
          }
        }
      }

      return null
    }

    override fun intercept(chain: Interceptor.Chain): Response
    {
      val newRequest = chain.request().newBuilder()

      authProvider.apply {
        newRequest.header("XApiKey", getXApiKey())

        if (chain.request().url() != HttpUrl.parse(getAuthRoute()))
        {
          getAccessToken()?.also { accessToken ->
            newRequest.header("Authorization", "${accessToken.tokenType} ${accessToken.accessToken}")
          }
        }
      }

      return chain.proceed(newRequest.build())
    }
  }

  private var isHttpAuthClientInitialized = false

  private val httpAuthClient: OkHttpClient by lazy {
    computeHttpAuthClient()
  }

  private val authService: AuthAPI? by lazy {
    authProvider.run {
      val authRoute = if (getAuthRoute().endsWith("/"))
      {
        getAuthRoute()
      }
      else
      {
        "${getAuthRoute()}/"
      }

      val serviceBuilder = Retrofit
          .Builder()
          .baseUrl(authRoute)
          .client(httpAuthClient)

      converterFactories.forEach { converterFactory ->
        serviceBuilder.addConverterFactory(converterFactory)
      }

      return@run serviceBuilder.build().create(AuthAPI::class.java)
    }
  }

  private fun computeHttpAuthClient(): OkHttpClient
  {
    val okHttpAuthClientBuilder = OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
        .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
        .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)

    okHttpAuthClientBuilder.addNetworkInterceptor(TokenAuthenticatorInterceptor())

    isHttpAuthClientInitialized = true

    return okHttpAuthClientBuilder.build()
  }

  @WorkerThread
  protected fun loginUser(username: String, password: String): Boolean
  {
    val newAccessToken = try
    {
      executeAuth(authService?.authToken(authProvider.getAuthRoute(), username, password))?.also { accessToken ->
        authProvider.setAccessToken(accessToken)
      }
    }
    catch (exception: Exception)
    {
      null
    }

    return newAccessToken != null
  }

  @WorkerThread
  private fun executeAuth(call: Call<AccessToken>?): AccessToken?
  {
    call?.request()?.let { request ->
      debug("Starting execution of auth call ${request.method()} to ${request.url()}")

      val newRequest = request.newBuilder().build()
      val response: Response? = httpAuthClient.newCall(newRequest).execute()
      val responseBody = response?.body()?.string()

      return mapResponseToObject(responseBody, AccessToken::class.java)
    } ?: return null
  }
}