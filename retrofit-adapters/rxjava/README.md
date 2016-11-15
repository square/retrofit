RxJava Adapter
==============

A `CallAdapter.Factory` which converts Calls to [RxJava][1] `Observable`s.

```java
@GET("user")
Observable<User> getCurrentUser();
```
If you need both the deserialized object, as well as the Retrofit `Response`:
```java
@GET("user")
Observable<Response<User>> getCurrentUser();
```

Download
--------

Via Maven:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>adapter-rxjava</artifactId>
  <version>latest.version.here</version>
</dependency>
```
or Gradle:
```groovy
compile 'com.squareup.retrofit2:adapter-rxjava:latest.version.here'
```

 [1]: https://github.com/ReactiveX/RxJava
