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

/**
 * A class where you configure your service with an implemented authenticator
 *
 * @param[authProvider] [AuthProvider] object with the configuration for the authentication.
 * @param[api] interface where you implement your retrofit calls, see [Retrofit.create].
 * @param[baseUrl] base url of the API you want to reach.
 * @param[connectTimeout] the connect timeout is applied when connecting a TCP socket to the target host.
 * @param[readTimeout] the read timeout is applied to both the TCP socket and for individual read IO operations including on [Source] of the [Response].
 * @param[writeTimeout] the write timeout is applied for individual write IO operations.
 * @param[withBuiltInCache] class to configure the cache and its default values. You must [setupCache]. If set to null, the built in cache is disabled and you don't need to [setupCache]. See [BuiltInCache].
 *
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
  : JacksonRetrofitWebServiceCaller<API>(api, baseUrl, connectTimeout, readTimeout, writeTimeout, builtInCache)
{
  private inner class TokenAuthenticatorInterceptor : Authenticator, Interceptor
  {
    override fun authenticate(route: Route?, response: Response): Request?
    {
      authProvider.apply {
        val accessToken = getAccessToken()

        // We don't want to try to refresh the token more than one time
        if (responseCount(response) <= 1 && accessToken != null)
        {
          try
          {
            setAccessToken(executeAuth(authService?.refreshToken(getAuthRoute(), accessToken.refreshToken)))
          }
          catch (exception: Exception)
          {
            debug("Call of refresh token failed with exception: ${exception.printStackTrace()}")
            setAccessToken(null)
          }

          getAccessToken()?.apply {
            val newAuthorization = "${this.tokenType} ${this.accessToken}"

            // We don't want to try the call two times with the same token
            if (response.request().header("Authorization") != newAuthorization)
            {
              return response.request().newBuilder()
                  .header("Authorization", newAuthorization)
                  .build()
            }
          }
        }

        setAccessToken(null)
        return null
      }
    }

    override fun intercept(chain: Interceptor.Chain): Response
    {
      val newRequest = chain.request().newBuilder()

      authProvider.apply {
        getXApiKey().takeIf { XApiKey -> XApiKey.isNotBlank() }?.apply {
          newRequest.header("XApiKey", this)
        }

        getAccessToken()?.takeIf { accessToken -> accessToken.tokenType.isNotBlank() && accessToken.accessToken.isNotBlank() }?.apply {
          newRequest.header("Authorization", "$tokenType $accessToken")
        }
      }

      return chain.proceed(newRequest.build())
    }

    private fun responseCount(response: Response): Int
    {
      var responseHolder: Response? = response
      var result = 1

      while (responseHolder?.priorResponse() != null)
      {
        result++
        responseHolder = response.priorResponse()
      }

      return result
    }
  }

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


  /**
   * Method to login the user with the implemented authenticator.
   *
   * @param[username] the username of the user to connect.
   * @param[password] the password of the user to connect.
   *
   * @return the success of the connection.
   */
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
      debug("Call of login failed with exception: ${exception.printStackTrace()}")
      null
    }

    return newAccessToken != null
  }

  final override fun setupAuthenticator(): Authenticator?
  {
    return TokenAuthenticatorInterceptor()
  }

  final override fun setupFirstAppInterceptors(): List<Interceptor>?
  {
    return listOf(TokenAuthenticatorInterceptor())
  }

  final override fun shouldDoSecondCall(response: Response?, exception: Exception?): Boolean
  {
    return if (response?.code() == 401 || response?.code() == 403)
    {
      false
    }
    else
    {
      super.shouldDoSecondCall(response, exception)
    }
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

  private fun computeHttpAuthClient(): OkHttpClient
  {
    val okHttpAuthClientBuilder = OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
        .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
        .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)

    okHttpAuthClientBuilder.addNetworkInterceptor(TokenAuthenticatorInterceptor())

    return okHttpAuthClientBuilder.build()
  }
}