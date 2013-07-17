package retrofit.converter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A {@link Converter} which uses Jackson for serializing/deserializing entities.
 *
 * @author Kai Waldron (kaiwaldron@gmail.com)
 */
public class JacksonConverter implements Converter {
  private final ObjectMapper objectMapper;

  public JacksonConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @SuppressWarnings("unchecked")
  @Override public Object fromBody(TypedInput body, Type type) throws ConversionException {
    try {
      return objectMapper.readValue(body.in(), (Class<Object>) type);
    } catch (final JsonParseException e) {
      throw new ConversionException(e);
    } catch (final JsonMappingException e) {
      throw new ConversionException(e);
    } catch (final IOException e) {
      throw new ConversionException(e);
    }
  }

  @Override public TypedOutput toBody(Object object) {
    try {
      final String json = objectMapper.writeValueAsString(object);
      return new JsonTypedOutput(json.getBytes("UTF-8"));
    } catch (final JsonProcessingException e) {
      throw new AssertionError(e);
    } catch (final UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }
}
