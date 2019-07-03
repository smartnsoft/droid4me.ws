package com.smartnsoft.retrofitsample.ws

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.smartnsoft.droid4me.ws.WebServiceClient
import com.smartnsoft.retrofitsample.bo.Publication
import com.smartnsoft.ws.retrofit.JacksonRetrofitWebServiceCaller
import okhttp3.Interceptor
import java.io.IOException

/**
 * @author Anthony Msihid
 * @since 2019.06.18
 */

object UNSAWebServiceCaller
  : JacksonRetrofitWebServiceCaller<UNSAAPI>(
    api = UNSAAPI::class.java,
    baseUrl = UNSAAPI.URL,
    withBuiltInCache = BuiltInCache())
{
  @Throws(WebServiceClient.CallException::class, IOException::class, JsonParseException::class, JsonMappingException::class)
  fun getPublication(publicationId: Int?): Publication?
  {
    return execute(Publication::class.java, service.getPublication(publicationId), CachePolicy(FetchPolicyType.NETWORK_THEN_CACHE, UNSAAPI.INFINITE_DATA_RETENTION_DURATION_IN_SECONDS))
  }

  @Throws(WebServiceClient.CallException::class, IOException::class, JsonParseException::class, JsonMappingException::class)
  fun getPublications(fromCache: Boolean): List<Publication>?
  {
    return execute(object: TypeReference<List<Publication>>(){}, service.getPublications(), CachePolicy(if (fromCache) FetchPolicyType.CACHE_THEN_NETWORK else FetchPolicyType.NETWORK_THEN_CACHE, UNSAAPI.RETENTION_PERIOD_IN_SECONDS))
  }

  override fun setupNetworkInterceptors(): List<Interceptor>?
  {
    return listOf(Interceptor { chain ->
      val newRequest = chain.request().newBuilder()
          .addHeader("Xapikey", "d41d8cd98f00b204e9800998ecf8427e")
          .build()

      return@Interceptor chain.proceed(newRequest)
    })
  }
}