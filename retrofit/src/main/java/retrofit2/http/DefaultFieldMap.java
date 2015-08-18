package retrofit2.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Default named key/value pairs for a form-encoded request.
 * <p>
 * Simple example:
 * <pre>
 * &#64;DefaultFieldMap("occupation=President")
 * &#64;FormUrlEncoded
 * &#64;POST("/")
 * void example(@Field("name") String name);
 * ...
 * </pre>
 * Calling with {@code foo.example("Tom")} yields a request body of
 * {@code name=Tom&occupation=President}
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface DefaultFieldMap {
  String[] value();
}
