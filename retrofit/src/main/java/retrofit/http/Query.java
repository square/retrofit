// Copyright 2013 Square, Inc.
package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Query parameter appended to the URL. Values are converted to strings using
 * {@link String#valueOf(Object)}.
 * <p>
 * <pre>
 * @GET("/list")
 * void example(@Query("page") int page, ..);
 * </pre>
 */
@Target(PARAMETER) @Retention(RUNTIME)
public @interface Query {
  String value();
}
