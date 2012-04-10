package retrofit.http;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.content.AbstractContentBody;
import retrofit.io.TypedBytes;

/** Adapts ContentBody to TypedBytes. */
public class TypedBytesBody extends AbstractContentBody {
  private final TypedBytes typedBytes;
  private final String name;

  public TypedBytesBody(TypedBytes typedBytes, String baseName) {
    super(typedBytes.mimeType().mimeName());
    this.typedBytes = typedBytes;
    this.name = baseName + "." + typedBytes.mimeType().extension();
  }

  @Override public long getContentLength() {
    return typedBytes.length();
  }

  @Override public String getFilename() {
    return name;
  }

  @Override public String getCharset() {
    return null;
  }

  @Override public String getTransferEncoding() {
    return MIME.ENC_BINARY;
  }

  @Override public void writeTo(OutputStream out) throws IOException {
    /*
     * Note: We probably want to differentiate I/O errors that occur
     * while reading a file from network errors. Network operations can
     * be retried. File operations will probably continue to fail.
     *
     * In the case of photo uploads, we at least check that the file
     * exists before we even try to upload it.
     */
    typedBytes.writeTo(out);
  }
}