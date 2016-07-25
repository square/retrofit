NDJSON Converter
==============

A `Converter` for the [NDJSON][1] format serialization.
There is currently no standard for transporting instances of JSON text within a stream protocol,
apart from `Websockets`, which is unnecessarily complex for non-browser applications.
A common use case for `NDJSON` is delivering multiple instances of JSON text through streaming
protocols like TCP or UNIX Pipes. It can also be used to store semi-structured data.

An instance of `NDJsonConverter` will be created and it will use a `ResponseBody` to create
a `NDJsonResponse` object.
The `NDJsonResponse` object created will split the raw content into valid `JSON` strings, that
will be stored into an array. After that, this content can be parsed using any `JSON` parsing
libraries into the right `DTOs` objects.


 [1]: http://ndjson.org/
