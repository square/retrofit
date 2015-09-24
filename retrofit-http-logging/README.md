HTTP Logging Interceptor
========================

An [OkHttp interceptor][1] which logs HTTP request and response data.


Usage
-----

```java
OkHttpClient client = new OkHttpClient();
HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
logging.setLevel(Level.BASIC);
client.interceptors().add(logging);

Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("..")
    .client(client)
    .build();
// ...
```


Download
--------

Use the `com.squareup.retrofit:http-logging` coordinates with the latest version in your `pom.xml`
or `build.gradle`.
