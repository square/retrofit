package retrofit2.parameters;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Converter;
import retrofit2.ParameterHandler;
import retrofit2.RequestBuilder;
import retrofit2.Retrofit;
import retrofit2.Utils;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;

public class FieldParameterFactory implements ParameterHandler.Factory {

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof Field) {
      Field field = (Field) annotation;
      String name = field.value();
      boolean encoded = field.encoded();


      Class<?> rawParameterType = Utils.getRawType(type);
      if (Iterable.class.isAssignableFrom(rawParameterType)) {
        if (!(type instanceof ParameterizedType)) {
          throw new IllegalArgumentException(rawParameterType.getSimpleName()
              + " must include generic type (e.g., "
              + rawParameterType.getSimpleName()
              + "<String>)");
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
        Converter<?, String> converter =
            retrofit.stringConverter(iterableType, annotations);
        return new NamedParameterHandler<>(name, new FieldHandler<>(converter, encoded))
            .iterable();
      } else if (rawParameterType.isArray()) {
        Class<?> arrayComponentType = Utils.boxIfPrimitive(rawParameterType.getComponentType());
        Converter<?, String> converter =
            retrofit.stringConverter(arrayComponentType, annotations);
        return new NamedParameterHandler<>(name, new FieldHandler<>(converter, encoded)).array();
      } else {
        Converter<?, String> converter =
            retrofit.stringConverter(type, annotations);
        return new NamedParameterHandler<>(name, new FieldHandler<>(converter, encoded));
      }
    } else if (annotation instanceof FieldMap) {
      FieldMap fieldMap = (FieldMap) annotation;
      Converter<?, String> converter =
          retrofit.stringConverter(MapParameterHandler.getValueType(type, annotation), annotations);
      return new MapParameterHandler<>(new FieldHandler<>(converter, fieldMap.encoded()), "Field");
    }
    return null;
  }

  static final class FieldHandler<T> implements NamedValuesHandler<T> {
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    FieldHandler(Converter<T, String> valueConverter, boolean encoded) {
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override
    public void apply(RequestBuilder builder, String name, T value) throws IOException {
      if (value == null) return; // Skip null values.
      builder.addFormField(name, valueConverter.convert(value), encoded);
    }
  }
}
