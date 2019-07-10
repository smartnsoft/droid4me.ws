package test

import com.smartnsoft.retrofitsample.ws.InfoAPI
import com.smartnsoft.ws.retrofit.AccessToken
import com.smartnsoft.ws.retrofit.AuthJacksonRetrofitWebServiceCaller
import com.smartnsoft.ws.retrofit.AuthProvider
import okhttp3.*
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

    private class AuthSimpleWebServiceCaller(val authProvider: AuthProvider, builtInCache: BuiltInCache? = BuiltInCache(shouldReturnErrorResponse = true), baseUrl: String) : AuthJacksonRetrofitWebServiceCaller<InfoAPI>(api = InfoAPI::class.java, baseUrl = baseUrl, builtInCache = builtInCache, authProvider = authProvider)
    {

      init
      {
        setupCache(File("./"), "auth")
      }

      fun info(): Response?
      {
        return executeResponse(service.getInfo())
      }

      fun login(username: String, password: String): Boolean
      {
        return loginUser(username, password)
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
    }

    var mockServerBaseUrl: String = ""
    private lateinit var serviceCaller: AuthSimpleWebServiceCaller

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
          val xApiKey = request.headers["XApiKey"]
          if (xApiKey == null || xApiKey != InfoAPI.apiToken)
          {
            return MockResponse().setResponseCode(403)
          }

          if (request.path == "/auth")
          {
            val params = request.body.toString().removePrefix("[text=").removeSuffix("]").split("&").map {
              val keyValue = it.split("=")
              keyValue[0] to keyValue[1]
            }.toMap()

            return if (params["username"] == "user" && params["password"] == "pwd" && params["grant_type"] == "password")
            {
              MockResponse().setResponseCode(200)
                  .setBody("{\n" +
                      "  \"access_token\":\"abcccccc\",\n" +
                      "  \"refresh_token\":\"12345abc\",\n" +
                      "  \"expires_in\":3000,\n" +
                      "  \"token_type\":\"Bearer\"\n" +
                      "}")
            }
            else if (params["refresh_token"] == "12345abc" && params["grant_type"] == "refresh_token")
            {
              MockResponse().setResponseCode(200)
                  .setBody("{\n" +
                      "  \"access_token\":\"abccccccd\",\n" +
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

          if (request.path == "/info" && request.getHeader("Authorization") == "Bearer abccccccd")
            return MockResponse().setResponseCode(200).setBody("{\\\"info\\\":{\\\"name\":\"Lucas Albuquerque\",\"age\":\"21\",\"gender\":\"male\"}}")
          else if (request.path == "/info" && request.getHeader("Authorization") == "Bearer abcccccc")
            return MockResponse().setResponseCode(401)
          else if (request.path == "/info" && request.getHeader("Authorization") != "Bearer abcccccc")
            return MockResponse().setResponseCode(403)

          return MockResponse().setResponseCode(404)
        }
      }
      server.dispatcher = dispatcher

      val tokenProvider = object : AuthProvider
      {
        override fun getAuthRoute(): String
        {
          return mockServerBaseUrl + "auth"
        }

        override fun getXApiKey(): String
        {
          return InfoAPI.apiToken
        }

        var _accessToken: AccessToken? = null

        override fun getAccessToken(): AccessToken?
        {
          return _accessToken
        }

        override fun setAccessToken(accessToken: AccessToken?)
        {
          accessToken.apply {
            _accessToken = accessToken
          }
        }
      }

      serviceCaller = AuthSimpleWebServiceCaller(baseUrl = mockServerBaseUrl, authProvider = tokenProvider)
    }
  }

  @Test
  fun ok()
  {
    serviceCaller.authProvider.setAccessToken(null)
    serviceCaller.login("user", "pwd").also {
      assert(serviceCaller.info()?.code == 200)
    }
  }

  @Test
  fun ko()
  {
    serviceCaller.authProvider.setAccessToken(null)
    serviceCaller.login("usr", "pwd")
    assert(serviceCaller.info()?.code == 403)
  }
}