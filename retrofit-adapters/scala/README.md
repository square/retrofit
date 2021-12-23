Scala Adapter
=============

An `Adapter` for adapting Scala `Future`.


Usage
-----

Add `ScalaCallAdapterFactory` as a `Call` adapter when building your `Retrofit` instance:
```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://example.com/")
    .addCallAdapterFactory(ScalaCallAdapterFactory.create())
    .build();
```

Your service methods can now use `Future` as their return type.
```java
interface MyService {
  @GET("/user")
  Future<User> getUser();
}
```


Download
--------

Download [the latest JAR][2] or grab via [Maven][3]:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>adapter-scala</artifactId>
  <version>latest.version</version>
</dependency>
```
or [Gradle][3]:
```groovy
implementation 'com.squareup.retrofit2:adapter-scala:latest.version'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].



 [2]: https://search.maven.org/remote_content?g=com.squareup.retrofit2&a=adapter-scala&v=LATEST
 [3]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.squareup.retrofit2%22%20a%3A%22adapter-scala%22
 [snap]: https://s01.oss.sonatype.org/content/repositories/snapshots/
