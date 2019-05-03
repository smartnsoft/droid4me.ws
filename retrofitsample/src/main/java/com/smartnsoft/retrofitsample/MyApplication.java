package com.smartnsoft.retrofitsample;

import android.app.Application;

import com.smartnsoft.retrofitsample.ws.MyWebServiceCaller;
import com.smartnsoft.retrofitsample.ws.TimeWebServiceCaller;

/**
 * @author David Fournier
 * @since 2018.03.28
 */
public class MyApplication
    extends Application
{

  @Override
  public void onCreate()
  {
    super.onCreate();
    MyWebServiceCaller.INSTANCE.cacheDir(getApplicationContext().getCacheDir());
    TimeWebServiceCaller.INSTANCE.cacheDir(getApplicationContext().getCacheDir());
  }
}
