# kotlinx.serialization Converter

A `Converter` which uses [kotlinx.serialization.json][1] for serialization.

Given a `Json`, call `asConverterFactory()` in order to
create a `Converter.Factory`.

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://example.com/")
    .addConverterFactory(Json.asConverterFactory())
    .build()
```


## Download

Download [the latest JAR][2] or grab via [Maven][3]:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>converter-kotlinx-serialization-json</artifactId>
  <version>latest.version</version>
</dependency>
```
or [Gradle][3]:
```groovy
implementation 'com.squareup.retrofit2:converter-kotlinx-serialization-json:latest.version'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].



 [1]: https://github.com/Kotlin/kotlinx.serialization
 [2]: https://search.maven.org/remote_content?g=com.squareup.retrofit2&a=converter-kotlinx-serialization-json&v=LATEST
 [3]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.squareup.retrofit2%22%20a%3A%22converter-kotlinx-serialization-json%22
 [snap]: https://s01.oss.sonatype.org/content/repositories/snapshots/
