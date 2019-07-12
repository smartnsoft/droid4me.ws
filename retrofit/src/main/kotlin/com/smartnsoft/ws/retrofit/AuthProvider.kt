package com.smartnsoft.ws.retrofit

import android.support.annotation.WorkerThread

/**
 * The class description here.
 *
 * @author David Fournier
 * @since 2019.06.21
 */

/**
 * Interface you have to implement to configure the authentication for [AuthJacksonRetrofitWebServiceCaller] service.
 */
interface AuthProvider
{
  /**
   * Method to define the route for the authentication. (eg: "https://www.server.com/auth")
   *
   * @return the route to use for the authentication service.
   */
  @WorkerThread
  fun getAuthRoute(): String

  /**
   * Method to define the XApiKey header for the authentication.
   *
   * @return the XApiKey password.
   */
  @WorkerThread
  fun getXApiKey(): String

  /**
   * Method to retrieve the [AccessToken]. You have to implement the way you retrieve it in the application.
   *
   * @return the [AccessToken] object you want to retrieve.
   */
  @WorkerThread
  fun getAccessToken(): AccessToken?

  /**
   * Method to set the [AccessToken]. You have to implement the way you persist it in the application.
   *
   * @param[accessToken] the [AccessToken] object you want to persist.
   */
  @WorkerThread
  fun setAccessToken(accessToken: AccessToken?)
}