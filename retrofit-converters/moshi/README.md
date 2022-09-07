Moshi Converter
===============

A `Converter` which uses [Moshi][1] for serialization to and from JSON.

A default `Moshi` instance will be created or one can be configured and passed to
`MoshiConverterFactory.create()` to further control the serialization.


Download
--------

Download [the latest JAR][2] or grab via [Maven][3]:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>converter-moshi</artifactId>
  <version>latest.version</version>
</dependency>
```
or [Gradle][3]:
```groovy
implementation 'com.squareup.retrofit2:converter-moshi:latest.version'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].



 [1]: https://github.com/square/moshi
 [2]: https://search.maven.org/remote_content?g=com.squareup.retrofit2&a=converter-moshi&v=LATEST
 [3]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.squareup.retrofit2%22%20a%3A%22converter-moshi%22
 [snap]: https://s01.oss.sonatype.org/content/repositories/snapshots/
