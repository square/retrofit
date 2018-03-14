Retrofit
========

Type-safe HTTP client for Android and Java by Square, Inc.

For more information please see [the website][1].


Download
--------

Download [the latest JAR][2] or grab via Maven:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>retrofit</artifactId>
  <version>2.4.0</version>
</dependency>
```
or Gradle:
```groovy
implementation 'com.squareup.retrofit2:retrofit:2.4.0'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].

Retrofit requires at minimum Java 7 or Android 2.3.


ProGuard
--------

If you are using ProGuard you need to add the following options:
```
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain service method parameters.
-keepclassmembernames,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
```

You might also need rules for OkHttp and Okio which are dependencies.


License
=======

    Copyright 2013 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


 [1]: https://square.github.io/retrofit/
 [2]: https://search.maven.org/remote_content?g=com.squareup.retrofit2&a=retrofit&v=LATEST
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
