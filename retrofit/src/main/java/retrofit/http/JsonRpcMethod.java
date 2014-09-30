package retrofit.http;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by dp on 21/04/14.
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
@RestMethod(value = "POST", hasBody = true)
public @interface JsonRpcMethod {
  String value();
}
