package com.smartnsoft.ws.retrofit

import com.fasterxml.jackson.databind.ObjectMapper
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 *
 * @author David Fournier
 * @since 2018.03.26
 */
abstract class JacksonRetrofitWebServiceCaller<API>(api: Class<API>, baseUrl: String, connectTimeout: Long = CONNECT_TIMEOUT, readTimeout: Long = READ_TIMEOUT, writeTimeout: Long = WRITE_TIMEOUT, cacheSize: Long = CACHE_SIZE, defaultCachePolicy: CachePolicy = CachePolicy.ONLY_NETWORK) : RetrofitWebServiceCaller<API>(api, baseUrl, converterFactories = arrayOf(JacksonConverterFactory.create(),ScalarsConverterFactory.create()))
{

  private val mapper = ObjectMapper()

  override fun <T> mapResponseToObject(responseBody: String?, clazz: Class<T>): T? {
    return mapper.readValue(responseBody, clazz)
  }
}