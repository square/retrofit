RxJava2 Adapter
==============

An `Adapter` for adapting [RxJava 2.x][1] types.

Available types:

 * `Observable<T>`, `Observable<Response<T>>`, and `Observable<Result<T>>` where `T` is the body type.
 * `Flowable<T>`, `Flowable<Response<T>>` and `Flowable<Result<T>>` where `T` is the body type.
 * `Single<T>`, `Single<Response<T>>`, and `Single<Result<T>>`  where `T` is the body type.
 * `Maybe<T>`, `Maybe<Response<T>>`, and `Maybe<Result<T>>`  where `T` is the body type.
 * `Completable` where response bodies are discarded.


Usage
-----

Add `RxJava2CallAdapterFactory` as a `Call` adapter when building your `Retrofit` instance:
```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://example.com/")
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    .build();
```

Your service methods can now use any of the above types as their return type.
```java
interface MyService {
  @GET("/user")
  Observable<User> getUser();
}
```

By default all reactive types execute their requests synchronously. There are multiple ways to
control the threading on which a request occurs:

 * Call `subscribeOn` on the returned reactive type with a `Scheduler` of your choice.
 * Use `createAsync()` when creating the factory which will use OkHttp's internal thread pool.
 * Use `createWithScheduler(Scheduler)` to supply a default subscription `Scheduler`.

Download
--------

Download [the latest JAR][2] or grab via [Maven][3]:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>adapter-rxjava2</artifactId>
  <version>latest.version</version>
</dependency>
```
or [Gradle][3]:
```groovy
implementation 'com.squareup.retrofit2:adapter-rxjava2:latest.version'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].



 [1]: https://github.com/ReactiveX/RxJava/tree/2.x
 [2]: https://search.maven.org/remote_content?g=com.squareup.retrofit2&a=adapter-rxjava2&v=LATEST
 [3]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.squareup.retrofit2%22%20a%3A%22adapter-rxjava2%22
 [snap]: https://s01.oss.sonatype.org/content/repositories/snapshots/
