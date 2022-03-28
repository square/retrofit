Retrofit
========

A type-safe HTTP client for Android and Java.

For more information please see [the website][1].


Download
--------

Download [the latest JAR][2] or grab from Maven central at the coordinates `com.squareup.retrofit2:retrofit:2.9.0`.

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].

Retrofit requires at minimum Java 8+ or Android API 21+.


R8 / ProGuard
-------------

If you are using R8 the shrinking and obfuscation rules are included automatically.

ProGuard users must manually add the options from
[retrofit2.pro][proguard file].
You might also need [rules for OkHttp][okhttp proguard] and [Okio][okio proguard] which are dependencies of this library.


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
 [snap]: https://s01.oss.sonatype.org/content/repositories/snapshots/
 [proguard file]: https://github.com/square/retrofit/blob/master/retrofit/src/main/resources/META-INF/proguard/retrofit2.pro
 [okhttp proguard]: https://square.github.io/okhttp/r8_proguard/
 [okio proguard]: https://square.github.io/okio/#r8-proguard
