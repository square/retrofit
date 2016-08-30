package retrofit2.parameters;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Converter;
import retrofit2.ParameterHandler;
import retrofit2.RequestBuilder;
import retrofit2.Retrofit;
import retrofit2.Utils;
import retrofit2.http.PartMap;

public class PartMapParameterFactory implements ParameterHandler.Factory {

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof PartMap) {
      Class<?> rawParameterType = Utils.getRawType(type);
      if (!Map.class.isAssignableFrom(rawParameterType)) {
        throw new IllegalArgumentException("@PartMap parameter type must be Map.");
      }
      Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
      if (!(mapType instanceof ParameterizedType)) {
        throw new IllegalArgumentException(
            "Map must include generic types (e.g., Map<String, String>)");
      }
      ParameterizedType parameterizedType = (ParameterizedType) mapType;

      Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
      if (String.class != keyType) {
        throw new IllegalArgumentException("@PartMap keys must be of type String: " + keyType);
      }

      Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
      if (MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(valueType))) {
        throw new IllegalArgumentException("@PartMap values cannot be MultipartBody.Part. "
            + "Use @Part List<Part> or a different value type instead.");
      }

      Converter<?, RequestBody> valueConverter =
          retrofit.requestBodyConverter(valueType, annotations, methodAnnotations);

      PartMap partMap = (PartMap) annotation;
      return new PartMapParameter<>(valueConverter, partMap.encoding());
    }
    return null;
  }

  static final class PartMapParameter<T> extends ParameterHandler<Map<String, T>> {
    private final Converter<T, RequestBody> valueConverter;
    private final String transferEncoding;

    PartMapParameter(Converter<T, RequestBody> valueConverter, String transferEncoding) {
      this.valueConverter = valueConverter;
      this.transferEncoding = transferEncoding;
    }

    @Override
    public void apply(RequestBuilder builder, Map<String, T> value) throws IOException {
      if (value == null) {
        throw new IllegalArgumentException("Part map was null.");
      }

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Part map contained null key.");
        }
        T entryValue = entry.getValue();
        if (entryValue == null) {
          throw new IllegalArgumentException(
              "Part map contained null value for key '" + entryKey + "'.");
        }

        Headers headers = Headers.of(
            "Content-Disposition", "form-data; name=\"" + entryKey + "\"",
            "Content-Transfer-Encoding", transferEncoding);

        builder.addPart(headers, valueConverter.convert(entryValue));
      }
    }
  }
}
