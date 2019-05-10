package com.smartnsoft.retrofitsample.ws

import com.smartnsoft.ws.retrofit.JacksonRetrofitWebServiceCaller

/**
 * @author Anthony Msihid
 * @since 2019.05.03
 */
object TimeWebServiceCaller :
    JacksonRetrofitWebServiceCaller<TimeApi>(api = TimeApi::class.java, baseUrl = TimeApi.url)
{

  fun getTime(): String?
  {
    return execute(service.getTime(), CachePolicy.CACHE_THEN_NETWORK, 10)
  }
}