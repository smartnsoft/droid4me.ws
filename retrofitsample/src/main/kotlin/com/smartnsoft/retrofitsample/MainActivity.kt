package com.smartnsoft.retrofitsample

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.smartnsoft.droid4me.app.SmartCommands
import com.smartnsoft.droid4me.ws.WebServiceClient
import com.smartnsoft.retrofitsample.ws.MyWebServiceCaller
import kotlinx.android.synthetic.main.activity_main.*

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

    rootView.setOnClickListener {

      SmartCommands.execute(object : SmartCommands.GuardedCommand<Context>(applicationContext)
      {

        override fun onThrowable(throwable: Throwable): Throwable?
        {
          Log.w(TAG, "Error", throwable)
          return null
        }

        @Throws(Exception::class)
        override fun runGuarded()
        {
          Log.d(TAG, "Launching thread")
          MyWebServiceCaller.getString()?.also {
            Log.d(TAG, it)
          }
          MyWebServiceCaller.getIp()?.also {
            Log.d(TAG, it.origin)
          }
          MyWebServiceCaller.delete().also {
            Log.d(TAG, "deletion complete")
          }
          MyWebServiceCaller.post("127.0.0.1")?.also {
            Log.d(TAG, it.form.ip)
          }
          MyWebServiceCaller.put("127.0.0.2")?.also {
            Log.d(TAG, it.form.ip)
          }
          MyWebServiceCaller.status(200)?.also {
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
        }

      })
    }
  }

}
