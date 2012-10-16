// Copyright 2012 Square, Inc.
package retrofit.http;

import org.apache.http.HttpEntity;
import retrofit.io.TypedBytes;

import java.lang.reflect.Type;

/**
 * Arbiter for converting objects to and from their representation in HTTP.
 *
 * @author Jake Wharton (jw@squareup.com)
 */
public interface Converter {
  /**
   * Convert an HTTP response body to a concrete object of the specified type.
   *
   * @param entity HTTP response body.
   * @param type Target object type.
   * @return Instance of {@code type} which will be cast by the caller.
   * @throws ConversionException If conversion was unable to complete. This will trigger a call to
   * {@link Callback#serverError(retrofit.http.Callback.ServerError, int)}.
   */
  Object to(HttpEntity entity, Type type) throws ConversionException;

  /**
   * Convert and object to appropriate representation for HTTP transport.
   *
   * @param object Object instance to convert.
   * @return Representation of the specified object as bytes.
   */
  TypedBytes from(Object object);
}
