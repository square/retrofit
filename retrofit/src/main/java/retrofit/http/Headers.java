// Copyright 2013 Square, Inc.
package retrofit.http;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Adds headers literally supplied in the {@code value}.
 *
 * <p/>
 * ex.
 *
 * <pre>
 * @Headers("Cache-Control: max-age=640000")
 * @GET("/")
 * ...
 *
 * @Headers({
 *   "X-Foo: Bar",
 *   "X-Ping: Pong"
 * })
 * @GET("/")
 * ...
 * </pre>
 *
 * @author Adrian Cole (adrianc@netflix.com)
 */
@Target(METHOD) @Retention(RUNTIME)
public @interface Headers {
  String[] value();
}
