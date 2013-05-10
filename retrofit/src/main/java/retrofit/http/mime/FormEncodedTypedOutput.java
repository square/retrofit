// Copyright 2013 Square, Inc.
package retrofit.http.mime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

public final class FormEncodedTypedOutput implements TypedOutput {
  final ByteArrayOutputStream content = new ByteArrayOutputStream();

  public void addPair(String name, String value) {
    if (name == null) {
      throw new NullPointerException("name");
    }
    if (value == null) {
      throw new NullPointerException("value");
    }
    if (content.size() > 0) {
      content.write('&');
    }
    try {
      name = URLEncoder.encode(name, "UTF-8");
      value = URLEncoder.encode(value, "UTF-8");

      content.write(name.getBytes("UTF-8"));
      content.write('=');
      content.write(value.getBytes("UTF-8"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override public String fileName() {
    return null;
  }

  @Override public String mimeType() {
    return "application/x-www-form-urlencoded; charset=UTF-8";
  }

  @Override public long length() {
    return content.size();
  }

  @Override public void writeTo(OutputStream out) throws IOException {
    out.write(content.toByteArray());
  }
}
