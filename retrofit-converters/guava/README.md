Guava Converter
===============

A `Converter` which supports [Guava][1]'s `Optional<T>` by delegating to other converters for `T`
and then wrapping it into `Optional`.


Download
--------

Download [the latest JAR][2] or grab via [Maven][3]:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>converter-guava</artifactId>
  <version>latest.version</version>
</dependency>
```
or [Gradle][3]:
```groovy
implementation 'com.squareup.retrofit2:converter-guava:latest.version'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].


 [1]: https://github.com/google/guava
 [2]: https://search.maven.org/remote_content?g=com.squareup.retrofit2&a=converter-guava&v=LATEST
 [3]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.squareup.retrofit2%22%20a%3A%22converter-guava%22
 [snap]: https://s01.oss.sonatype.org/content/repositories/snapshots/
