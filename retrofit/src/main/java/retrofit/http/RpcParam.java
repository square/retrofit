package retrofit.http;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by dp on 21/04/14.
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface RpcParam {
  String value();
}
