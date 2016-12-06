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

Download [the latest jar][2] or grab via Maven:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>adapter-rxjava</artifactId>
  <version>see.latest.version</version>
</dependency>
```
or Gradle:
```groovy
compile 'com.squareup.retrofit2:adapter-rxjava:see.latest.version'
```

 [1]: https://github.com/ReactiveX/RxJava
 [2]: https://search.maven.org/remote_content?g=com.squareup.retrofit2&a=adapter-rxjava&v=LATEST
