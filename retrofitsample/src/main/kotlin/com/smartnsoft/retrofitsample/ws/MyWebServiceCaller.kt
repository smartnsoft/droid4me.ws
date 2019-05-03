package com.smartnsoft.retrofitsample.ws

import android.annotation.SuppressLint
import android.content.Context
import com.smartnsoft.retrofitsample.bo.IP
import com.smartnsoft.retrofitsample.bo.PostResponse
import com.smartnsoft.ws.retrofit.JacksonRetrofitWebServiceCaller
import okhttp3.Response
import java.io.File

@SuppressLint("StaticFieldLeak")
/**
 *
 * @author David Fournier
 * @since 2017.10.27
 */
object MyWebServiceCaller :
    JacksonRetrofitWebServiceCaller<WSApi>(api = WSApi::class.java, baseUrl = WSApi.url, defaultCachePolicy = CachePolicy.CACHE_THEN_NETWORK, defaultCacheRetentionTimeInSeconds = 30)
{

  fun getString(): Response?
  {
    return executeResponse(service.getString())
  }

  fun getIp(): IP?
  {
    return execute(IP::class.java, service.getIp())
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