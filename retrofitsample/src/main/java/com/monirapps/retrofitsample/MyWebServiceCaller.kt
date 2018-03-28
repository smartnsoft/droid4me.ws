package com.monirapps.retrofitsample

import android.annotation.SuppressLint
import android.content.Context
import com.smartnsoft.ws.retrofit.JacksonRetrofitWebServiceCaller
import java.io.File

@SuppressLint("StaticFieldLeak")
/**
 *
 * @author David Fournier
 * @since 2017.10.27
 */
object MyWebServiceCaller : JacksonRetrofitWebServiceCaller<WSApi>(api = WSApi::class.java, baseUrl = WSApi.Constants.url) {

  lateinit var context: Context

  fun setup(context: Context) {
    this.context = context
  }

  fun getIp(): IP? {
    return execute(IP::class.java, service.getIp(), CachePolicy.ONLY_CACHE)
  }

  override fun cacheDir(): File?
  {
    return context.cacheDir
  }
}