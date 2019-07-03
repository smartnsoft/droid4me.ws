package com.smartnsoft.retrofitsample.ws

import com.smartnsoft.retrofitsample.bo.Publication
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * @author Anthony Msihid
 * @since 2019.06.18
 */

interface UNSAAPI
{

  companion object
  {

    const val URL: String = "https://unsapnc.equinoa.net/ws/"

    const val INFINITE_DATA_RETENTION_DURATION_IN_SECONDS: Int = Int.MAX_VALUE

    const val RETENTION_PERIOD_IN_SECONDS: Int = (60 * 60 * 24 * 30 * 2) / 1000

  }

  @GET("${URL}publication/")
  fun getPublication(@Query("publication_id") publicationId: Int?): Call<Publication>

  @GET("${URL}publications")
  fun getPublications(): Call<List<Publication>>

}
