package retrofit2;


import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Use this meta-annotation on an annotation you want to use on a service method parameter to cause
 * retrofit to ignore the parameter when processing a request.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface IgnoreParameter {
}
