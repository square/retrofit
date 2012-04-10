package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a query parameter.
 *
 * @author Patrick Forhan (patrick@squareup.com)
 */
@Target({ METHOD })
@Retention(RUNTIME)
public @interface QueryParam {
  String name();
  String value();
}
