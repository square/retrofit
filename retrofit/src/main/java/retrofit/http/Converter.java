// Copyright 2012 Square, Inc.
package retrofit.http;

import java.lang.reflect.Type;
import retrofit.io.TypedBytes;

/**
 * Arbiter for converting objects to and from their representation in HTTP.
 *
 * @author Jake Wharton (jw@squareup.com)
 */
public interface Converter {
  /**
   * Convert an HTTP response body to a concrete object of the specified type.
   *
   * @param body HTTP response body.
   * @param type Target object type.
   * @return Instance of {@code type} which will be cast by the caller.
   * @throws ConversionException If conversion was unable to complete. This will trigger a call to
   * {@link Callback#failure(RetrofitError)} or throw a {@link retrofit.http.RetrofitError}.
   */
  Object fromBody(byte[] body, Type type) throws ConversionException;

  /**
   * Convert and object to an appropriate representation for HTTP transport.
   *
   * @param object Object instance to convert.
   * @return Representation of the specified object as bytes.
   */
  TypedBytes toBody(Object object);
}
