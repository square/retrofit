package retrofit.converter;

import com.bluelinelabs.logansquare.LoganSquare;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * A {@link Converter} which uses LoganSquare for serialization and deserialization of entities.
 */
public class LoganSquareConverter implements Converter {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");

  @Override
  public Object fromBody(ResponseBody body, Type type) throws IOException {
    InputStream is = body.byteStream();
    try {
      if (ParameterizedType.class.isAssignableFrom(type.getClass())) {
        ParameterizedType parameterizedType = (ParameterizedType) type;
        return LoganSquare.parseList(is, (Class) parameterizedType.getActualTypeArguments()[0]);
      } else {
        return LoganSquare.parse(is, (Class) type);
      }
    } finally {
      try {
        is.close();
      } catch (IOException ignored) {
      }
    }
  }

  @Override
  public RequestBody toBody(Object object, Type type) {
    try {
      String json;
      if (List.class.isAssignableFrom(object.getClass())) {
        List<Object> list = (List<Object>) object;
        if (list.isEmpty()) {
          json = "[]";
        } else {
          json = LoganSquare.serialize(list, (Class<Object>) list.get(0).getClass());
        }
      } else {
        json = LoganSquare.serialize(object);
      }
      return RequestBody.create(MEDIA_TYPE, json);
    } catch (Exception e) {
      e.printStackTrace();
      throw new AssertionError(e);
    }
  }
}
