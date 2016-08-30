package retrofit2.parameters;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import retrofit2.Converter;
import retrofit2.ParameterHandler;
import retrofit2.RequestBuilder;
import retrofit2.Retrofit;
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

      Type itemType = RepeatedParameterHelper.getItemType(type);
      Converter<?, String> converter = retrofit.stringConverter(itemType, annotations);
      return RepeatedParameterHelper.wrapIfRepeated(type,
          new NamedParameterHandler<>(name, new FieldHandler<>(converter, encoded)));
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
