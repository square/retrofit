// Copyright 2013 Square, Inc.
package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Named pair for a form-encoded request.
 *
 * <pre>
 * @FormEncoded
 * @POST("/")
 * void example(@Pair("name") String name, @Pair("occupation") String occupation, ..);
 * }
 * </pre>
 */
@Target(PARAMETER) @Retention(RUNTIME)
public @interface Pair {
  String value();
}
