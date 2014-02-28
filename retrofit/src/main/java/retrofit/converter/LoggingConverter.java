// Copyright 2014 Square, Inc.
package retrofit.converter;

import java.lang.reflect.Type;
import retrofit.mime.TypedInput;

/** A {@link Converter} which supports conversion to a log-friendly format. */
public interface LoggingConverter extends Converter {
  /**
   * Convert an HTTP response body to as string for logging purposes.
   *
   * @param body HTTP response body.
   * @param type Target object type. This can be used to assist in converting the body to a state
   * which can be converted to a string (e.g., with binary protocols like protocol buffers).
   * @return String representation of the HTTP response body.
   */
  String bodyToLogString(TypedInput body, Type type) throws ConversionException;

  /**
   * Convert an HTTP request body object to a string for logging purposes.
   * <p>
   * This method is similar to {@link #toBody(Object)} except that the destination serialization
   * should be appropriate for logging.
   *
   * @param object Object instance to convert.
   * @return String representation of the HTTP request body.
   */
  String bodyToLogString(Object object);
}
