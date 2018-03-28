package com.monirapps.retrofitsample

import retrofit2.Call
import retrofit2.http.GET

/**
 *
 * @author David Fournier
 * @since 2017.10.27
 */

interface WSApi
{

  companion object Constants
  {

    val url = "http://httpbin.org/"
  }

  @GET("get")
  fun getString(): Call<String>

  @GET("ip")
  fun getIp(): Call<IP>
}