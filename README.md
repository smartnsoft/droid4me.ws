# droid4me.ws
Extension of the droid4me framework which contains web service caller implementation

Available implementations :

* httpclient (deprecated)
* okhttp

**Note :** an implementation based on the `URLConnection` is available into the droid4me framework.

## Usage

### From JCenter

Library releases are available on Jcenter

**Gradle**

```groovy
compile 'com.smartnsoft:okhttpwebservicecaller:1.0'
```

**Maven**

```xml
<dependency>
  <groupId>com.smartnsoft</groupId>
  <artifactId>okhttpwebservicecaller</artifactId>
  <version>1.0</version>
  <type>aar</type>
</dependency>
```

### As Library Project

Check out this repository and add it as a library project.

Import the project into your favorite IDE and add `android.library.reference.1=/path/to/droid4me.log/the_choosen_module` to your `project.properties`.

### Generate an aar file

Library releases are not available on Maven Central or JCenter but you can generate an aar file by your owned :

```console
gradle clean choosen_module:assembleRelease
```

or

```console
gradle -b choosen_module/build.gradle clean assembleRelease
```

## Author

The Android Team @Smart&Soft, software agency http://www.smartnsoft.com

## License

droid4me.ws is available under the MIT license. See the LICENSE file for more info.
