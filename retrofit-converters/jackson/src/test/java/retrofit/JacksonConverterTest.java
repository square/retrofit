package retrofit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import okio.Buffer;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JacksonConverterTest {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
  private static final MyObject OBJECT = new MyObject("hello world", 10);
  private final String JSON = "{\"message\":\"hello world\",\"count\":10}";

  private final JacksonConverter converter = new JacksonConverter();

  @Test public void serialize() throws Exception {
    RequestBody body = converter.toBody(OBJECT, MyObject.class);
    assertThat(body.contentType()).isEqualTo(MEDIA_TYPE);
    assertBody(body).isEqualTo(JSON);
  }

  @Test public void deserialize() throws Exception {
    ResponseBody body = ResponseBody.create(MEDIA_TYPE, JSON);
    MyObject result = (MyObject) converter.fromBody(body, MyObject.class);
    assertThat(result).isEqualTo(OBJECT);
  }

  @Test public void deserializeWrongValue() throws Exception {
    ResponseBody body = ResponseBody.create(MEDIA_TYPE, "{\"foo\":\"bar\"}");
    try {
      converter.fromBody(body, MyObject.class);
    } catch (UnrecognizedPropertyException ignored) {
    }
  }

  @Test public void deserializeWrongClass() throws Exception {
    ResponseBody body = ResponseBody.create(MEDIA_TYPE, JSON);
    try {
      converter.fromBody(body, String.class);
    } catch (JsonMappingException ignored) {
    }
  }

  private static AbstractCharSequenceAssert<?, String> assertBody(RequestBody body) throws IOException {
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    return assertThat(buffer.readUtf8());
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
