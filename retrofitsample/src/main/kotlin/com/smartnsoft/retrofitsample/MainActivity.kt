package com.smartnsoft.retrofitsample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.smartnsoft.retrofitsample.ws.MyWebServiceCaller
import com.smartnsoft.droid4me.ws.WebServiceClient

class MainActivity :
    AppCompatActivity()
{

  companion object
  {

    const val TAG = "WEB SERVICE"
  }

  override fun onCreate(savedInstanceState: Bundle?)
  {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    Thread(
        {
          MyWebServiceCaller.getString()?.let {
            Log.d(TAG, it)
          }
          MyWebServiceCaller.getIp()?.let {
            Log.d(TAG, it.origin)
          }
          MyWebServiceCaller.delete().let {
            Log.d(TAG, "deletion complete")
          }
          MyWebServiceCaller.post("127.0.0.1")?.let {
            Log.d(TAG, it.form.ip)
          }
          MyWebServiceCaller.put("127.0.0.2")?.let {
            Log.d(TAG, it.form.ip)
          }
          MyWebServiceCaller.status(200)?.let {
            Log.d(TAG, "Successful : ${it.isSuccessful}")
          }
          try
          {
            MyWebServiceCaller.status(400)
          }
          catch (callException: WebServiceClient.CallException)
          {
            Log.d(TAG, callException.message)
          }

        }).start()


  }

}
