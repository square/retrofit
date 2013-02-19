// Copyright 2013 Square, Inc.
package retrofit.io;

import java.io.UnsupportedEncodingException;

public class TypedString extends TypedByteArray {
  public TypedString(String string) {
    super("text/plain; charset=UTF-8", convertToBytes(string));
  }

  private static byte[] convertToBytes(String string) {
    try {
      return string.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
