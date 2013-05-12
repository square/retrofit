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
 * &#64;FormUrlEncoded
 * &#64;POST("/")
 * void example(@Field("name") String name, @Field("occupation") String occupation, ..);
 * }
 * </pre>
 */
@Target(PARAMETER) @Retention(RUNTIME)
public @interface Field {
  String value();
}
