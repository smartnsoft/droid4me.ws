package test

import com.smartnsoft.retrofitsample.ws.AuthAPI
import com.smartnsoft.ws.retrofit.AccessToken
import com.smartnsoft.ws.retrofit.JacksonRetrofitWebServiceCaller
import com.smartnsoft.ws.retrofit.TokenAuthenticatorWebServiceCaller
import com.smartnsoft.ws.retrofit.TokenProvider
import okhttp3.*
import okhttp3.Authenticator
import okhttp3.internal.http2.Header
import okhttp3.mockwebserver.Dispatcher
import org.junit.Test
import java.io.File
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.MockWebServer
import org.junit.BeforeClass


/**
 * The class description here.
 *
 * @author David Fournier
 * @since 2019.06.20
 */
class Authenticator
{

  companion object
  {

    class SimpleWebServiceCaller(builtInCache: BuiltInCache? = BuiltInCache(), baseUrl: String, val tokenProvider: TokenProvider) : JacksonRetrofitWebServiceCaller<AuthAPI>(api = AuthAPI::class.java, baseUrl = baseUrl, withBuiltInCache = builtInCache)
    {

      init
      {
        setupCache(File("./"), "auth")
      }

      override fun setupAuthenticator(): Authenticator?
      {
        return TokenAuthenticatorWebServiceCaller.TokenAuthenticator()
      }

      fun info(): Response?
      {
        return executeResponse(service.getInfo())
      }

      fun auth(username: String, password: String): AccessToken?
      {
        return execute(AccessToken::class.java, service.login(username, password))
      }

      override fun shouldDoSecondCall(response: Response?, exception: Exception?): Boolean
      {
        return if (response?.code == 401 || response?.code == 403)
        {
          false
        }
        else
        {
          super.shouldDoSecondCall(response, exception)
        }
      }

      override fun setupNetworkInterceptors(): List<Interceptor>?
      {
        return (super.setupNetworkInterceptors() ?: emptyList()) + (object : Interceptor
        {
          override fun intercept(chain: Interceptor.Chain): Response
          {
            val original = chain.request()

            val headers = mutableListOf(Header("XApiKey", AuthAPI.apiToken))
            tokenProvider.getAccessToken()?.accessToken?.also { accessToken ->
              headers.add(Header("Authorization", accessToken))
            }

            val request = original.newBuilder()
                .headers(headers)
                .method(original.method, original.body)
                .build()

            return chain.proceed(request)
          }
        })
      }
    }

    var mockServerBaseUrl: String = ""
    lateinit var serviceCaller: SimpleWebServiceCaller

    @BeforeClass
    @JvmStatic
    fun initializeServer()
    {
      val server = MockWebServer()

      //      // Schedule some responses.
      //      server.enqueue(MockResponse().setBody("hello, world!"))
      //      server.enqueue(MockResponse().setBody("sup, bra?"))
      //      server.enqueue(MockResponse().setBody("yo dog"))

      // Start the server.
      server.start()

      // Ask the server for its URL. You'll need this to make HTTP requests.
      mockServerBaseUrl = server.url("/").toString()


      val dispatcher = object : Dispatcher()
      {

        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse
        {
          if (request.headers["XApiKey"] == null || request.headers["XApiKey"]!! != AuthAPI.apiToken)
          {
            return MockResponse().setResponseCode(403)
          }

          val segments = (request.requestUrl as HttpUrl).encodedPathSegments
          if (segments.size == 3 && segments[0] == "auth")
          {
            return if (segments[1] == "user" && segments[2] == "pwd")
            {
              MockResponse().setResponseCode(200)
                  .setBody("{\n" +
                      "  \"access_token\":\"abcccccc\",\n" +
                      "  \"refresh_token\":\"12345abc\",\n" +
                      "  \"expires_in\":3000,\n" +
                      "  \"token_type\":\"Bearer\"\n" +
                      "}")
            }
            else
            {
              MockResponse().setResponseCode(403)
            }
          }

          if (request.path == "/info")

            when (request.path)
            {
              "/v1/login/auth/"   -> return MockResponse().setResponseCode(200)
              "v1/check/version/" -> return MockResponse().setResponseCode(200).setBody("version=9")
              "/v1/profile/info"  -> return MockResponse().setResponseCode(200).setBody("{\\\"info\\\":{\\\"name\":\"Lucas Albuquerque\",\"age\":\"21\",\"gender\":\"male\"}}")
            }
          return MockResponse().setResponseCode(404)
        }
      }
      server.dispatcher = dispatcher

      val tokenProvider = object : TokenProvider
      {

        var _accessToken = AccessToken()

        override fun getAccessToken(): AccessToken
        {
          return _accessToken
        }

        override fun setAccessToken(accessToken: AccessToken)
        {
          accessToken.apply {
            _accessToken = accessToken
          }
        }

        override fun deleteAccessToken()
        {
        }
      }

      serviceCaller = SimpleWebServiceCaller(baseUrl = mockServerBaseUrl, tokenProvider = tokenProvider)
    }
  }

  @Test
  fun crash()
  {
    val auth = serviceCaller.auth("user", "pwd")
    serviceCaller.info()
  }
}