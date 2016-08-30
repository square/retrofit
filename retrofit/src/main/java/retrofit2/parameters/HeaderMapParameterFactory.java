package retrofit2.parameters;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import retrofit2.Converter;
import retrofit2.ParameterHandler;
import retrofit2.RequestBuilder;
import retrofit2.Retrofit;
import retrofit2.Utils;
import retrofit2.http.HeaderMap;

public class HeaderMapParameterFactory implements ParameterHandler.Factory {

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof HeaderMap) {
      Class<?> rawParameterType = Utils.getRawType(type);
      if (!Map.class.isAssignableFrom(rawParameterType)) {
        throw new IllegalArgumentException("@HeaderMap parameter type must be Map.");
      }
      Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
      if (!(mapType instanceof ParameterizedType)) {
        throw new IllegalArgumentException(
            "Map must include generic types (e.g., Map<String, String>)");
      }
      ParameterizedType parameterizedType = (ParameterizedType) mapType;
      Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
      if (String.class != keyType) {
        throw new IllegalArgumentException("@HeaderMap keys must be of type String: " + keyType);
      }
      Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
      Converter<?, String> valueConverter =
          retrofit.stringConverter(valueType, annotations);

      return new HeaderMapParameter<>(valueConverter);
    }
    return null;
  }

  static final class HeaderMapParameter<T> extends ParameterHandler<Map<String, T>> {
    private final Converter<T, String> valueConverter;

    HeaderMapParameter(Converter<T, String> valueConverter) {
      this.valueConverter = valueConverter;
    }

    @Override
    public void apply(RequestBuilder builder, Map<String, T> value) throws IOException {
      if (value == null) {
        throw new IllegalArgumentException("Header map was null.");
      }

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String headerName = entry.getKey();
        if (headerName == null) {
          throw new IllegalArgumentException("Header map contained null key.");
        }
        T headerValue = entry.getValue();
        if (headerValue == null) {
          throw new IllegalArgumentException(
              "Header map contained null value for key '" + headerName + "'.");
        }
        builder.addHeader(headerName, valueConverter.convert(headerValue));
      }
    }
  }
}
