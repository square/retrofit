Change Log
==========

Version 2.9.0 *(2020-05-20)*
----------------------------

 * New: RxJava 3 adapter!

   The Maven coordinates are `com.squareup.retrofit2:adapter-rxjava3`.

   Unlike the RxJava 1 and RxJava 2 adapters, the RxJava 3 adapter's `create()` method will produce asynchronous HTTP requests by default. For synchronous requests use `createSynchronous()` and for synchronous on a scheduler use `createWithScheduler(..)`.


Version 2.8.2 *(2020-05-18)*
----------------------------

 * Fix: Detect running on the Android platform by using system property rather than the presence of classes.
   This ensures that even when you're running on the JVM with Android classes present on the classpath you
   get JVM semantics.
 * Fix: Update to OkHttp 3.14.9 which contains an associated Android platform detection fix.


Version 2.8.1 *(2020-03-25)*
----------------------------

 * Fix: Do not access `MethodHandles.Lookup` on Android API 24 and 25. The class is only available
   on Android API 26 and higher.


Version 2.8.0 *(2020-03-23)*
----------------------------

 * New: Add `Call.timeout()` which returns the `okio.Timeout` of the full call.
 * Fix: Change `Call.awaitResponse()` to accept a nullable response type.
 * Fix: Support default methods on Java 14+. We had been working around a bug in earlier versions of
   Java. That bug was fixed in Java 14, and the fix broke our workaround.


Version 2.7.2 *(2020-02-24)*
----------------------------

 * Fix: Update to OkHttp 3.14.7 for compatibility with Android R (API 30).


Version 2.7.1 *(2020-01-02)*
----------------------------

 * Fix: Support 'suspend' functions in services interfaces when using 'retrofit-mock' artifact.


Version 2.7.0 *(2019-12-09)*
----------------------------

**This release changes the minimum requirements to Java 8+ or Android 5+.**
See [this blog post](https://cashapp.github.io/2019-02-05/okhttp-3-13-requires-android-5) for more information on the change.

 * New: Upgrade to OkHttp 3.14.4. Please see [its changelog for 3.x](https://square.github.io/okhttp/changelog_3x/).
 * Fix: Allow service interfaces to extend other interfaces.
 * Fix: Ensure a non-null body is returned by `Response.error`.


Version 2.6.4 *(2020-01-02)*
----------------------------

 * Fix: Support 'suspend' functions in services interfaces when using 'retrofit-mock' artifact.


Version 2.6.3 *(2019-12-09)*
----------------------------

 * Fix: Change mechanism for avoiding `UndeclaredThrowableException` in rare cases from using `yield`
   an explicit dispatch which ensures that it will work even on dispatchers which do not support yielding.


Version 2.6.2 *(2019-09-23)*
----------------------------

 * Fix: Avoid `IOException`s being wrapped in `UndeclaredThrowableException` in rare cases when using
   `Response<..>` as a return type for Kotlin 'suspend' functions.


Version 2.6.1 *(2019-07-31)*
----------------------------

 * Fix: Avoid `IOException`s being wrapped in `UndeclaredThrowableException` in rare cases.
 * Fix: Include no-content `ResponseBody` for responses created by `Response.error`.
 * Fix: Update embedded R8/ProGuard rules to not warn about nested classes used for Kotlin extensions.


Version 2.6.0 *(2019-06-05)*
----------------------------

 * New: Support `suspend` modifier on functions for Kotlin! This allows you to express the asynchrony of HTTP requests
   in an idiomatic fashion for the language.

   ```kotlin
   @GET("users/{id}")
   suspend fun user(@Path("id") id: Long): User
   ```

   Behind the scenes this behaves as if defined as `fun user(...): Call<User>` and then invoked with `Call.enqueue`.
   You can also return `Response<User>` for access to the response metadata.

   Currently this integration only supports non-null response body types. Follow
   [issue 3075](https://github.com/square/retrofit/issues/3075) for nullable type support.

 * New: **`@Tag`** parameter annotation for setting tags on the underlying OkHttp `Request` object. These can be read
   in `CallAdapter`s or OkHttp `Interceptor`s for tracing, analytics, varying behavior, and more.

 * New: **`@SkipCallbackExecutor`** method annotation will result in your `Call` invoking its `Callback` on the
   background thread on which the HTTP call was made.

 * New: Support OkHttp's `Headers` type for `@HeaderMap` parameters.

 * New: Add `Retrofit.Builder.baseUrl(URL)` overload.

 * Fix: Add embedded R8/ProGuard rule which retains Retrofit interfaces (while still allowing obfuscation). This
   is needed because R8 running in 'full mode' (i.e., not in ProGuard-compatibility mode) will see that there are
   no subtypes of these interfaces and rewrite any code which references instances to null.
 * Fix: Mark `HttpException.response()` as `@Nullable` as serializing the exception does not retain this instance.
 * Fix: Fatal errors (such as stack overflows, out of memory, etc.) now propagate to the OkHttp `Dispatcher` thread
   on which they are running.
 * Fix: Ensure JAX-B converter closes the response body when an exception is thrown during deserialization.
 * Fix: Ignore static methods when performing eager validation of interface methods.
 * Fix: Ensure that calling `source()` twice on the `ResponseBody` passed to a `Converter` always returns the same
   instance. Prior to the fix, intermediate buffering would cause response data to be lost.


Version 2.5.0 *(2018-11-18)*
----------------------------

 * New: Built-in support for Kotlin's `Unit` type. This behaves the same as Java's `Void` where the body
   content is ignored and immediately discarded.
 * New: Built-in support for Java 8's `Optional` and `CompletableFuture` types. Previously the 'converter-java8'
   and 'adapter-java8' dependencies were needed and explicitly adding `Java8OptionalConverterFactory` and/or
   `Java8CallAdapterFactory` to your `Retrofit.Builder` in order to use these types. Support is now built-in and
   those types and their artifacts are marked as deprecated.
 * New: `Invocation` class provides a reference to the invoked method and argument list as a tag on the
   underlying OkHttp `Call`. This can be accessed from an OkHttp interceptor for things like logging, analytics,
   or metrics aggregation.
 * New: Kotlin extension for `Retrofit` which allows you call `create` passing the interface type only as
   a generic parameter (e.g., `retrofit.create<MyService>()`).
 * New: Added `Response.success` overload which allows specifying a custom 2xx status code.
 * New: Added `Calls.failure` overload which allows passing any `Throwable` subtype.
 * New: Minimal R8 rules now ship inside the jar requiring no client configuration in the common case.
 * Fix: Do not propagate fatal errors to the callback. They are sent to the thread's uncaught
   exception handler.
 * Fix: Do not enqueue/execute an otherwise useless call when the RxJava type is disposed by `onSubscribe`.
 * Fix: Call `RxJavaPlugins` assembly hook when creating an RxJava 2 type.
 * Fix: Ensure both the Guava and Java 8 `Optional` converters delegate properly. This ensures that converters
   registered prior to the optional converter can be used for deserializing the body type.
 * Fix: Prevent `@Path` values from participating in path-traversal. This ensures untrusted input passed as
   a path value cannot cause you to make a request to an un-intended relative URL.
 * Fix: Simple XML converter (which is deprecated) no longer wraps subtypes of `RuntimeException`
   or `IOException` when it fails.
 * Fix: Prevent JAXB converter from loading remote entities and DTDs.
 * Fix: Correctly detect default methods in interfaces on Android (API 24+). These still do not work, but
   now a correct exception will be thrown when detected.
 * Fix: Report more accurate exceptions when a `@QueryName` or `@QueryMap` precedes a `@Url` parameter.
 * Update OkHttp dependency to 3.12.


Version 2.4.0 *(2018-03-14)*
----------------------------

 * New: `Retrofit.Builder` exposes mutable lists of the added converter and call adapter factories.
 * New: Call adapter added for Scala's `Future`.
 * New: Converter for JAXB replaces the now-deprecated converter for Simple XML Framework.
 * New: Add Java 9 automatic module names for each artifact corresponding to their root package.
 * Fix: Do not swallow `Error`s from callbacks (usually `OutOfMemoryError`).
 * Fix: Moshi and Gson converters now assert that the full response was consumed. This prevents
   hiding bugs in faulty adapters which might not have consumed the full JSON input which would
   then cause failures on the next request over that connection.
 * Fix: Do not conflate OkHttp `Call` cancelation with RxJava unsubscription/disposal. Prior to
   this change, canceling of a `Call` would prevent a cancelation exception from propagating down
   the Rx stream.


Version 2.3.0 *(2017-05-13)*
----------------------------

 *  **Retrofit now uses `@Nullable` to annotate all possibly-null values.** We've
    added a compile-time dependency on the JSR 305 annotations. This is a
    [provided][maven_provided] dependency and does not need to be included in
    your build configuration, `.jar` file, or `.apk`. We use
    `@ParametersAreNonnullByDefault` and all parameters and return types are
    never null unless explicitly annotated `@Nullable`.

    **Warning: this release is source-incompatible for Kotlin users.**
    Nullability was previously ambiguous and lenient but now the compiler will
    enforce strict null checks.

 * New: Converters added for Java 8's and Guava's `Optional` which wrap a potentially-nullable
   response body. These converters still rely on normal serialization library converters for parsing
   the response bytes into an object.
 * New: String converters that return `null` for an `@Query` or `@Field` parameter are now skipped.
 * New: The mock module's `NetworkBehavior` now throws a custom subclass of `IOException` to more
   clearly indicate the exception's source.
 * RxJava 1.x converter updated to 1.3.0 which stabilizes the use of `Completable`.
 * Fix: Add explicit handling for `OnCompleteFailedException`, `OnErrorFailedException`, and
   `OnErrorNotImplementedException` for RxJava 1.x to ensure they're correct delivered to the
   plugins/hooks for handling.
 * Fix: `NoSuchElementException` thrown when unsubscribing from an RxJava 1.x `Single`.


Version 2.2.0 *(2017-02-21)*
----------------------------

 * RxJava 2.x is now supported with a first-party 'adapter-rxjava2' artifact.
 * New: `@QueryName` annotation allows creating a query parameter with no '=' separator or value.
 * New: Support for messages generated by Protobuf 3.0 or newer when using the converter for Google's
   protobuf.
 * New: RxJava 1.x call adapter now correctly handles broken subscribers whose methods throw exceptions.
 * New: Add `toString()` implementations for `Response` and `Result`.
 * New: The Moshi converter factory now offers methods for enabling null serialization and lenient
   parsing.
 * New: Add `createAsync()` to RxJava 1.x call adapter factory which executes requests using
   `Call.enqueue()` using the underlying HTTP client's asynchronous support.
 * New: `NetworkBehavior` now allows setting an error percentage and returns HTTP errors when triggered.
 * `HttpException` has been moved into the main artifact and should be used instead of the versions
   embedded in each adapter (which have been deprecated).
 * Promote the response body generic type on `CallAdapter` from the `adapt` method to the enclosing
   class. This is a source-incompatible but binary-compatible change which is only relevant if you are
   implementing your own `CallAdapter`s.
 * Remove explicit handling of the now-defunct RoboVM platform.
 * Fix: Close response on HTTP 204 and 205 to avoid resource leak.
 * Fix: Reflect the canceled state of the HTTP client's `Call` in Retrofit's `Call`.
 * Fix: Use supplied string converters for the `String` type on non-body parameters. This allows user
   converters to handle cases such as when annotating string parameters instead of them always using
   the raw string.
 * Fix: Skip a UTF-8 BOM (if present) when using the converter for Moshi.


Version 2.1.0 *(2016-06-15)*
----------------------------

 * New: `@HeaderMap` annotation and support for supplying an arbitrary number of headers to an endpoint.
 * New: `@JsonAdapter` annotations on the `@Body` parameter and on the method will be propagated to Moshi
   for creating the request and response adapters, respectively.
 * Fix: Honor the `Content-Type` encoding of XML responses when deserializing response bodies.
 * Fix: Remove the stacktrace from fake network exceptions created from retrofit-mock's `NetworkBehavior`.
   They had the potential to be misleading and look like a library issue.
 * Fix: Eagerly catch malformed `Content-Type` headers supplied via `@Header` or `@Headers`.


Version 2.0.2 *(2016-04-14)*
----------------------------

 * New: `ProtoConverterFactory.createWithRegistry()` method accepts an extension registry to be used
   when deserializing protos.
 * Fix: Pass the correct `Call` instance to `Callback`'s `onResponse` and `onFailure` methods such
   that calling `clone()` retains the correct threading behavior.
 * Fix: Reduce the per-request allocation overhead for the RxJava call adapter.


Version 2.0.1 *(2016-03-30)*
----------------------------

 * New: Support OkHttp's `HttpUrl` as a `@Url` parameter type.
 * New: Support iterable and array `@Part` parameters using OkHttp's `MultipartBody.Part`.
 * Fix: Honor backpressure in `Observable`s created from the RxJavaCallAdapterFactory.


Version 2.0.0 *(2016-03-11)*
----------------------------

Retrofit 2 is a major release focused on extensibility. The API changes are numerous but solve
shortcomings of the previous version and provide a path for future enhancement.

Because the release includes breaking API changes, we're changing the project's package name from
`retrofit` to `retrofit2`. This should make it possible for large applications and libraries to
migrate incrementally. The Maven group ID is now `com.squareup.retrofit2`. For an explanation of
this strategy, see Jake Wharton's post, [Java Interoperability Policy for Major Version
Updates](http://jakewharton.com/java-interoperability-policy-for-major-version-updates/).

 * **Service methods return `Call<T>`.** This allows them to be executed synchronously or
   asynchronously using the same method definition. A `Call` instance represents a single
   request/response pair so it can only be used once, but you can `clone()` it for re-use.
   Invoking `cancel()` will cancel in-flight requests or prevent the request from even being
   performed if it has not already.
 
 * **Multiple converters for multiple serialization formats.** API calls returning different
  formats (like JSON, protocol buffers, and plain text) no longer need to be separated into
  separate service interfaces. Combine them together and add multiple converters. Converters are
  chosen based on the response type you declare. Gson is no longer included by default, so you will
  always need to add a converter for any serialization support. OkHttp's `RequestBody` and
  `ResponseBody` types can always be used without adding one, however.
   
 * **Call adapters allow different execution mechanisms.** While `Call` is the built-in mechanism,
   support for additional ones can be added similar to how different converters can be added.
   RxJava's `Observable` support has moved into a separate artifact as a result, and support for
   Java 8's `CompletableFuture` and Guava's `ListenableFuture` are also provided as additional
   artifacts.
   
 * **Generic response type includes HTTP information and deserialized body.** You no longer have to
   choose between the deserialized body and reading HTTP information. Every `Call` automatically
   receives both via the `Response<T>` type and the RxJava, Guava, and Java 8 call adapters also
   support it.
   
 * **@Url for hypermedia-like APIs.** When your API returns links for pagination, additional
   resources, or updated content they can now be used with a service method whose first parameter
   is annotated with `@Url`.

Changes from beta 4:

 * New: `RxJavaCallAdapterFactory` now supports service methods which return `Completable` which
   ignores and discards response bodies, if any.
 * New: `RxJavaCallAdapterFactory` supports supplying a default `Scheduler` which will be used
   for `subscribeOn` on returned `Observable`, `Single`, and `Completable` instances.
 * New: `MoshiConverterFactory` supports creating an instance which uses lenient parsing.
 * New: `@Part` can omit the part name and use OkHttp's `MultipartBody.Part` type for supplying
   parts. This lets you customize the headers, name, and filename and provide the part body in a
   single argument.
 * The `BaseUrl` interface and support for changeable base URLs was removed. This functionality
   can be done using an OkHttp interceptor and a sample showcasing it was added.
 * `Response.isSuccess()` was renamed to `Response.isSuccessful()` for parity with the name of
   OkHttp's version of that method.
 * Fix: Throw a more appropriate exception with a message when a resolved url (base URL + relative
   URL) is malformed.
 * Fix: `GsonConverterFactory` now honors settings on the `Gson` instance (like leniency).
 * Fix: `ScalarsConverterFactory` now supports primitive scalar types in addition to boxed for
   response body parsing.
 * Fix: `Retrofit.callbackExecutor()` may now return an executor even when one was not explicitly
   provided. This allows custom `CallAdapter.Factory` implementations to use it when triggering
   callbacks to ensure they happen on the appropriate thread for the platform (e.g., Android).


Version 2.0.0-beta4 *(2016-02-04)*
----------------------------------

 * New: `Call` instance is now passed to both `onResponse` and `onFailure` methods of `Callback`. This aids
   in detecting when `onFailure` is called as a result of `Call.cancel()` by checking `Call.isCanceled()`.
 * New: `Call.request()` returns (optionally creating) the `Request` object for the call. Note: If this is
   called before `Call.execute()` or `Call.enqueue()` this will do relatively expensive work synchronously.
   Doing so in performance-critical sections (like on the Android main thread) should be avoided.
 * New: Support for the release version of OkHttp 3.0 and newer.
 * New: `adapter-guava` module provides a `CallAdapter.Factory` for Guava's `ListenableFuture`.
 * New: `adapter-java8` module provides a `CallAdapter.Factory` for Java 8's `CompleteableFuture`.
 * New: `ScalarsConverterFactory` (from `converter-scalars` module) now supports parsing response bodies
   into either `String`, the 8 primitive types, or the 8 boxed primitive types.
 * New: Automatic support for sending callbacks to the iOS main thread when running via RoboVM.
 * New: Method annotations are now passed to the factory for request body converters. This allows converters
   to alter the structure of both request bodies and response bodies with a single method-level annotation.
 * Each converter has been moved to its own package under `retrofit2.converter.<name>`. This prevents type
   collisions when many converters are simultaneously in use.
 * Fix: Exceptions thrown when unable to locate a `CallAdapter.Factory` for a method return type now
   correctly list the `CallAdapter.Factory` instances checked.
 * Fix: Ensure default methods on service interfaces can be invoked.
 * Fix: Correctly resolve the generic parameter types of collection interfaces when subclasses of those
   collections are used as method parameters.
 * Fix: Do not encode `/` characters in `@Path` replacements when `encoded = true`.


Version 2.0.0-beta3 *(2016-01-05)*
----------------------------------

 * New: All classes have been migrated to the `retrofit2.*` package name. The Maven groupId is now
   `com.squareup.retrofit2`. This is in accordance with the
   [Java Interoperability Policy for Major Version Updates](http://jakewharton.com/java-interoperability-policy-for-major-version-updates/).
   With this change Retrofit 2.x can coexiest with Retrofit 1.x in the same project.
 * New: Update to use the OkHttp 3 API and OkHttp 3.0.0-RC1 or newer is now required. Similar to the previous
   point, OkHttp has a new package name (`okhttp3.*`) and Maven groupId (`com.squareup.okhttp3`) which allow
   it to coexist with OkHttp 2.x in the same project.
 * New: String converters allow for custom serialization of parameters that end up as strings (such as `@Path`,
   `@Query`, `@Header`, etc.). `Converter.Factory` has a new `stringConverter` method which receives the
   parameter type and annotations and can return a converter for that type. This allows providing custom
   rendering of types like `Date`, `User`, etc. to a string before being used for its purpose. A default
   converter will call `toString()` for any type which retains the mimics the previous behavior.
 * New: OkHttp's `Call.Factory` type is now used as the HTTP client rather than using the `OkHttpClient` type
   directly (`OkHttpClient` does implement `Call.Factory`). A `callFactory` method has been added to both
   `Retrofit.Builder` and `Retrofit` to allow supplying alternate implementations of an HTTP client. The
   `client(OkHttpClient)` method on `Retrofit.Builder` still exists as a convenience.
 * New: `isExecuted()` method returns whether a `Call` has been synchronously or asynchronously executed.
 * New: `isCanceled()` method returns whether a `Call` has been canceled. Use this in `onFailure` to determine
   whether the callback was invoked from cancellation or actual transport failure.
 * New: `converter-scalars` module provides a `Converter.Factory` for converting `String`, the 8 primitive
   types, and the 8 boxed primitive types as `text/plain` bodies. Install this before your normal converter
   to avoid passing these simple scalars through, for example, a JSON converter.
 * New: `Converter.Factory` methods now receive a `Retrofit` instance which also now has methods for querying
   the next converter for a given type. This allows implementations to delegate to others and provide
   additional behavior without complete reimplementation.
 * New: `@OPTIONS` annotation more easily allows for making OPTIONS requests.
 * New: `@Part` annotation now supports `List` and array types.
 * New: The `@Url` annotation now allows using `java.net.URI` or `android.net.Uri` (in addition to `String`)
   as parameter types for providing relative or absolute endpoint URLs dynamically.
 * New: The `retrofit-mock` module has been rewritten with a new `BehaviorDelegate` class for implementing
   fake network behavior in a local mock implementation of your service endpoints. Documentation and more
   tests are forthcoming, but the `SimpleMockService` demonstrates its use for now.
 * Fix: Forbid Retrofit's `Response` type and OkHttp's `Response` type as the response body type given to
   a `Call` (i.e., `Call<Response>`). OkHttp's `ResponseBody` type is the correct one to use when the raw
   body contents are desired.
 * Fix: The Gson converter now respects settings on the supplied `Gson` instance (such as `serializeNulls`).
   This requires Gson 2.4 or newer.
 * The Wire converter has been updated to the Wire 2.0 API.
 * The change in 2.0.0-beta2 which provided the `Retrofit` instance to the `onResponse` callback of `Callback`
   has been reverted. There are too many edge cases around providing the `Retrofit` object in order to allow
   deserialization of the error body. To accommodate this use case, pass around the `Retrofit` response
   manually or implement a custom `CallAdapter.Factory` does so automatically.


Version 2.0.0-beta2 *(2015-09-28)*
----------------------------------

 * New: Using a response type of `Void` (e.g., `Call<Void>`) will ignore and discard the response body. This
   can be used when there will be no response body (such as in a 201 response) or whenever the body is not
   needed. `@Head` requests are now forced to use this as their response type.
 * New: `validateEagerly()` method on `Retrofit.Builder` will verify the correctness of all service methods
   on calls to `create()` instead of lazily validating on first use.
 * New: `Converter` is now parameterized over both 'from' and 'to' types with a single `convert` method.
   `Converter.Factory` is now an abstract class and has factory methods for both request body and response
   body.
 * New: `Converter.Factory` and `CallAdapter.Factory` now receive the method annotations when being created
   for a return/response type and the parameter annotations when being created for a parameter type.
 * New: `callAdapter()` method on `Retrofit` allows querying a `CallAdapter` for a given type. The
   `nextCallAdapter()` method allows delegating to another `CallAdapter` from within a `CallAdapter.Factory`.
   This is useful for composing call adapters to incrementally build up behavior.
 * New: `requestConverter()` and `responseConverter()` methods on `Retrofit` allow querying a `Converter` for
   a given type.
 * New: `onResponse` method in `Callback` now receives the `Retrofit` instance. Combined with the
   `responseConverter()` method on `Retrofit`, this provides a way of deserializing an error body on `Response`.
   See the `DeserializeErrorBody` sample for an example.
 * New: The `MoshiConverterFactory` has been updated for its v1.0.0.
 * Fix: Using `ResponseBody` for the response type or `RequestBody` for a parameter type is now correctly
   identified. Previously these types would erroneously be passed to the supplied converter.
 * Fix: The encoding of `@Path` values has been corrected to conform to OkHttp's `HttpUrl`.
 * Fix: Use form-data content disposition subtype for `@Multipart`.
 * Fix: `Observable` and `Single`-based execution of requests now behave synchronously (and thus requires
   `subscribeOn()` for running in the background).
 * Fix: Correct `GsonConverterFactory` to honor the configuration of the `Gson` instances (such as not
   serializing null values, the default).


Version 2.0.0-beta1 *(2015-08-27)*
----------------------------------

 * New: `Call` encapsulates a single request/response HTTP call. A call can by run synchronously
   via `execute()` or asynchronously via `enqueue()` and can be canceled with `cancel()`.
 * New: `Response` is now parameterized and includes the deserialized body object.
 * New: `@Url` parameter annotation allows passing a complete URL for an endpoint.
 * New: OkHttp is now required as a dependency. Types like `TypedInput` and `TypedOutput` (and its
   implementations), `Request`, and `Header` have been replaced with OkHttp types like `RequestBody`,
   `ResponseBody`, and `Headers`.
 * New: `CallAdapter` (and `Factory`) provides extension point for supporting multiple execution
   mechanisms. An RxJava implementation is provided by a sibling module.
 * New: `Converter` (and `Factory`) provides extension point for supporting multiple serialization
   mechanisms. Gson, Jackson, Moshi, Protobuf, Wire, and SimpleXml implementations are provided by sibling
   modules.
 * Fix: A lot of things.
 * Hello Droidcon NYC 2015!


Version 1.9.0 *(2015-01-07)*
----------------------------

 * Update to OkHttp 2.x's native API. If you are using OkHttp you must use version 2.0 or newer (the latest
   is 2.2 at time of writing) and you no longer need to use the `okhttp-urlconnection` shim.
 * New: Allow disabling Simple XML Framework's strict parsing.
 * New: `@Header` now accepts a `List` or array for a type.
 * New: `@Field` and `@FieldMap` now have options for enabling or disabling URL encoding of names and values.
 * Fix: Remove query parameters from thread name when running background requests for asynchronous use.


Version 1.8.0 *(2014-11-18)*
----------------------------

 * Update to RxJava 1.0. This comes with the project's 'groupId' change from `com.netflix.rxjava` to
   `io.reactivex` which is why the minor version was bumped.


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
   [OkHttp](https://square.github.io/okhttp) jar to your project if you need this behavior.
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

 * New: Converter for [Wire protocol buffers](https://github.com/square/wire)!


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


 [maven_provided]: https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html
