package com.smartnsoft.ws.retrofit.api

import com.smartnsoft.ws.retrofit.bo.AccessToken
import com.smartnsoft.ws.retrofit.bo.LoginBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.http.*

/**
 * @author David Fournier
 * @since 2019.07.01
 */
interface AuthAPI
{

  @POST
  fun authToken(@Url url: String, @Body authBody: LoginBody)
      : Call<AccessToken>

  @POST
  fun refreshToken(@Url url: String, @Field("refresh_token") refreshToken: String)
      : Call<AccessToken>

}