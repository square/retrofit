Change Log
==========

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
