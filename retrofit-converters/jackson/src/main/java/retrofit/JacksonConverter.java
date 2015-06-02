package retrofit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * A {@link Converter} which uses Jackson for reading and writing entities.
 *
 * @author Kai Waldron (kaiwaldron@gmail.com)
 */
public class JacksonConverter implements Converter {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");

  private final ObjectMapper objectMapper;

  public JacksonConverter() {
    this(new ObjectMapper());
  }

  public JacksonConverter(ObjectMapper objectMapper) {
    if (objectMapper == null) throw new NullPointerException("objectMapper == null");
    this.objectMapper = objectMapper;
  }

  @Override public Object fromBody(ResponseBody body, Type type) throws IOException {
    InputStream is = body.byteStream();
    try {
      JavaType javaType = objectMapper.getTypeFactory().constructType(type);
      return objectMapper.readValue(is, javaType);
    } finally {
      try {
        is.close();
      } catch (IOException ignored) {
      }
    }
  }

  @Override public RequestBody toBody(Object object, Type type) {
    try {
      JavaType javaType = objectMapper.getTypeFactory().constructType(type);
      String json = objectMapper.writerWithType(javaType).writeValueAsString(object);
      return RequestBody.create(MEDIA_TYPE, json);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
