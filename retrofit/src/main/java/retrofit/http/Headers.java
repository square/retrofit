// Copyright 2013 Square, Inc.
package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Adds headers literally supplied in the {@code value}.
 * <p>
 * <pre>
 * &#64;Headers("Cache-Control: max-age=640000")
 * &#64;GET("/")
 * ...
 *
 * &#64;Headers({
 *   "X-Foo: Bar",
 *   "X-Ping: Pong"
 * })
 * &#64;GET("/")
 * ...
 * </pre>
 * <p>
 * <strong>Note:</strong> Headers do not overwrite each other. All headers with the same name will
 * be included in the request.
 *
 * @author Adrian Cole (adrianc@netflix.com)
 */
@Target(METHOD) @Retention(RUNTIME)
public @interface Headers {
  String[] value();
}
