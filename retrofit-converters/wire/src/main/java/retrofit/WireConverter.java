// Copyright 2013 Square, Inc.
package retrofit;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import com.squareup.wire.Message;
import com.squareup.wire.Wire;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/** A {@link Converter} that reads and writes protocol buffers using Wire. */
public class WireConverter implements Converter {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/x-protobuf");

  private final Wire wire;

  /** Create a converter with a default {@link Wire} instance. */
  public WireConverter() {
    this(new Wire());
  }

  /** Create a converter using the supplied {@link Wire} instance. */
  public WireConverter(Wire wire) {
    if (wire == null) throw new NullPointerException("wire == null");
    this.wire = wire;
  }

  @SuppressWarnings("unchecked") //
  @Override public Object fromBody(ResponseBody body, Type type) throws IOException {
    if (!(type instanceof Class<?>)) {
      throw new IllegalArgumentException("Expected a raw Class<?> but was " + type);
    }
    Class<?> c = (Class<?>) type;
    if (!Message.class.isAssignableFrom(c)) {
      throw new IllegalArgumentException("Expected a proto message but was " + c.getName());
    }

    InputStream in = body.byteStream();
    try {
      return wire.parseFrom(in, (Class<Message>) c);
    } finally {
      try {
        in.close();
      } catch (IOException ignored) {
      }
    }
  }

  @Override public RequestBody toBody(Object object, Type type) {
    if (!(object instanceof Message)) {
      throw new IllegalArgumentException(
          "Expected a proto message but was " + (object != null ? object.getClass().getName()
              : "null"));
    }
    byte[] bytes = ((Message) object).toByteArray();
    return RequestBody.create(MEDIA_TYPE, bytes);
  }
}
