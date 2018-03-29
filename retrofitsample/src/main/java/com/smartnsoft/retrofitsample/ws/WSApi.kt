package com.smartnsoft.retrofitsample.ws

import com.smartnsoft.retrofitsample.bo.IP
import com.smartnsoft.retrofitsample.bo.PostResponse
import retrofit2.Call
import retrofit2.http.*

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

  @DELETE("delete")
  fun delete(): Call<String>

  @FormUrlEncoded
  @POST("post")
  fun post(@Field("ip") ip: String): Call<PostResponse>

  @FormUrlEncoded
  @PUT("put")
  fun put(@Field("ip") ip: String): Call<PostResponse>

  @GET("status/{code}")
  fun status(@Path("code") code: Int): Call<Any>
}