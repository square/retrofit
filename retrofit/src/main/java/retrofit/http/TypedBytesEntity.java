// Copyright 2011 Square, Inc.
package retrofit.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.entity.AbstractHttpEntity;
import retrofit.io.TypedBytes;

/**
 * Container class for when you want to pass an entire {@link TypedBytes} as a http request entity.
 *
 * @author Eric Denman (edenman@squareup.com)
 */
class TypedBytesEntity extends AbstractHttpEntity {

  private final TypedBytes typedBytes;

  public TypedBytesEntity(TypedBytes typedBytes) {
    this.typedBytes = typedBytes;
    setContentType(typedBytes.mimeType().mimeName());
  }

  @Override public boolean isRepeatable() {
    return true;
  }

  @Override public long getContentLength() {
    return typedBytes.length();
  }

  @Override public InputStream getContent() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    typedBytes.writeTo(out);
    return new ByteArrayInputStream(out.toByteArray());
  }

  @Override public void writeTo(OutputStream out) throws IOException {
    typedBytes.writeTo(out);
  }

  @Override public boolean isStreaming() {
    return false;
  }
}
