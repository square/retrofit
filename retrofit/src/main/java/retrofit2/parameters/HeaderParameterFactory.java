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
import retrofit2.http.Header;
import retrofit2.http.HeaderMap;

public class HeaderParameterFactory implements ParameterHandler.Factory {

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof Header) {
      Header header = (Header) annotation;
      String name = header.value();

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
        return new NamedParameterHandler<>(name, new HeaderHandler<>(converter)).iterable();
      } else if (rawParameterType.isArray()) {
        Class<?> arrayComponentType = Utils.boxIfPrimitive(rawParameterType.getComponentType());
        Converter<?, String> converter =
            retrofit.stringConverter(arrayComponentType, annotations);
        return new NamedParameterHandler<>(name, new HeaderHandler<>(converter)).array();
      } else {
        Converter<?, String> converter =
            retrofit.stringConverter(type, annotations);
        return new NamedParameterHandler<>(name, new HeaderHandler<>(converter));
      }
    } else if (annotation instanceof HeaderMap) {
      Converter<?, String> converter =
          retrofit.stringConverter(MapParameterHandler.getValueType(type, annotation), annotations);
      return new MapParameterHandler<>(new HeaderHandler<>(converter), "Header");
    }
    return null;
  }

  static final class HeaderHandler<T> implements NamedValuesHandler<T> {
    private final Converter<T, String> valueConverter;

    HeaderHandler(Converter<T, String> valueConverter) {
      this.valueConverter = valueConverter;
    }

    @Override
    public void apply(RequestBuilder builder, String name, T value) throws IOException {
      if (value == null) return; // Skip null values.
      builder.addHeader(name, valueConverter.convert(value));
    }
  }

}
