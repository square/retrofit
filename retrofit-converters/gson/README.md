Gson Converter
==============

A `Converter` which uses [Gson][1] for serialization to and from JSON.

A default `Gson` instance will be created or one can be configured and passed to the
`GsonConverter` construction to further control the serialization.

Download
--------

Download [the latest JAR][2] or grab via [Maven][3]:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>converter-gson</artifactId>
  <version>see.latest.version</version>
</dependency>
```
or [Gradle][2]:
```groovy
compile 'com.squareup.retrofit2:converter-gson:see.latest.version'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].

 [1]: https://github.com/google/gson
 [2]: https://search.maven.org/remote_content?g=com.squareup.retrofit2&a=converter-gson&v=LATEST
 [3]: http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22converter-gson%22
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
