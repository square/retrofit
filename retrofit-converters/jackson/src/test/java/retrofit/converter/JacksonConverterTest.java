package retrofit.converter;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonConverterTest {
  private static final String MIME_TYPE = "application/json; charset=UTF-8";

  private final MyObject obj = new MyObject("hello world", 10);
  private final String objAsJson = String.format("{\"message\":\"%s\",\"count\":%d}", obj.getMessage(), obj.getCount());
  private final JacksonConverter converter = new JacksonConverter(new ObjectMapper());

  @Test public void serialize() throws Exception {
    final TypedOutput typedOutput = converter.toBody(obj);
    assertThat(typedOutput.mimeType()).isEqualTo(MIME_TYPE);
    assertThat(asString(typedOutput)).isEqualTo(objAsJson);
  }

  @Test public void deserialize() throws Exception {
    final TypedInput input = new TypedByteArray(MIME_TYPE, objAsJson.getBytes());
    final MyObject result = (MyObject) converter.fromBody(input, MyObject.class);
    assertThat(result).isEqualTo(obj);
  }

  @Test(expected = ConversionException.class) public void deserializeWrongValue() throws Exception {
    final TypedInput input = new TypedByteArray(MIME_TYPE, "{\"foo\":\"bar\"}".getBytes());
    converter.fromBody(input, MyObject.class);
  }

  @Test(expected = ConversionException.class) public void deserializeWrongClass() throws Exception {
    final TypedInput input = new TypedByteArray(MIME_TYPE, objAsJson.getBytes());
    converter.fromBody(input, String.class);
  }

  private String asString(TypedOutput typedOutput) throws Exception {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    typedOutput.writeTo(bytes);
    return new String(bytes.toByteArray());
  }

  static class MyObject {
    private final String message;
    private final int count;

    public MyObject(@JsonProperty("message") String message, @JsonProperty("count") int count) {
      this.message = message;
      this.count = count;
    }

    public String getMessage() {
      return message;
    }

    public int getCount() {
      return count;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + count;
      result = prime * result + ((message == null) ? 0 : message.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      MyObject other = (MyObject) obj;
      if (count != other.count) {
        return false;
      }
      if (message == null) {
        if (other.message != null) {
          return false;
        }
      } else if (!message.equals(other.message)) {
        return false;
      }
      return true;
    }
  }
}
