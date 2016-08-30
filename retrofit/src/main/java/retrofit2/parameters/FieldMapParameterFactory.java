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
import retrofit2.http.FieldMap;

public class FieldMapParameterFactory implements ParameterHandler.Factory {

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof FieldMap) {
      Class<?> rawParameterType = Utils.getRawType(type);
      if (!Map.class.isAssignableFrom(rawParameterType)) {
        throw new IllegalArgumentException("@FieldMap parameter type must be Map.");
      }
      Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
      if (!(mapType instanceof ParameterizedType)) {
        throw new IllegalArgumentException(
            "Map must include generic types (e.g., Map<String, String>)");
      }
      ParameterizedType parameterizedType = (ParameterizedType) mapType;
      Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
      if (String.class != keyType) {
        throw new IllegalArgumentException("@FieldMap keys must be of type String: " + keyType);
      }
      Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
      Converter<?, String> valueConverter =
          retrofit.stringConverter(valueType, annotations);

      return new FieldMapParameter<>(valueConverter, ((FieldMap) annotation).encoded());
    }
    return null;
  }

  static final class FieldMapParameter<T> extends ParameterHandler<Map<String, T>> {
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    FieldMapParameter(Converter<T, String> valueConverter, boolean encoded) {
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override
    public void apply(RequestBuilder builder, Map<String, T> value) throws IOException {
      if (value == null) {
        throw new IllegalArgumentException("Field map was null.");
      }

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Field map contained null key.");
        }
        T entryValue = entry.getValue();
        if (entryValue == null) {
          throw new IllegalArgumentException(
              "Field map contained null value for key '" + entryKey + "'.");
        }
        builder.addFormField(entryKey, valueConverter.convert(entryValue), encoded);
      }
    }
  }
}
