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
    return execute(service.getTime(1), CachePolicy(cacheRetentionPolicyInSeconds = 20, customKey = "lol"))
  }

  fun getTime2(): String?
  {
    return execute(service.getTime(2), CachePolicy(CachePolicyType.ONLY_CACHE, allowedTimeExpiredCacheInSeconds = 100, customKey = "lol"))
  }

}