package retrofit2.parameters;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Converter;
import retrofit2.ParameterHandler;
import retrofit2.RequestBuilder;
import retrofit2.Retrofit;
import retrofit2.Utils;
import retrofit2.http.Part;
import retrofit2.http.PartMap;

public class PartParameterFactory implements ParameterHandler.Factory {

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof Part) {
      Part part = (Part) annotation;
      String partName = part.value();

      Type itemType = RepeatedParameterHelper.getItemType(type);

      if (MultipartBody.Part.class.isAssignableFrom((Class<?>) itemType)) {
        if (!partName.isEmpty()) {
          throw new IllegalArgumentException(
              "@Part parameters using the MultipartBody.Part must not "
                  + "include a part name in the annotation.");
        }
        return RepeatedParameterHelper.wrapIfRepeated(type, new RawPartParameter());
      } else {
        if (partName.isEmpty()) {
          throw new IllegalArgumentException(
              "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
        }
        String encoding = part.encoding();
        Converter<?, RequestBody> converter =
            retrofit.requestBodyConverter(itemType, annotations, methodAnnotations);
        return RepeatedParameterHelper.wrapIfRepeated(type,
            new NamedParameterHandler<>(partName, new PartHandler<>(encoding, converter))
        );
      }
    } else if (annotation instanceof PartMap) {
      Type valueType = MapParameterHandler.getValueType(type, annotation);
      if (MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(valueType))) {
        throw new IllegalArgumentException("@PartMap values cannot be MultipartBody.Part. "
            + "Use @Part List<Part> or a different value type instead.");
      }

      PartMap partMap = (PartMap) annotation;

      Converter<?, RequestBody> converter =
          retrofit.requestBodyConverter(valueType, annotations, methodAnnotations);
      return new MapParameterHandler<>(new PartHandler<>(partMap.encoding(), converter), "Part");
    }
    return null;
  }

  static final class PartHandler<T> implements NamedValuesHandler<T> {
    private final String encoding;
    private final Converter<T, RequestBody> converter;

    PartHandler(String encoding, Converter<T, RequestBody> converter) {
      this.encoding = encoding;
      this.converter = converter;
    }

    @Override
    public void apply(RequestBuilder builder, String name, T value) {
      if (value == null) return; // Skip null values.

      RequestBody body;
      try {
        body = converter.convert(value);
      } catch (IOException e) {
        throw new RuntimeException("Unable to convert " + value + " to RequestBody", e);
      }
      Headers headers =
          Headers.of("Content-Disposition", "form-data; name=\"" + name + "\"",
              "Content-Transfer-Encoding", encoding);
      builder.addPart(headers, body);
    }
  }

  static final class RawPartParameter implements ParameterHandler<MultipartBody.Part> {

    @Override
    public void apply(RequestBuilder builder, MultipartBody.Part value) throws IOException {
      if (value != null) { // Skip null values.
        builder.addPart(value);
      }
    }
  }
}
