# Response Type Keeper

Generates keep rules for types mentioned in generic parameter positions of Retrofit service methods.

## Problem

Given a service method like
```java
@GET("users/{id}")
Call<User> getUser(
  @Path("id") String id);
```

If you execute this request and do not actually use the returned `User` instance, R8 will remove it
and replace the return type as `Call<?>`. This fails Retrofit's runtime validation since a wildcard
is not a valid type to pass to a converter. Note: this removal only occurs if the Retrofit's service
method definition is the only reference to `User`.

## Solution

This module contains an annotation processor which looks at each Retrofit method and generates
explicit `-keep` rules for the types mentioned.

Add it to Gradle Java projects with
```groovy
annotationProcessor 'com.squareup.retrofit2:response-type-keeper:<version>'
```
Or Gradle Kotlin projects with
```groovy
kapt 'com.squareup.retrofit2:response-type-keeper:<version>'
```

For other build systems, the `com.squareup.retrofit2:response-type-keeper` needs added to the Java
compiler `-processor` classpath.

For the example above, the annotation processor's generated file would contain
```
-keep com.example.User
```

This works for nested generics, such as `Call<ApiResponse<User>>`, which would produce:
```
-keep com.example.ApiResponse
-keep com.example.User
```

It also works on Kotlin `suspend` functions which turn into a type like
`Continuation<? extends User>` in the Java bytecode.
