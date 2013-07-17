package retrofit.converter;

import java.io.IOException;
import java.io.OutputStream;

import retrofit.mime.TypedOutput;

class JsonTypedOutput implements TypedOutput {
  private final byte[] jsonBytes;

  JsonTypedOutput(byte[] jsonBytes) {
    this.jsonBytes = jsonBytes;
  }

  @Override public String fileName() {
    return null;
  }

  @Override public String mimeType() {
    return "application/json; charset=UTF-8";
  }

  @Override public long length() {
    return jsonBytes.length;
  }

  @Override public void writeTo(OutputStream out) throws IOException {
    out.write(jsonBytes);
  }
}
