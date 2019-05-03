package com.smartnsoft.retrofitsample

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.smartnsoft.droid4me.app.SmartCommands
import com.smartnsoft.retrofitsample.ws.TimeWebServiceCaller
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
          TimeWebServiceCaller.getTime()?.also {
            Log.d(TAG, "getTime: $it")
          }
          /*MyWebServiceCaller.getString()?.also {
            Log.d(TAG, "getString (headers): ${it.headers()}")
            Log.d(TAG, "getString (body): ${it.body()?.string()}")
          }
          MyWebServiceCaller.getIp()?.also {
            Log.d(TAG, "getIp: ${it.origin}")
          }
          MyWebServiceCaller.delete().also {
            Log.d(TAG, "deletion complete: $it")
          }
          MyWebServiceCaller.post("127.0.0.1")?.also {
            Log.d(TAG, "post: ${it.form.ip}")
          }
          MyWebServiceCaller.put("127.0.0.2")?.also {
            Log.d(TAG, "put: ${it.form.ip}")
          }
          MyWebServiceCaller.status(200)?.also {
            Log.d(TAG, "status : ${it.isSuccessful}")
          }
          try
          {
            MyWebServiceCaller.status(400)?.also {
              Log.d(TAG, "status : ${it.isSuccessful}")
            }
          }
          catch (callException: WebServiceClient.CallException)
          {
            Log.d(TAG, "callException: ${callException.message}")
          }*/
        }

      })
    }
  }

}
