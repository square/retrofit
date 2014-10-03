// Copyright 2013 Square, Inc.
package retrofit.mime;

import java.util.List;

public class MimeHelper {
  public static List<byte[]> getParts(MultipartTypedOutput output) {
    try {
      return output.getParts();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static MultipartTypedOutput newMultipart(String boundary) {
    return new MultipartTypedOutput(boundary);
  }
}
