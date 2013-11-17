// Copyright 2013 Square, Inc.
package retrofit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;

class MockTypedInput implements TypedInput {
  private final Converter converter;
  private final Object body;

  private byte[] bytes;

  MockTypedInput(Converter converter, Object body) {
    this.converter = converter;
    this.body = body;
  }

  @Override public String mimeType() {
    return "application/unknown";
  }

  @Override public long length() {
    try {
      initBytes();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return bytes.length;
  }

  @Override public InputStream in() throws IOException {
    initBytes();
    return new ByteArrayInputStream(bytes);
  }

  private synchronized void initBytes() throws IOException {
    if (bytes == null) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      converter.toBody(body).writeTo(out);
      bytes = out.toByteArray();
    }
  }
}
