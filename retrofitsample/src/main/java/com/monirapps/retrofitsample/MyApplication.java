package com.monirapps.retrofitsample;

import android.app.Application;

/**
 * The class description here.
 *
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
    MyWebServiceCaller.INSTANCE.setup(getApplicationContext());
  }
}
