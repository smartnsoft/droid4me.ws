[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![TeamCity status](https://ci-mobile.cyllene.co/app/rest/builds/buildType(id:smartnsoft_droid4me_ws_android)/statusIcon)](https://ci-mobile.cyllene.co/viewType.html?buildTypeId=smartnsoft_droid4me_ws_android)

# droid4me.ws
![image](https://raw.githubusercontent.com/smartnsoft/droid4me.ws/develop/banner.png)

droid4me.ws a is an extension of the Android [droid4me framework](https://github.com/smartnsoft/droid4me) which contains several web service caller implementation.

droid4me.ws consists of several parts :
* _httpclient_ (deprecated) : an implementation based on the [Apache HTTP client](https://hc.apache.org/index.html).
* _okhttp_ : an implementation based on the [Square HTTP client](http://square.github.io/okhttp/)
* _retrofit_ : an implementation based on the [Retrofit library](https://square.github.io/retrofit/)

**Note :** a default implementation based on the `URLConnection` is available into the [droid4me framework](https://github.com/smartnsoft/droid4me).

## Usage

### 1. [DEPRECATED] httpclient

Android 6.0 release removes support for the Apache HTTP client ([source](https://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-apache-http-client))

### 2. okhttp ([wiki](https://github.com/smartnsoft/droid4me.ws/wiki/okhttp))

This implementation is based based on the [Square HTTP client](http://square.github.io/okhttp/) and exposes two classes :
* `OkHttpClientWebServiceCaller` : this class extends the droid4me class `WebServiceCaller` and is responsible for computing the http client, performing the http request, managing the cache and managing errors.
* `JacksonOkHttpClientWebServiceCaller` : this class extends `OkHttpClientWebServiceCaller` and implements the droid4me.ext interface `ObjectMapperComputer`. It also provides an `ObjectMapper` attribute (from the [Jackson library](https://github.com/FasterXML/jackson)) in order to parse JSON.

### 3. Retrofit
This implementation is based based on [Retrofit library](http://square.github.io/retrofit/) and exposes two classes :
* `RetrofitWebServiceCaller` : this class is responsible for computing the http client, performing http requests via retrofitAPI, managing cache and errors.
* `JacksonRetrofitWebServiceCaller` : this class extends `RetrofitWebServiceCaller` and provides an `ObjectMapper` attribute (from the [Jackson library](https://github.com/FasterXML/jackson)) in order to parse JSON.

## Download

To add these implementations to your project, include the following in your **app module** `build.gradle` file:

```groovy
dependencies
{ 
  //okhttp
  implementation ("com.smartnsoft:okhttpwebservicecaller:${latest.version}")
  
  //retrofit
  implementation ("com.smartnsoft:retrofitwebservicecaller:${latest.version}")
}
```
For the okhttp implementation, `${latest.version}` is [ ![Download](https://api.bintray.com/packages/smartnsoft/maven/okhttpwebservicecaller/images/download.svg) ](https://bintray.com/smartnsoft/maven/okhttpwebservicecaller/_latestVersion)

For the retrofit implementation, `${latest.version}` is [ ![Download](https://api.bintray.com/packages/smartnsoft/maven/retrofitwebservicecaller/images/download.svg) ](https://bintray.com/smartnsoft/maven/retrofitwebservicecaller/_latestVersion)

## License

This library is available under the MIT license. See the LICENSE file for more info.

## Author

This library was proudly made by [Smart&Soft](https://smartnsoft.com/), Paris FRANCE
