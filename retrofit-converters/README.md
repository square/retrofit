Retrofit Converters
===================

Retrofit ships with a default converter for JSON that uses Gson but the library is content-format
agnostic. The child modules contained herein are additional converters for other popular formats.

To use, supply an instance of your desired converter when building your `RestAdapter` instance.

```java
RestAdapter restAdapter = new RestAdapter.Builder()
    .setServer("https://api.fake.google.com")
    .setConverter(new ProtoConverter())
    .build();
```
