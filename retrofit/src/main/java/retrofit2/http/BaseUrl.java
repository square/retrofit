package retrofit2.http;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import retrofit2.Retrofit;

/**
 * Use this annotation to override base url per service.
 * By doing this you can use service per remote with one {@link Retrofit Retrofit} instance
 *
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface BaseUrl {
  String value();
}
