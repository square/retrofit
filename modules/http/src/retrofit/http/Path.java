package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * REST URL path relative to base URL.
 *
 * @author Bob Lee (bob@squareup.com)
 */
@Target({ METHOD })
@Retention(RUNTIME)
public @interface Path {
  String value();
}
