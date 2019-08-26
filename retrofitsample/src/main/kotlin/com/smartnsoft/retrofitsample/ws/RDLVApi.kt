package com.smartnsoft.retrofitsample.ws

import com.smartnsoft.retrofitsample.bo.Client
import retrofit2.Call
import retrofit2.http.GET

/**
 * @author Anthony Msihid
 * @since 2019.08.23
 */
interface RDLVApi
{

  companion object Constants
  {

    const val baseRoute: String = "https://api-rdlv.equinoa.net/v1/"

    const val xApiToken: String = "301be45fe04fe22ef42dc00f3ab3429e"

  }

  @GET("clients")
  fun getClients(): Call<List<Client>>

}