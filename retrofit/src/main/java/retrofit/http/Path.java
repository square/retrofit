// Copyright 2013 Square, Inc.
package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Named replacement in the URL path. Values are converted to string using
 * {@link String#valueOf(Object)}.
 * <p>
 * <pre>
 * @GET("/image/{id}")
 * void example(@Path("id") int id, ..);
 * </pre>
 */
@Retention(RUNTIME) @Target(PARAMETER)
public @interface Path {
  String value();
}
