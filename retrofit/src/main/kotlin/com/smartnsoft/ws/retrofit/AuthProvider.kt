package com.smartnsoft.ws.retrofit

import android.support.annotation.WorkerThread

/**
 * The class description here.
 *
 * @author David Fournier
 * @since 2019.06.21
 */
interface AuthProvider
{
  @WorkerThread
  fun getAuthRoute(): String

  @WorkerThread
  fun getXApiKey(): String

  @WorkerThread
  fun getAccessToken(): AccessToken?

  @WorkerThread
  fun setAccessToken(accessToken: AccessToken?)
}