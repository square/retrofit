Change Log
==========

Version 1.7.1 *(2014-10-23)*
----------------------------

 * Fix: Correctly log `null` request arguments for `HEADERS_AND_ARGS` log level.


Version 1.7.0 *(2014-10-08)*
----------------------------

 * New: `RetrofitError`'s `getKind()` now disambiguates the type of error represented.
 * New: `HEADERS_AND_ARGS` log level displays parameters passed to method invocation along with normal
   header list.
 * New: `@Part` and `@PartMap` now support specifying the `Content-Transfer-Encoding` of their respective
   values.
 * New: `@Path`, `@Query`, and `@QueryMap` now have options for enabling or disabling URL encoding on
   names (where appropriate) and values.
 * `@Header` now accepts all object types, invoking `String.valueOf` when neccesary.
 * Attempting to use a `@Path` replacement block (`{name}`) in a query parameter now suggested `@Query` in
   the exception message.
 * Fix: Correct NPE when `Content-Type` override is specified on requests without a body.
 * Fix: `WireConverter` now correctly throws `ConversionException` on incorrect MIME types for parity with
   `ProtoConverter`.
 * Fix: Include `Content-Type` on AppEngine requests.
 * Fix: Account for NPE on AppEngine when the response URL was not automatically populated in certain cases.
 * Fix: `MockRestAdapter`'s RxJava support now correctly schedules work on the HTTP executor, specifically
   when chaining multiple requests together.
 * Experimental RxJava support updated for v0.20.


Version 1.6.1 *(2014-07-02)*
----------------------------

 * Fix: Add any explicitly-specified 'Content-Type' header (via annotation or param) to the request even
   if there is no request body (e.g., DELETE).
 * Fix: Include trailing CRLF in multi-part uploads to work around a bug in .NET MVC 4 parsing.
 * Fix: Allow `null` mock exception bodies and use the success type from the declared service interface.


Version 1.6.0 *(2014-06-06)*
----------------------------

 * New: `@Streaming` on a `Response` type will skip buffering the body to a `byte[]` before delivering.
 * When using OkHttp, version 1.6.0 or newer (including 2.0.0+) is now required.
 * The absence of a response body and an empty body are now differentiated in the log messages.
 * Fix: If set, the `RequestInterceptor` is now applied at the time of `Observable` subscription rather
   than at the time of its creation.
 * Fix: `Callback` subtypes are now supported when using `MockRestAdapter`.
 * Fix: `RetrofitError` now contains a useful message indicating the reason for the failure.
 * Fix: Exceptions thrown when parsing the response type of the interface are now properly propagated.
 * Fix: Calling `Response#getBody` when `null` body now correctly returns instead of throwing an NPE.
 * Experimental RxJava support updated for v0.19.
 * The `Content-Type` and `Content-Length` headers are no longer automatically added to the header list
   on the `Request` object. This reverts erroneous behavior added in v1.5.0. Custom `Client` implementations
   should revert to adding these headers based on the `TypedInput` body of the `Request`.


Version 1.5.1 *(2014-05-08)*
----------------------------

 * New: `@PartMap` annotation accepts a `Map` of key/value pairs for multi-part.
 * Fix: `MockRestAdpater` uses the `ErrorHandler` from its parent `RestAdapter`.
 * Experimental RxJava support updated for v0.18 and is now lazily initialized.


Version 1.5.0 *(2014-03-20)*
----------------------------

 * New: Support for AppEngine's [URL Fetch](https://developers.google.com/appengine/docs/java/urlfetch/)
   HTTP client.
 * New: Multipart requests of unknown length are now supported.
 * New: HTTP `Content-Type` can be overridden with a method-level or paramter header annotation.
 * New: Exceptions from malformed interface methods now include detailed information.
 * Fix: Support empty HTTP response status reason.
 * If an `ErrorHandler` is supplied it will be invoked for `Callback` and `Observable` methods.
 * HTTP `PATCH` method using `HttpUrlConnection` is no longer supported. Add the
   [OkHttp](http://square.github.io/okhttp) jar to your project if you need this behavior.
 * Custom `Client` implementations should no longer set `Content-Type` or `Content-Length` headers
   based on the `TypedInput` body of the `Request`. These headers will now be added automatically
   as part of the standard `Request` header list.


Version 1.4.1 *(2014-02-01)*
----------------------------

 * Fix: `@QueryMap`, `@EncodedFieldMap`, and `@FieldMap` now correctly detect `Map`-based parameter
   types.


Version 1.4.0 *(2014-01-31)*
----------------------------

 * New: `@Query` and `@EncodedQuery` now accept `List` or arrays for multiple values.
 * New: `@QueryMap` and `@EncodedQueryMap` accept a `Map` of key/value pairs for query parameters.
 * New: `@Field` now accepts `List` or arrays for multiple values.
 * New: `@FieldMap` accepts a `Map` of name/value pairs for form URL-encoded request bodies.
 * New: `Endpoint` replaces `Server` as the representation of the remote API root. The `Endpoints`
   utility class contains factories methods for creating instances. `Server` and `ChangeableServer`
   are now deprecated.
 * `SimpleXmlConverter` and `JacksonConverter` now have a default constructor.
 * `Response` now includes the URL.
 * Fix: Hide references to optional classes to prevent over-eager class verifiers from
   complaining (e.g., Dalvik).
 * Fix: Properly detect and reject interfaces which extend from other interfaces.


Version 1.3.0 *(2013-11-25)*
----------------------------

 * New: Converter module for SimpleXML.
 * New: Mock module which allows simulating real network behavior for local service interface
   implementations. See 'mock-github-client' example for a demo.
 * New: RxJava `Observable` support! Declare a return type of `Observable<Foo>` on your service
   interfaces to automatically get an observable for that request. (Experimental API)
 * Fix: Use `ObjectMapper`'s type factory when deserializing (Jackson converter).
 * Multipart POST requests now stream their individual part bodies.
 * Log chunking to 4000 characters now only happens on the Android platform.


Version 1.2.2 *(2013-09-12)*
----------------------------

 * Fix: Respect connection and read timeouts on supplied `OkHttpClient` instances.
 * Fix: Ensure connection is closed on non-200 responses.


Version 1.2.1 *(2013-08-30)*
----------------------------

 * New: Converter for [Wire protocol buffers](http://github.com/square/wire)!


Version 1.2.0 *(2013-08-23)*
----------------------------

 * New: Additional first-party converters for Jackson and Protocol Buffers! These are provided
   as separate modules that you can include and pass to `RestAdapter.Builder`'s `setConverter`.
 * New: `@EncodedPath` and `@EncodedQuery` annotations allow provided path and query params that
   are already URL-encoded.
 * New: `@PATCH` HTTP method annotation.
 * Fix: Properly support custom HTTP method annotations in `UrlConnectionClient`.
 * Fix: Apply `RequestInterceptor` during method invocation rather than at request execution time.
 * Change `setDebug` to `setLogLevel` on `RestAdapter` and `RestAdapter.Builder` and provide
   two levels of logging via `LogLevel`.
 * Query parameters can now be added in a request interceptor.


Version 1.1.1 *(2013-06-25)*
----------------------------

 * Fix: Ensure `@Headers`-defined headers are correctly added to requests.
 * Fix: Supply reasonable connection and read timeouts for default clients.
 * Fix: Allow passing `null` for a `@Part`-annotated argument to remove it from the multipart
   request body.


Version 1.1.0 *(2013-06-20)*
----------------------------

 * Introduce `RequestInterceptor` to replace `RequestHeaders`. An interceptor provided to the
   `RestAdapter.Builder` will be called for every request and allow setting both headers and
   additional path parameter replacements.
 * Add `ErrorHandler` for customizing the exceptions which are thrown when synchronous methods
   return non-200 error codes.
 * Properly parse responses which erroneously omit the "Content-Type" header.


Version 1.0.2 *(2013-05-23)*
----------------------------

 * Allow uppercase letters in path replacement identifiers.
 * Fix: Static query parameters in the URL are now correctly appended with a separating '?'.
 * Fix: Explicitly allow or forbid `null` as a value for method parameters.
   * `@Path` - Forbidden
   * `@Query` - Allowed
   * `@Field` - Allowed
   * `@Part` - Forbidden
   * `@Body` - Forbidden
   * `@Header` - Allowed


Version 1.0.1 *(2013-05-13)*
----------------------------

 * Fix: Correct bad regex behavior on Android.


Version 1.0.0 *(2013-05-13)*
----------------------------

Initial release.
