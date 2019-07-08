package com.smartnsoft.ws.retrofit

import retrofit2.Call
import retrofit2.http.*

/**
 * The class description here.
 *
 * @author David Fournier
 * @since 2019.07.01
 */
interface AuthAPI
{

  @POST
  @FormUrlEncoded
  fun authToken(@Url url: String,
                @Field("username") username: String,
                @Field("password") password: String,
                @Field("grant_type") grantType: String = "password")
      : Call<AccessToken>

  @POST
  @FormUrlEncoded
  fun refreshToken(@Url url: String,
                   @Field("refresh_token") refreshToken: String,
                   @Field("grant_type") grantType: String = "refresh_token")
      : Call<AccessToken>

}