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

    enum class ServerBehavior
    {
      NORMAL,
      TOKEN_ERROR,
      TOKEN_AND_REFRESH_ERROR,
      WRONG_XAPIKEY
    }

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


    }

    var mockServerBaseUrl: String = ""
    private lateinit var serviceCaller: AuthSimpleWebServiceCaller

    @JvmStatic
    fun initializeServer(behavior: ServerBehavior)
    {
      val server = MockWebServer()
      server.start()
      mockServerBaseUrl = server.url("/").toString()

      val dispatcher = object : Dispatcher()
      {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse
        {
          val xApiKey = request.headers["XApiKey"]
          val xApiKeyServer = if (behavior == Companion.ServerBehavior.WRONG_XAPIKEY) "yosoleil" else InfoAPI.apiToken

          if (xApiKey == null || xApiKey != xApiKeyServer)
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
                      "  \"access_token\":\"abc\",\n" +
                      "  \"refresh_token\":\"123\",\n" +
                      "  \"expires_in\":3000,\n" +
                      "  \"token_type\":\"Bearer\"\n" +
                      "}")
            }
            else if (params["refresh_token"] == "123" && params["grant_type"] == "refresh_token")
            {
              MockResponse().setResponseCode(200)
                  .setBody("{\n" +
                      "  \"access_token\":\"def\",\n" +
                      "  \"refresh_token\":\"456\",\n" +
                      "  \"expires_in\":3000,\n" +
                      "  \"token_type\":\"Bearer\"\n" +
                      "}")
            }
            else if (params["refresh_token"] == "456" && params["grant_type"] == "refresh_token")
            {
              MockResponse().setResponseCode(200)
                  .setBody("{\n" +
                      "  \"access_token\":\"ghi\",\n" +
                      "  \"refresh_token\":\"789\",\n" +
                      "  \"expires_in\":3000,\n" +
                      "  \"token_type\":\"Bearer\"\n" +
                      "}")
            }
            else
            {
              MockResponse().setResponseCode(403)
            }
          }

          if (request.path == "/info" )
          {
            if (request.getHeader("Authorization")?.isNotBlank() == true)
            {
              when (behavior)
              {
                Companion.ServerBehavior.NORMAL                  ->
                {
                  if (request.getHeader("Authorization") == "Bearer abc")
                    return MockResponse().setResponseCode(200).setBody("{\\\"info\\\":{\\\"name\":\"Lucas Albuquerque\",\"age\":\"21\",\"gender\":\"male\"}}")
                  else
                    return MockResponse().setResponseCode(401)
                }
                Companion.ServerBehavior.TOKEN_ERROR             ->
                {
                  if (request.getHeader("Authorization") == "Bearer def")
                    return MockResponse().setResponseCode(200).setBody("{\\\"info\\\":{\\\"name\":\"Lucas Albuquerque\",\"age\":\"21\",\"gender\":\"male\"}}")
                  else
                    return MockResponse().setResponseCode(401)
                }
                Companion.ServerBehavior.TOKEN_AND_REFRESH_ERROR ->
                {
                  if (request.getHeader("Authorization") == "Bearer ghi")
                    return MockResponse().setResponseCode(200).setBody("{\\\"info\\\":{\\\"name\":\"Lucas Albuquerque\",\"age\":\"21\",\"gender\":\"male\"}}")
                  else
                    return MockResponse().setResponseCode(401)
                }
                Companion.ServerBehavior.WRONG_XAPIKEY           ->
                {
                }
              }
            }
            else
            {
              return MockResponse().setResponseCode(403)
            }
          }

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
  fun refreshTokenOK()
  {
    initializeServer(Companion.ServerBehavior.TOKEN_ERROR)

    serviceCaller.authProvider.setAccessToken(null)
    serviceCaller.login("user", "pwd").also {
      assert(serviceCaller.info()?.code == 200)
    }
  }

  @Test
  fun loginKO()
  {
    initializeServer(Companion.ServerBehavior.NORMAL)

    serviceCaller.authProvider.setAccessToken(null)
    serviceCaller.login("usr", "pwd")
    assert(serviceCaller.info()?.code == 403)
  }

  @Test
  fun tokenAndRefreshKO()
  {
    initializeServer(Companion.ServerBehavior.TOKEN_AND_REFRESH_ERROR)

    serviceCaller.authProvider.setAccessToken(null)
    serviceCaller.login("user", "pwd").also {
      assert(serviceCaller.info()?.code == 401)
    }
  }

  @Test
  fun loginAndInfoOK()
  {
    initializeServer(Companion.ServerBehavior.NORMAL)

    serviceCaller.authProvider.setAccessToken(null)
    serviceCaller.login("user", "pwd").also {
      assert(serviceCaller.info()?.code == 200)
    }
  }
}