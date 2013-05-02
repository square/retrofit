package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Make a GET request to a REST path relative to base URL. */
@Target(METHOD)
@Retention(RUNTIME)
@RestMethod("GET")
public @interface GET {
  String value() default "";
}
