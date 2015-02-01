// Copyright 2013 Square, Inc.
package retrofit.converter;

import com.google.protobuf.AbstractMessageLite;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/** A {@link Converter} that reads and writes protocol buffers. */
public class ProtoConverter implements Converter {
  private static final String MIME_TYPE = "application/x-protobuf";

  @Override public Object fromBody(TypedInput body, Type type) throws IOException {
    if (!(type instanceof Class<?>)) {
      throw new IllegalArgumentException("Expected a raw Class<?> but was " + type);
    }
    Class<?> c = (Class<?>) type;
    if (!AbstractMessageLite.class.isAssignableFrom(c)) {
      throw new IllegalArgumentException("Expected a protobuf message but was " + c.getName());
    }

    String mimeType = body.mimeType();
    if (!MIME_TYPE.equals(mimeType)) {
      throw new RuntimeException("Response content type was not a proto: " + mimeType);
    }

    try {
      Method parseFrom = c.getMethod("parseFrom", InputStream.class);
      return parseFrom.invoke(null, body.in());
    } catch (InvocationTargetException e) {
      throw new RuntimeException(c.getName() + ".parseFrom() failed", e.getCause());
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("Expected a protobuf message but was " + c.getName());
    } catch (IllegalAccessException e) {
      throw new AssertionError();
    }
  }

  @Override public TypedOutput toBody(Object object, Type type) {
    if (!(object instanceof AbstractMessageLite)) {
      throw new IllegalArgumentException(
          "Expected a protobuf message but was " + (object != null ? object.getClass().getName()
              : "null"));
    }
    byte[] bytes = ((AbstractMessageLite) object).toByteArray();
    return new TypedByteArray(MIME_TYPE, bytes);
  }
}
