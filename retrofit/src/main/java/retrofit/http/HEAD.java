// Copyright 2013 Square, Inc.
package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Make a HEAD request to a REST path relative to base URL. */
@Target(METHOD)
@Retention(RUNTIME)
@RestMethod("HEAD")
public @interface HEAD {
  String value() default "";
}
