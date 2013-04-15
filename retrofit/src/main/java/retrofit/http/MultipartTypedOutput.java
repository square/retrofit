// Copyright 2013 Square, Inc.
package retrofit.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import retrofit.http.mime.TypedFile;
import retrofit.http.mime.TypedOutput;

final class MultipartTypedOutput implements TypedOutput {
  final Map<String, TypedOutput> parts = new LinkedHashMap<String, TypedOutput>();
  private final String boundary;

  MultipartTypedOutput() {
    boundary = UUID.randomUUID().toString();
  }

  void addPart(String name, TypedOutput body) {
    if (name == null) {
      throw new NullPointerException("Part name must not be null.");
    }
    if (body == null) {
      throw new NullPointerException("Part body must not be null.");
    }
    parts.put(name, body);
  }

  @Override public String mimeType() {
    return "multipart/form-data; boundary=" + boundary;
  }

  @Override public long length() {
    return -1;
  }

  @Override public void writeTo(OutputStream out) throws IOException {
    boolean first = true;
    for (Map.Entry<String, TypedOutput> part : parts.entrySet()) {
      writeBoundary(out, boundary, first, false);
      writePart(out, part);
      first = false;
    }
    writeBoundary(out, boundary, false, true);
  }

  private static void writeBoundary(OutputStream out, String boundary, boolean first, boolean last)
      throws IOException {
    StringBuilder sb = new StringBuilder();
    if (!first) {
      sb.append("\r\n");
    }
    sb.append("--");
    sb.append(boundary);
    if (last) {
      sb.append("--");
    } else {
      sb.append("\r\n");
    }
    out.write(sb.toString().getBytes("UTF-8"));
  }

  private static void writePart(OutputStream out, Map.Entry<String, TypedOutput> part)
      throws IOException {
    String name = part.getKey();
    TypedOutput value = part.getValue();

    StringBuilder headers = new StringBuilder();
    headers.append("Content-Disposition: form-data; name=\"");
    headers.append(name);
    if (value instanceof TypedFile) {
      headers.append("\"; filename=\"");
      headers.append(((TypedFile) value).file().getName());
    }
    headers.append("\"\r\nContent-Type: ");
    headers.append(value.mimeType());
    headers.append("\r\nContent-Transfer-Encoding: binary\r\n\r\n");
    out.write(headers.toString().getBytes("UTF-8"));

    value.writeTo(out);
  }
}
