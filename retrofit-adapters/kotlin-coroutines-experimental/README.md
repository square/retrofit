Kotlin Coroutine (Experimental) Adapter
=======================================

An `Adapter` for adapting [Kotlin coroutine's][1] `Deferred`.


Usage
-----

Add `KotlinCoroutineCallAdapterFactory` as a `Call` adapter when building your `Retrofit` instance:
```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://example.com/")
    .addCallAdapterFactory(KotlinCoroutineCallAdapter())
    .build()
```

Your service methods can now use `Deferred` as their return type.
```kotlin
interface MyService {
  @GET("/user")
  fun getUser(): Deferred<User>

  // or

  @GET("/user")
  fun getUser(): Deferred<Response<User>>
}
```


Download
--------

Download [the latest JAR][2] or grab via [Maven][3]:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>adapter-kotlin-coroutines-experimental</artifactId>
  <version>latest.version</version>
</dependency>
```
or [Gradle][3]:
```groovy
compile 'com.squareup.retrofit2:adapter-kotlin-coroutines-experimental:latest.version'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].



 [1]: https://kotlinlang.org/docs/reference/coroutines.html
 [2]: https://search.maven.org/remote_content?g=com.squareup.retrofit2&a=adapter-kotlin-coroutines-experimental&v=LATEST
 [3]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.squareup.retrofit2%22%20a%3A%22adapter-kotlin-coroutines-experimental%22
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
