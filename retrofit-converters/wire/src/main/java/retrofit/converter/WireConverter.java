// Copyright 2013 Square, Inc.
package retrofit.converter;

import com.squareup.wire.Message;
import com.squareup.wire.Wire;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/** A {@link Converter} that reads and writes protocol buffers using Wire. */
public class WireConverter implements Converter {
  private static final String MIME_TYPE = "application/x-protobuf";

  private final Wire wire;

  public WireConverter(Wire wire) {
    this.wire = wire;
  }

  @SuppressWarnings("unchecked") //
  @Override public Object fromBody(TypedInput body, Type type) throws ConversionException {
    if (!(type instanceof Class<?>)) {
      throw new IllegalArgumentException("Expected a raw Class<?> but was " + type);
    }
    Class<?> c = (Class<?>) type;
    if (!Message.class.isAssignableFrom(c)) {
      throw new IllegalArgumentException("Expected a proto message but was " + c.getName());
    }

    if (!MIME_TYPE.equalsIgnoreCase(body.mimeType())) {
      throw new IllegalArgumentException("Expected a proto but was: " + body.mimeType());
    }

    try {
      byte[] data = consumeAsBytes(body.in());
      return wire.parseFrom(data, (Class<Message>) c);
    } catch (IOException e) {
      throw new ConversionException(e);
    }
  }

  @Override public TypedOutput toBody(Object object) {
    if (!(object instanceof Message)) {
      throw new IllegalArgumentException(
          "Expected a proto message but was " + (object != null ? object.getClass().getName()
              : "null"));
    }
    byte[] bytes = ((Message) object).toByteArray();
    return new TypedByteArray(MIME_TYPE, bytes);
  }

  /** Reads a stream into a {@code byte} array. */
  private byte[] consumeAsBytes(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    pipe(in, out);
    return out.toByteArray();
  }

  /** Reads content from the given input and pipes it to the given output. */
  private void pipe(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[4096];
    int count;
    while ((count = in.read(buffer)) != -1) {
      out.write(buffer, 0, count);
    }
  }
}
