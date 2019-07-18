package com.smartnsoft.retrofitsample.ws

import com.smartnsoft.ws.retrofit.caller.JacksonRetrofitWebServiceCaller
import okhttp3.Response

/**
 * @author Anthony Msihid
 * @since 2019.05.03
 */
object TimeWebServiceCaller
  : JacksonRetrofitWebServiceCaller<TimeApi>(api = TimeApi::class.java, baseUrl = TimeApi.url)
{
  fun getTime(): Response?
  {
    return executeResponse(service.getTime(1), CachePolicy(FetchPolicyType.CACHE_THEN_NETWORK, cacheRetentionPolicyInSeconds = 20))
  }

  fun getTime2(): Response?
  {
    return executeResponse(service.getTime(2), CachePolicy(FetchPolicyType.CACHE_THEN_NETWORK, cacheRetentionPolicyInSeconds = 20))
  }

  fun getTime3(): Response?
  {
    return executeResponse(service.getTime(3), CachePolicy(FetchPolicyType.CACHE_THEN_NETWORK, cacheRetentionPolicyInSeconds = 30))
  }
}