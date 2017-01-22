package retrofit2.http;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes that the request body is in JSON format. 
 * Requests made with this annotation will have {@code application/json} MIME type.
 * 
 * A root key may be specific with {@link Root @Root}.
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface JSON {
	
}
