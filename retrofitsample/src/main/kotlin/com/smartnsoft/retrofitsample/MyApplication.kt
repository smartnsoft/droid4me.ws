package com.smartnsoft.retrofitsample

import android.app.Application

import com.smartnsoft.retrofitsample.ws.MyWebServiceCaller
import com.smartnsoft.retrofitsample.ws.TimeWebServiceCaller
import com.smartnsoft.retrofitsample.ws.UNSAWebServiceCaller

/**
 * @author David Fournier
 * @since 2018.03.28
 */
class MyApplication : Application()
{
  override fun onCreate()
  {
    super.onCreate()
    MyWebServiceCaller.setupCache(applicationContext.cacheDir, "MyService")
    TimeWebServiceCaller.setupCache(applicationContext.cacheDir, "TimeService")
    UNSAWebServiceCaller.setupCache(applicationContext.cacheDir, "Unsa")
  }
}
