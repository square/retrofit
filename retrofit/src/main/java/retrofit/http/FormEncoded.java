// Copyright 2013 Square, Inc.
package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Denotes that the request body is form-encoded. Parts should be declared as parameters and
 * annotated with {@link Pair @Pair}.
 */
@Target(METHOD) @Retention(RUNTIME)
public @interface FormEncoded {
}
