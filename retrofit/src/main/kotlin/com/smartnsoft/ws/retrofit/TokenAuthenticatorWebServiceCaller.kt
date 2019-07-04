package com.smartnsoft.ws.retrofit

import okhttp3.*
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * The class description here.
 *
 * @author David Fournier
 * @since 2019.06.21
 */
abstract class TokenAuthenticatorWebServiceCaller<API>
@JvmOverloads
constructor(api: Class<API>, baseUrl: String, connectTimeout: Long = CONNECT_TIMEOUT, readTimeout: Long = READ_TIMEOUT, writeTimeout: Long = WRITE_TIMEOUT, withBuiltInCache: BuiltInCache? = BuiltInCache())
  : RetrofitWebServiceCaller<API>(api, baseUrl, connectTimeout, readTimeout, writeTimeout, withBuiltInCache)
{

  abstract val tokenProvider: TokenProvider

   class TokenAuthenticator : Authenticator
  {

    override fun authenticate(route: Route?, response: Response): Request?
    {
      val encodedPath = response.request().url().encodedPath()
      print("coucou $encodedPath")
      return null
    }
  }

  inner class TokenInterceptor : Interceptor
  {

    override fun intercept(chain: Interceptor.Chain): Response
    {
      return chain.proceed(chain.request())
//      val encodedPath = chain.request().url().encodedPath()
//      return if (CleyadesApi.isTokenRoute(encodedPath))
//      {
//        chain.proceed(chain.request())
//      }
//      else
//      {
//        tokenProvider.getAccessToken()?.apply {
//          val request = chain
//              .request()
//              .newBuilder()
//              .addHeader("Authorization", "${this.tokenType} ${this.accessToken}")
//        }
//
//
//        val apiToken = tokenProvider.apiToken?.token ?: getAPIToken()
//        val request = chain
//            .request()
//            .newBuilder()
//            .addHeader("Authorization", "Bearer $apiToken")
//        if (CleyadesApi.isAuthRoute(encodedPath).not())
//        {
//          // NOTE: It should be impossible to call API without user token
//          val userToken = tokenProvider.userToken?.token ?: ""
//          request.addHeader("Token", userToken)
//        }
//        return chain.proceed(request.build())
//      }
    }

  }

  override fun setupAuthenticator(): Authenticator?
  {
    super.setupAuthenticator()
    return TokenAuthenticator()
  }

  override fun setupAppInterceptors(): List<Interceptor>?
  {
    return (super.setupAppInterceptors() ?: emptyList()) + listOf(TokenInterceptor())
  }

}