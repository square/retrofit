package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Type of HTTP request to make.
 *
 * @author Rob Dickerson (rdickerson@squareup.com)
 * @author Patrick Forhan (patrick@squareup.com)
 */
@Target({ METHOD, ANNOTATION_TYPE })
@Retention(RUNTIME)
@interface HttpMethod {
  HttpMethodType value();
}
