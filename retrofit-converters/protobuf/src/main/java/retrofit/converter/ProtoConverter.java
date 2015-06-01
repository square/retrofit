// Copyright 2013 Square, Inc.
package retrofit.converter;

import com.google.protobuf.AbstractMessageLite;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/** A {@link Converter} that reads and writes protocol buffers. */
public class ProtoConverter implements Converter {

  private final MediaType mediaType;

  /**
   * Default constructor for converter with default Content-Type - application/x-protobuf
   */
  public ProtoConverter() {
    mediaType = MediaType.parse("application/x-protobuf");
  }

  /**
   * Constructor allowing usage of custom Content-Type - e.g. application/octet-stream,
   * application/x-google-protobuf
   *
   * @param contentType Custom Content-Type
   */
  public ProtoConverter(String contentType) {
    mediaType = MediaType.parse(contentType);
  }


  @Override public Object fromBody(ResponseBody body, Type type) throws IOException {
    if (!(type instanceof Class<?>)) {
      throw new IllegalArgumentException("Expected a raw Class<?> but was " + type);
    }
    Class<?> c = (Class<?>) type;
    if (!AbstractMessageLite.class.isAssignableFrom(c)) {
      throw new IllegalArgumentException("Expected a protobuf message but was " + c.getName());
    }

    InputStream is = body.byteStream();
    try {
      Method parseFrom = c.getMethod("parseFrom", InputStream.class);
      return parseFrom.invoke(null, is);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(c.getName() + ".parseFrom() failed", e.getCause());
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("Expected a protobuf message but was " + c.getName());
    } catch (IllegalAccessException e) {
      throw new AssertionError();
    } finally {
      try {
        is.close();
      } catch (IOException ignored) {
      }
    }
  }

  @Override public RequestBody toBody(Object object, Type type) {
    if (!(object instanceof AbstractMessageLite)) {
      throw new IllegalArgumentException(
          "Expected a protobuf message but was " + (object != null ? object.getClass().getName()
              : "null"));
    }
    byte[] bytes = ((AbstractMessageLite) object).toByteArray();
    return RequestBody.create(mediaType, bytes);
  }
}
