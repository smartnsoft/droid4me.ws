package test

import android.util.Log
import com.smartnsoft.retrofitsample.ws.RDLVApi
import com.smartnsoft.ws.retrofit.api.AuthProvider
import com.smartnsoft.ws.retrofit.bo.AccessToken
import com.smartnsoft.ws.retrofit.bo.ErrorResponse
import com.smartnsoft.ws.retrofit.bo.ResponseWithError
import com.smartnsoft.ws.retrofit.caller.AuthJacksonRetrofitWebServiceCaller
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.File
import java.lang.Exception


/**
 * @author Anthony Msihid
 * @since 2019.08.23
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RDLVAuthenticator
{
  enum class RequestBehavior
  {
    NORMAL,
    WRONG_TOKEN,
    WRONG_TOKEN_AND_REFRESH,
    WRONG_XAPIKEY,
    EMPTY_XAPIKEY
  }

  companion object
  {
    private var privateAccessToken: AccessToken? = null

    private class AuthSimpleWebServiceCaller(val authProvider: AuthProvider, builtInCache: BuiltInCache? = BuiltInCache(shouldReturnErrorResponse = true), baseUrl: String) : AuthJacksonRetrofitWebServiceCaller<RDLVApi>(api = RDLVApi::class.java, baseUrl = baseUrl, builtInCache = builtInCache, authProvider = authProvider)
    {

      init
      {
        setupCache(File("./"), "RDLV")
      }

      fun login(username: String, password: String): ResponseWithError<AccessToken, ErrorResponse>?
      {
        return loginUser(username, password)
      }

      fun getClients(): okhttp3.Response?
      {
        return executeResponse(service.getClients())
      }
    }

    private lateinit var serviceCaller: AuthSimpleWebServiceCaller

    @JvmStatic
    fun initializeService(requestBehavior: RequestBehavior)
    {
      val authProvider = object : AuthProvider
      {
        override fun getBaseRoute(): String
        {
          return RDLVApi.baseRoute
        }

        override fun getXApiKey(): String
        {
          return when (requestBehavior)
          {
            RequestBehavior.WRONG_XAPIKEY -> "wrongxapikey"
            RequestBehavior.EMPTY_XAPIKEY -> ""
            else                          -> RDLVApi.xApiToken
          }
        }

        override fun getAccessToken(): AccessToken?
        {
          return privateAccessToken
        }

        override fun setAccessToken(accessToken: AccessToken?)
        {
          accessToken.apply {
            privateAccessToken = accessToken
          }
        }
      }

      serviceCaller = AuthSimpleWebServiceCaller(baseUrl = authProvider.getBaseRoute(), authProvider = authProvider)

      when (requestBehavior)
      {
        RequestBehavior.WRONG_TOKEN             ->
        {
          authProvider.setAccessToken(privateAccessToken?.let { realAccessToken ->
            AccessToken("wrongtoken", realAccessToken.refreshToken, realAccessToken.tokenType, realAccessToken.expiresIn)
          })
        }
        RequestBehavior.WRONG_TOKEN_AND_REFRESH ->
        {
          authProvider.setAccessToken(privateAccessToken?.let { realAccessToken ->
            AccessToken("wrongtoken", "wrongrefresh", realAccessToken.tokenType, realAccessToken.expiresIn)
          })
        }
        else                                    ->
        {
          // Normal
        }
      }
    }
  }

  @Test
  fun test_01_loginOk()
  {
    initializeService(RequestBehavior.NORMAL)

    serviceCaller.authProvider.setAccessToken(null)
    val response = serviceCaller.login("j.dehais@equinoa.com", "api")
    val success = response?.successResponse != null

    assert(success && serviceCaller.authProvider.getAccessToken() != null)
  }

  @Test
  fun test_02_getInfoWithWrongApiKey()
  {
    initializeService(RequestBehavior.WRONG_XAPIKEY)

    try
    {
      val response = serviceCaller.getClients()

      assert(response?.isSuccessful == false && response.code == 403 && serviceCaller.authProvider.getAccessToken() == null)
    }
    catch (exception: Exception)
    {
      Log.e("RDLVAuthenticator", "Unable to get clients with exception :", exception)

      assert(false)
    }
  }

  @Test
  fun test_03_relogAfter403()
  {
    initializeService(RequestBehavior.NORMAL)

    val response = serviceCaller.login("j.dehais@equinoa.com", "api")
    val success = response?.successResponse != null

    assert(success && serviceCaller.authProvider.getAccessToken() != null)
  }

  @Test
  fun test_04_getInfoAfterLogin()
  {
    initializeService(RequestBehavior.NORMAL)

    // Waiting a bit
    Thread.sleep(1_000)

    try
    {
      val response = serviceCaller.getClients()

      assert(response?.isSuccessful == true && response.code == 200 && serviceCaller.authProvider.getAccessToken() != null)
    }
    catch (exception: Exception)
    {
      Log.e("RDLVAuthenticator", "Unable to get clients with exception :", exception)

      assert(false)
    }
  }

  @Test
  fun test_05_getInfoWithEmptyApiKey()
  {
    initializeService(RequestBehavior.EMPTY_XAPIKEY)

    try
    {
      val response = serviceCaller.getClients()

      assert(response?.isSuccessful == false && response.code == 403 && serviceCaller.authProvider.getAccessToken() == null)
    }
    catch (exception: Exception)
    {
      Log.e("RDLVAuthenticator", "Unable to get clients with exception :", exception)

      assert(false)
    }
  }

  @Test
  fun test_06_relogAfter403()
  {
    initializeService(RequestBehavior.NORMAL)

    val response = serviceCaller.login("j.dehais@equinoa.com", "api")
    val success = response?.successResponse != null

    assert(success && serviceCaller.authProvider.getAccessToken() != null)
  }

  @Test
  fun test_07_wrongAccessAndRefreshToken()
  {
    initializeService(RequestBehavior.WRONG_TOKEN_AND_REFRESH)

    try
    {
      val response = serviceCaller.getClients()

      assert(response?.isSuccessful == false && response.code == 401 && serviceCaller.authProvider.getAccessToken() == null)
    }
    catch (exception: Exception)
    {
      Log.e("RDLVAuthenticator", "Unable to get clients with exception :", exception)

      assert(false)
    }
  }

  @Test
  fun test_08_relogAfter403()
  {
    initializeService(RequestBehavior.NORMAL)

    val response = serviceCaller.login("j.dehais@equinoa.com", "api")
    val success = response?.successResponse != null

    assert(success && serviceCaller.authProvider.getAccessToken() != null)
  }

  @Test
  fun test_09_getInfoAfterRefresh()
  {
    initializeService(RequestBehavior.WRONG_TOKEN)
    val oldToken = serviceCaller.authProvider.getAccessToken()

    try
    {
      serviceCaller.getClients()
      val newToken = serviceCaller.authProvider.getAccessToken()

      assert(newToken != null && oldToken != newToken)
    }
    catch (exception: Exception)
    {
      Log.e("RDLVAuthenticator", "Unable to get clients with exception :", exception)

      assert(false)
    }
  }

  @Test
  fun test_10_loginKo()
  {
    initializeService(RequestBehavior.NORMAL)

    serviceCaller.authProvider.setAccessToken(null)
    val response = serviceCaller.login("wrong@equinoa.com", "wrong")
    val fail = response?.errorResponse != null

    assert(fail && serviceCaller.authProvider.getAccessToken() == null)
  }
}