package com.smartnsoft.retrofitsample.ws

import com.smartnsoft.retrofitsample.bo.IP
import com.smartnsoft.retrofitsample.bo.PostResponse
import com.smartnsoft.ws.retrofit.JacksonRetrofitWebServiceCaller
import okhttp3.Response

/**
 *
 * @author David Fournier
 * @since 2017.10.27
 */
object MyWebServiceCaller
  : JacksonRetrofitWebServiceCaller<WSApi>(api = WSApi::class.java, baseUrl = WSApi.url)
{
  fun getString(): String?
  {
    return execute(service.getString())
  }

  fun getIp(): IP?
  {
    return execute(IP::class.java, service.getIp(), CachePolicy(FetchPolicyType.CACHE_THEN_NETWORK, cacheRetentionPolicyInSeconds = 20, customKey = "ip"))
  }

  fun getIp2(): IP?
  {
    return execute(IP::class.java, service.getIp(), CachePolicy(FetchPolicyType.CACHE_THEN_NETWORK, allowedTimeExpiredCacheInSeconds = 100, customKey = "ip2"))
  }

  fun getIp3(): IP?
  {
    return execute(IP::class.java, service.getIp(), CachePolicy(FetchPolicyType.CACHE_THEN_NETWORK, allowedTimeExpiredCacheInSeconds = 100, customKey = "ip3"))
  }

  fun delete(): String?
  {
    return execute(service.delete())
  }

  fun post(ip: String): PostResponse?
  {
    return execute(PostResponse::class.java, service.post(ip))
  }

  fun put(ip: String): PostResponse?
  {
    return execute(PostResponse::class.java, service.put(ip))
  }

  fun status(code: Int): Response?
  {
    return executeResponse(service.status(code))
  }
}