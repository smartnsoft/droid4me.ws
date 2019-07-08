package com.smartnsoft.retrofitsample.ws

import retrofit2.Call
import retrofit2.http.*

/**
 * The class description here.
 *
 * @author David Fournier
 * @since 2019.07.01
 */
interface InfoAPI
{

  companion object Constants
  {

    const val apiToken: String = "123456789abcdef"

  }

  @GET("info")
  fun getInfo(): Call<String>

}