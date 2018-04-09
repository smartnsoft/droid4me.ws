[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![TeamCity status](https://ci.smartnsoft.com/app/rest/builds/buildType(id:smartnsoft_droid4me_ws_android)/statusIcon)](https://ci.smartnsoft.com/viewType.html?buildTypeId=smartnsoft_droid4me_ws_android)

# droid4me.ws
![image](https://raw.githubusercontent.com/smartnsoft/droid4me.ws/develop/banner.png)

droid4me.ws a is an extension of the Android [droid4me framework](https://github.com/smartnsoft/droid4me) which contains several web service caller implementation.

droid4me.ws consists of several parts :
* _httpclient_ (deprecated) : an implementation based on the [Apache HTTP client](https://hc.apache.org/index.html).
* _okhttp_ : an implementation based on the [Square HTTP client](http://square.github.io/okhttp/)

**Note :** a default implementation based on the `URLConnection` is available into the [droid4me framework](https://github.com/smartnsoft/droid4me).

## Usage

### 1. [DEPRECATED] httpclient

Android 6.0 release removes support for the Apache HTTP client ([source](https://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-apache-http-client))

### 2. okhttp ([wiki](https://github.com/smartnsoft/droid4me.ws/wiki/okhttp))

This implementation is based based on the [Square HTTP client](http://square.github.io/okhttp/) and exposed two classes :
* `OkHttpClientWebServiceCaller` : this class extends the droid4me class `WebServiceCaller` and is responsible to compute the http client, perform the http request, manage the cache and manage errors.
* `JacksonOkHttpClientWebServiceCaller` : this class extends `OkHttpClientWebServiceCaller` and implements the droid4me.ext interface `ObjectMapperComputer`. This class provides an `ObjectMapper` attribute (from the [Jackson library](https://github.com/FasterXML/jackson)) in order to parse JSON.

## Download

To add these implementations to your project, include the following in your **app module** `build.gradle` file:

```groovy
dependencies
{ 
  //okhttp
  implementation ("com.smartnsoft:okhttpwebservicecaller:${latest.version}")
}
```
For the okhttp implementation, `${latest.version}` is [ ![Download](https://api.bintray.com/packages/smartnsoft/maven/okhttpwebservicecaller/images/download.svg) ](https://bintray.com/smartnsoft/maven/okhttpwebservicecaller/_latestVersion)

## License

This library is available under the MIT license. See the LICENSE file for more info.

## Author

This library was proudly made by [Smart&Soft](https://smartnsoft.com/), Paris FRANCE
