// Copyright 2013 Square, Inc.
package retrofit.http.mime;

import java.util.List;

public class MimeHelper {
  public static List<byte[]> getParts(MultipartTypedOutput output) {
    return output.parts;
  }
}
