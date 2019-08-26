package com.smartnsoft.ws.retrofit.api

import com.smartnsoft.ws.retrofit.bo.AccessToken
import com.smartnsoft.ws.retrofit.bo.LoginBody
import retrofit2.Call
import retrofit2.http.*

/**
 * @author David Fournier
 * @since 2019.07.01
 */
interface AuthAPI
{

  @POST
  @Headers("Content-Type: application/json")
  fun authToken(@Url url: String, @Body authBody: LoginBody)
      : Call<AccessToken>

  @POST
  @FormUrlEncoded
  fun refreshToken(@Url url: String, @Field("refresh_token") refreshToken: String)
      : Call<AccessToken>

}