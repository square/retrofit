// Copyright 2013 Square, Inc.
package retrofit.converter;

import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/** A {@link Converter} that reads and writes protocol buffers. */
public class ProtoConverter implements LoggingConverter {
  private static final String MIME_TYPE = "application/x-protobuf";

  @Override public Object fromBody(TypedInput body, Type type) throws ConversionException {
    return convertToMessage(body, type);
  }

  private AbstractMessageLite convertToMessage(TypedInput body, Type type)
      throws ConversionException {
    if (!(type instanceof Class<?>)) {
      throw new IllegalArgumentException("Expected a raw Class<?> but was " + type);
    }
    Class<?> c = (Class<?>) type;
    if (!AbstractMessageLite.class.isAssignableFrom(c)) {
      throw new IllegalArgumentException("Expected a protobuf message but was " + c.getName());
    }

    String mimeType = body.mimeType();
    if (!MIME_TYPE.equals(mimeType)) {
      throw new ConversionException("Response content type was not a proto: " + mimeType);
    }

    try {
      Method parseFrom = c.getMethod("parseFrom", InputStream.class);
      return (AbstractMessageLite) parseFrom.invoke(null, body.in());
    } catch (InvocationTargetException e) {
      throw new ConversionException(c.getName() + ".parseFrom() failed", e.getCause());
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("Expected a protobuf message but was " + c.getName());
    } catch (IllegalAccessException e) {
      throw new AssertionError();
    } catch (IOException e) {
      throw new ConversionException(e);
    }
  }

  @Override public TypedOutput toBody(Object object) {
    AbstractMessageLite message = getAsMessage(object);
    return new TypedByteArray(MIME_TYPE, message.toByteArray());
  }

  @Override public String bodyToLogString(TypedInput body, Type type) throws ConversionException {
    return bodyToLogString(convertToMessage(body, type));
  }

  @Override public String bodyToLogString(Object object) {
    AbstractMessageLite message = getAsMessage(object);
    if (message instanceof Message) {
      return TextFormat.printToString((Message) message);
    }
    return "(lite protos cannot be represented)";
  }

  private static AbstractMessageLite getAsMessage(Object object) {
    if (!(object instanceof AbstractMessageLite)) {
      throw new IllegalArgumentException(
          "Expected a protobuf message but was " + (object != null ? object.getClass().getName()
              : "null"));
    }
    return (AbstractMessageLite) object;
  }
}
