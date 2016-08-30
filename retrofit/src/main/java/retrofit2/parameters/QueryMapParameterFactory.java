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
import retrofit2.http.QueryMap;

public class QueryMapParameterFactory implements ParameterHandler.Factory {

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof QueryMap) {
      Class<?> rawParameterType = Utils.getRawType(type);
      if (!Map.class.isAssignableFrom(rawParameterType)) {
        throw  new IllegalArgumentException("@QueryMap parameter type must be Map.");
      }
      Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
      if (!(mapType instanceof ParameterizedType)) {
        throw new IllegalArgumentException(
            "Map must include generic types (e.g., Map<String, String>)");
      }
      ParameterizedType parameterizedType = (ParameterizedType) mapType;
      Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
      if (String.class != keyType) {
        throw  new IllegalArgumentException("@QueryMap keys must be of type String: " + keyType);
      }
      Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
      Converter<?, String> valueConverter =
          retrofit.stringConverter(valueType, annotations);

      return new QueryMapParameter<>(valueConverter, ((QueryMap) annotation).encoded());
    }
    return null;
  }

  static final class QueryMapParameter<T> extends ParameterHandler<Map<String, T>> {
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    QueryMapParameter(Converter<T, String> valueConverter, boolean encoded) {
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override
    public void apply(RequestBuilder builder, Map<String, T> value) throws IOException {
      if (value == null) {
        throw new IllegalArgumentException("Query map was null.");
      }

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Query map contained null key.");
        }
        T entryValue = entry.getValue();
        if (entryValue == null) {
          throw new IllegalArgumentException(
              "Query map contained null value for key '" + entryKey + "'.");
        }
        builder.addQueryParam(entryKey, valueConverter.convert(entryValue), encoded);
      }
    }
  }
}
