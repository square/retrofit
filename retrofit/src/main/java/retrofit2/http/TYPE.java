package retrofit2.http;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Request type(http or https)
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface TYPE {

  int TYPE_HTTP = 0;
  int TYPE_HTTPS = 1;

  int value() default TYPE_HTTP;
}
