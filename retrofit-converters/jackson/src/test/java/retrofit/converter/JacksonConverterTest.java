package retrofit.converter;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.ByteArrayOutputStream;
import org.junit.Test;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

import static org.assertj.core.api.Assertions.assertThat;

public class JacksonConverterTest {
  private static final String MIME_TYPE = "application/json; charset=UTF-8";
  private static final MyObject OBJECT = new MyObject("hello world", 10);
  private final String JSON = "{\"message\":\"hello world\",\"count\":10}";

  private final JacksonConverter converter = new JacksonConverter();

  @Test public void serialize() throws Exception {
    TypedOutput typedOutput = converter.toBody(OBJECT);
    assertThat(typedOutput.mimeType()).isEqualTo(MIME_TYPE);
    assertThat(asString(typedOutput)).isEqualTo(JSON);
  }

  @Test public void deserialize() throws Exception {
    TypedInput input = new TypedByteArray(MIME_TYPE, JSON.getBytes());
    MyObject result = (MyObject) converter.fromBody(input, MyObject.class);
    assertThat(result).isEqualTo(OBJECT);
  }

  @Test(expected = ConversionException.class)
  public void deserializeWrongValue() throws Exception {
    TypedInput input = new TypedByteArray(MIME_TYPE, "{\"foo\":\"bar\"}".getBytes());
    converter.fromBody(input, MyObject.class);
  }

  @Test(expected = ConversionException.class)
  public void deserializeWrongClass() throws Exception {
    TypedInput input = new TypedByteArray(MIME_TYPE, JSON.getBytes());
    converter.fromBody(input, String.class);
  }

  private String asString(TypedOutput typedOutput) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
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

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      MyObject myObject = (MyObject) o;
      return count == myObject.count
          && !(message != null ? !message.equals(myObject.message) : myObject.message != null);
    }

    @Override public int hashCode() {
      int result = message != null ? message.hashCode() : 0;
      result = 31 * result + count;
      return result;
    }
  }
}
