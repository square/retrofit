package retrofit2;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;

public class JSONBody extends RequestBody {
  
  private static final MediaType CONTENT_TYPE =
      MediaType.parse("application/json");
  private String jsonString;
  
  private JSONBody(String jsonString) {
    this.jsonString = jsonString;
  }

  @Override
  public MediaType contentType() {
    return CONTENT_TYPE;
  }

  @Override public long contentLength() {
    return writeOrCountBytes(null, true);
  }

  @Override public void writeTo(BufferedSink sink) throws IOException {
    writeOrCountBytes(sink, false);
  }

  /**
   * Either writes this request to {@code sink} or measures its content length. We have one method
   * do double-duty to make sure the counting and content are consistent, particularly when it comes
   * to awkward operations like measuring the encoded length of header strings, or the
   * length-in-digits of an encoded integer.
   */
  private long writeOrCountBytes(BufferedSink sink, boolean countBytes) {
    long byteCount = 0L;

    Buffer buffer;
    if (countBytes) {
      buffer = new Buffer();
    } else {
      buffer = sink.buffer();
    }

    buffer.writeUtf8(jsonString);

    if (countBytes) {
      byteCount = buffer.size();
      buffer.clear();
    }

    return byteCount;
  }
  
  public static final class Builder {
    private String jsonString;

    /**
     * Sets the JSON string to be used for the JSONBody.
     * @param jsonString
     */
    public void setJSONString(String jsonString) {
      if (jsonString == null) {
        throw new IllegalArgumentException("jsonString must not be null.");
      }
      this.jsonString = jsonString;
    }

    public JSONBody build() {
      if (jsonString == null) {
        throw new IllegalStateException("No JSON string has been set");
      }
      return new JSONBody(jsonString);
    }
  }
}
