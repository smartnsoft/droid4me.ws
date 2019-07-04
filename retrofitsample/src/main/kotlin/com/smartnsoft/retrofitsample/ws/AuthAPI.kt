package com.smartnsoft.retrofitsample.ws

import com.smartnsoft.retrofitsample.bo.IP
import com.smartnsoft.retrofitsample.bo.PostResponse
import com.smartnsoft.ws.retrofit.AccessToken
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

  companion object Constants
  {

    const val apiToken: String = "123456789abcdef"
    const val baseUrl = "https://www.google.com/"
  }

  @GET("auth/{user}/{password}")
  fun login(@Path("user") user: String, @Path("password") password: String): Call<AccessToken>

  @GET("info")
  fun getInfo(): Call<String>

}