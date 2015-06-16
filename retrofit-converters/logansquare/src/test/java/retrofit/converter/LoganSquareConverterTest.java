package retrofit.converter;


import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okio.Buffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class LoganSquareConverterTest {

  private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
  private LoganSquareConverter converter = new LoganSquareConverter();

  //Simple JSON
  private static final MyObject OBJECT = new MyObject("hello world", 10);
  private final String JSON = "{\"message\":\"hello world\",\"count\":10}";

  //JSON for testing List type
  private final String LIST_JSON = "[{\"fullName\":\"sample\"}]";
  private List<User> userList = new ArrayList<User>();

  @Before
  public void setUp() {
    User user = new User("sample");
    userList.add(user);
  }

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
  public void serializeList() throws Exception {
    RequestBody body = converter.toBody(userList, Users.class);
    assertThat(body.contentType()).isEqualTo(MEDIA_TYPE);
    assertForEquality(body, LIST_JSON);
  }

  @Test
  public void deserializeWrongClass() throws Exception {
    ResponseBody body = ResponseBody.create(MEDIA_TYPE, JSON);
    try {
      converter.fromBody(body, String.class);
      fail();
    } catch (Exception ignored) {
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
