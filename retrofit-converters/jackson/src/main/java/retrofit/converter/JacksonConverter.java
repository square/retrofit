package retrofit.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * A {@link Converter} which uses Jackson for reading and writing entities.
 *
 * @author Kai Waldron (kaiwaldron@gmail.com)
 */
public class JacksonConverter implements Converter {
  private static final String MIME_TYPE = "application/json; charset=UTF-8";

  private final ObjectMapper objectMapper;

  public JacksonConverter() {
    this(new ObjectMapper());
  }

  public JacksonConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override public Object fromBody(TypedInput body, Type type) throws IOException {
    InputStream in = null;
    try {
      JavaType javaType = objectMapper.getTypeFactory().constructType(type);
      in = body.in();
      return objectMapper.readValue(in, javaType);
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException ignored) {
      }
    }
  }

  @Override public TypedOutput toBody(Object object, Type type) {
    try {
      JavaType javaType = objectMapper.getTypeFactory().constructType(type);
      String json = objectMapper.writerWithType(javaType).writeValueAsString(object);
      return new TypedByteArray(MIME_TYPE, json.getBytes("UTF-8"));
    } catch (JsonProcessingException e) {
      throw new AssertionError(e);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }
}
