package retrofit.converter;


import com.bluelinelabs.logansquare.NoSuchMapperException;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import org.junit.Test;

import java.io.IOException;

import okio.Buffer;

import static org.assertj.core.api.Assertions.assertThat;

public class LoganSquareConverterTest {

  private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
  private static final MyObject OBJECT = new MyObject("hello world", 10);
  private final String JSON = "{\"message\":\"hello world\",\"count\":10}";
  private LoganSquareConverter converter = new LoganSquareConverter();

  @Test
  public void serialize() throws Exception {
    RequestBody body = converter.toBody(OBJECT, MyObject.class);
    assertThat(body.contentType()).isEqualTo(MEDIA_TYPE);
    assertForEquality(body, JSON);
  }

  @Test
  public void deserialize() throws Exception {
    ResponseBody body = ResponseBody.create(MEDIA_TYPE, JSON);
    MyObject result = (MyObject) converter.fromBody(body, MyObject.class);
    assertThat(result).isEqualTo(OBJECT);
  }

  @Test
  public void deserializeWrongValue() throws Exception {
    ResponseBody body = ResponseBody.create(MEDIA_TYPE, "{\"foo\":\"bar\"}");
    try {
      converter.fromBody(body, MyObject.class);
    } catch (Exception ignored) {
    }
  }

  @Test
  public void deserializeWrongClass() throws Exception {
    ResponseBody body = ResponseBody.create(MEDIA_TYPE, JSON);
    try {
      converter.fromBody(body, String.class);
    } catch (NoSuchMapperException ignored) {
    }
  }

  private static void assertForEquality(final RequestBody requestBody, final String json) throws IOException {
    Buffer buffer = new Buffer();
    requestBody.writeTo(buffer);

    JsonParser parser = new JsonParser();
    JsonElement t1 = parser.parse(buffer.readUtf8());
    JsonElement t2 = parser.parse(json);
    assertThat(t1).isEqualTo(t2);
  }
}
