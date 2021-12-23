RxJava3 Adapter
==============

An `Adapter` for adapting [RxJava 3.x][1] types.

Available types:

 * `Observable<T>`, `Observable<Response<T>>`, and `Observable<Result<T>>` where `T` is the body type.
 * `Flowable<T>`, `Flowable<Response<T>>` and `Flowable<Result<T>>` where `T` is the body type.
 * `Single<T>`, `Single<Response<T>>`, and `Single<Result<T>>`  where `T` is the body type.
 * `Maybe<T>`, `Maybe<Response<T>>`, and `Maybe<Result<T>>`  where `T` is the body type.
 * `Completable` where response bodies are discarded.


Usage
-----

Add `RxJava3CallAdapterFactory` as a `Call` adapter when building your `Retrofit` instance:
```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://example.com/")
    .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
    .build();
```

Your service methods can now use any of the above types as their return type.
```java
interface MyService {
  @GET("/user")
  Observable<User> getUser();
}
```

By default, `create()` will produce reactive types which execute their HTTP requests asynchronously
on a background thread. There are two other ways to control the threading on which a request
occurs:

 * Use `createSynchronous()` and call `subscribeOn` on the returned reactive type with a `Scheduler`
   of your choice.
 * Use `createWithScheduler(Scheduler)` to supply a default subscription `Scheduler`.

Download
--------

Download [the latest JAR][2] or grab via [Maven][3]:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>adapter-rxjava3</artifactId>
  <version>latest.version</version>
</dependency>
```
or [Gradle][3]:
```groovy
implementation 'com.squareup.retrofit2:adapter-rxjava3:latest.version'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].



 [1]: https://github.com/ReactiveX/RxJava/tree/3.x
 [2]: https://search.maven.org/remote_content?g=com.squareup.retrofit2&a=adapter-rxjava3&v=LATEST
 [3]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.squareup.retrofit2%22%20a%3A%22adapter-rxjava3%22
 [snap]: https://s01.oss.sonatype.org/content/repositories/snapshots/
