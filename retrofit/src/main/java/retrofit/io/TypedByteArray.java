// Copyright 2010 Square, Inc.
package retrofit.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Byte array and its mime type.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public class TypedByteArray extends AbstractTypedBytes {
  private static final long serialVersionUID = 0;

  private final byte[] bytes;

  /**
   * Constructs a new typed byte array.
   *
   * @throws NullPointerException if bytes or mimeType is null
   */
  public TypedByteArray(byte[] bytes, MimeType mimeType) {
    super(mimeType);
    if (bytes == null) throw new NullPointerException("bytes");
    this.bytes = bytes;
  }

  public void writeTo(OutputStream out) throws IOException {
    out.write(bytes);
  }

  @Override public int length() {
    return bytes.length;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o instanceof TypedByteArray) {
      TypedByteArray rhs = (TypedByteArray) o;
      return Arrays.equals(bytes, rhs.bytes);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(bytes);
  }
}