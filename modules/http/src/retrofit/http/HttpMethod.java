package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Type of HTTP request to make.
 *
 * @author Rob Dickerson (rdickerson@squareup.com)
 */
@Target({ METHOD })
@Retention(RUNTIME)
public @interface HttpMethod {
  enum Type {GET, POST}
  Type value();
}
