// Copyright 2013 Square, Inc.
package retrofit;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import okio.Buffer;
import okio.BufferedSource;
import retrofit.converter.Converter;

class MockResponseBody extends ResponseBody {
  private final Converter converter;
  private final Object body;

  private byte[] bytes;

  MockResponseBody(Converter converter, Object body) {
    this.converter = converter;
    this.body = body;
  }

  @Override public MediaType contentType() {
    return MediaType.parse("application/unknown");
  }

  @Override public long contentLength() {
    try {
      initBytes();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return bytes.length;
  }

  @Override public BufferedSource source() {
    return new Buffer().write(bytes);
  }

  private synchronized void initBytes() throws IOException {
    if (bytes == null) {
      Buffer buffer = new Buffer();
      converter.toBody(body, body.getClass()).writeTo(buffer);
      bytes = buffer.readByteArray();
    }
  }
}
