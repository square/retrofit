Retrofit Adapters
=================

Retrofit ships with a default adapter for executing `Call` instances. The child modules contained
herein are additional adapters for other popular execution mechanisms.

To use, supply an instance of your desired adapter when building your `Retrofit` instance.

```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://api.example.com")
    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
    .build();
```
