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
import retrofit2.http.Query;

import static retrofit2.Utils.checkNotNull;

public class QueryParameterFactory implements ParameterHandler.Factory {

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof Query) {
      Query query = (Query) annotation;
      String name = query.value();
      boolean encoded = query.encoded();

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
        return new QueryParameter<>(name, converter, encoded).iterable();
      } else if (rawParameterType.isArray()) {
        Class<?> arrayComponentType = Utils.boxIfPrimitive(rawParameterType.getComponentType());
        Converter<?, String> converter =
            retrofit.stringConverter(arrayComponentType, annotations);
        return new QueryParameter<>(name, converter, encoded).array();
      } else {
        Converter<?, String> converter =
            retrofit.stringConverter(type, annotations);
        return new QueryParameter<>(name, converter, encoded);
      }
    }
    return null;
  }

  static final class QueryParameter<T> extends ParameterHandler<T> {
    private final String name;
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    QueryParameter(String name, Converter<T, String> valueConverter, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override
    public void apply(RequestBuilder builder, T value) throws IOException {
      if (value == null) return; // Skip null values.
      builder.addQueryParam(name, valueConverter.convert(value), encoded);
    }
  }

}
