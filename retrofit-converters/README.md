Retrofit Converters
===================

Retrofit ships with support for OkHttp's `RequestBody` and `ResponseBody` types but the library is
content-format agnostic. The child modules contained herein are additional converters for other
popular formats.

To use, supply an instance of your desired converter when building your `Retrofit` instance.

```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://api.example.com")
    .addConverterFactory(GsonConverterFactory.create())
    .build();
```
