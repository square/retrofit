RxJava 2 Adapter
==============

A `CallAdapter.Factory` which converts Calls to [RxJava 2.X][1] types.

Available types:

 * `Observable<T>`, `Observable<Response<T>>`, and `Observable<Result<T>>` where `T` is the body type.
 * `Flowable<T>`, `Flowable<Response<T>>` and `Flowable<Result<T>>` where `T` is the body type.
 * `Single<T>`, `Single<Response<T>>`, and `Single<Result<T>>`  where `T` is the body type.
 * `Maybe<T>`, `Maybe<Response<T>>`, and `Maybe<Result<T>>`  where `T` is the body type.
 * `Completable` where response bodies are discarded.

Download
--------

Download [the latest JAR][2] or grab via Maven:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>adapter-rxjava2</artifactId>
  <version>see.latest.version</version>
</dependency>
```
or Gradle:
```groovy
compile 'com.squareup.retrofit2:adapter-rxjava2:see.latest.version'
```

 [1]: https://github.com/ReactiveX/RxJava
 [2]: https://search.maven.org/remote_content?g=com.squareup.retrofit2&a=adapter-rxjava2&v=LATEST
