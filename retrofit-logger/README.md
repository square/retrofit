Retrofit Logger
===================

Retrofit ships with support for object logging to increase readability for case like response is binary and can't log in OkHttp layer.

To use, supply an instance of your desired logger when building your `Retrofit` instance.

```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://api.example.com")
    .setObjectLogger(GsonLogger.create())
    .build();
```
