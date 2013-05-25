Change Log
==========

Version 1.1.0 *(In Development)*
--------------------------------

 * Introduce `RequestInterceptor` to replace `RequestHeaders`. An interceptor provided to the
   `RestAdapter.Builder` will be called for every request and allow setting both headers and
   additional path parameter replacements.


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
