package com.smartnsoft.retrofitsample.ws

import com.smartnsoft.ws.retrofit.JacksonRetrofitWebServiceCaller

/**
 * @author Anthony Msihid
 * @since 2019.05.03
 */
object TimeWebServiceCaller :
    JacksonRetrofitWebServiceCaller<TimeApi>(api = TimeApi::class.java, baseUrl = TimeApi.url, defaultCachePolicy = CachePolicy.CACHE_THEN_NETWORK, defaultCacheRetentionTimeInSeconds = 2000)
{

  fun getTime(): String?
  {
    return execute(service.getTime(), withCachePolicy = CachePolicy.ONLY_CACHE)
  }
}