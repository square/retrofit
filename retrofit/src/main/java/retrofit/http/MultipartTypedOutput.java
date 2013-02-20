// Copyright 2013 Square, Inc.
package retrofit.http;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.AbstractContentBody;
import retrofit.http.mime.TypedOutput;

final class MultipartTypedOutput implements TypedOutput {
  // TODO implement our own Multipart logic instead!
  final MultipartEntity cheat = new MultipartEntity();

  void addPart(String name, TypedOutput body) {
    cheat.addPart(name, new TypedOutputBody(body));
  }

  @Override public String mimeType() {
    return cheat.getContentType().getValue();
  }

  @Override public long length() {
    return cheat.getContentLength();
  }

  @Override public void writeTo(OutputStream out) throws IOException {
    cheat.writeTo(out);
  }

  /** Adapts {@link org.apache.http.entity.mime.content.ContentBody} to {@link TypedOutput}. */
  static class TypedOutputBody extends AbstractContentBody {
    final TypedOutput typedBytes;

    TypedOutputBody(TypedOutput typedBytes) {
      super(typedBytes.mimeType());
      this.typedBytes = typedBytes;
    }

    @Override public long getContentLength() {
      return typedBytes.length();
    }

    @Override public String getFilename() {
      return null;
    }

    @Override public String getCharset() {
      return null;
    }

    @Override public String getTransferEncoding() {
      return MIME.ENC_BINARY;
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      // Note: We probably want to differentiate I/O errors that occur while reading a file from
      // network errors. Network operations can be retried. File operations will probably continue
      // to fail.
      //
      // In the case of photo uploads, we at least check that the file exists before we even try to
      // upload it.
      typedBytes.writeTo(out);
    }
  }
}
