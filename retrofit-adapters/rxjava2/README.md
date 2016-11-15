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

Via Maven:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>adapter-rxjava2</artifactId>
  <version>latest.version.here</version>
</dependency>
```
or Gradle:
```groovy
compile 'com.squareup.retrofit2:adapter-rxjava2:latest.version.here'
```

 [1]: https://github.com/ReactiveX/RxJava
