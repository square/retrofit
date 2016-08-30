package retrofit2.parameters;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import retrofit2.Converter;
import retrofit2.ParameterHandler;
import retrofit2.RequestBuilder;
import retrofit2.Retrofit;
import retrofit2.http.Header;
import retrofit2.http.HeaderMap;

public class HeaderParameterFactory implements ParameterHandler.Factory {

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof Header) {
      Header header = (Header) annotation;
      String name = header.value();

      Type itemType = RepeatedParameterHelper.getItemType(type);
      Converter<?, String> converter = retrofit.stringConverter(itemType, annotations);
      return RepeatedParameterHelper.wrapIfRepeated(type,
          new NamedParameterHandler<>(name, new HeaderHandler<>(converter)));
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
