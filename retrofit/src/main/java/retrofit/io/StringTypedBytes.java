// Copyright 2013 Square, Inc.
package retrofit.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class StringTypedBytes extends AbstractTypedBytes {
  private final byte[] bytes;

  public StringTypedBytes(String string) {
    super("text/plain; charset=UTF-8");
    try {
      bytes = string.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  @Override public int length() {
    return bytes.length;
  }

  @Override public void writeTo(OutputStream out) throws IOException {
    out.write(bytes);
  }
}
