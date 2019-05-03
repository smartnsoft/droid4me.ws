package com.smartnsoft.retrofitsample.ws

import retrofit2.Call
import retrofit2.http.GET

/**
 * @author Anthony Msihid
 * @since 2019.05.03
 */
interface TimeApi
{

  companion object Constants
  {

    const val url = "https://postman-echo.com/"
  }

  @GET("time/now/")
  fun getTime(): Call<String>

}