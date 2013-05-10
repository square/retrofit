// Copyright 2013 Square, Inc.
package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Denotes a single part of a mutli-part request.
 * <p>
 * The parameter type on which this annotation exists will be processed in one of two ways:
 * <ul>
 * <li>If the type implements {@link retrofit.http.mime.TypedOutput TypedOutput} the headers and
 * body will be used directly.</li>
 * <li>Other object types will be converted to an appropriate representation by calling {@link
 * Converter#toBody(Object)}.</li>
 * </ul>
 * <p>
 * <pre>
 * @Multipart
 * @POST("/")
 * void example(@Part("description") TypedString description,
 *              @Part("image") TypedFile image,
 *              ..
 * );
 * </pre>
 */
@Target(PARAMETER) @Retention(RUNTIME)
public @interface Part {
  String value();
}
