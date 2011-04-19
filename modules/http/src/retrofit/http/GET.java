package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import retrofit.http.HttpMethod.Type;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Make a GET request to a REST path relative to base URL.
 *
 * @author Rob Dickerson
 */
@Target({ METHOD })
@Retention(RUNTIME)
@HttpMethod(value = Type.GET)
public @interface GET {
  String value();
}
