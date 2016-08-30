package retrofit2.parameters;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
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

public class PartParameterFactory implements ParameterHandler.Factory {

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof Part) {
      Part part = (Part) annotation;


      String partName = part.value();
      Class<?> rawParameterType = Utils.getRawType(type);
      if (partName.isEmpty()) {
        if (Iterable.class.isAssignableFrom(rawParameterType)) {
          if (!(type instanceof ParameterizedType)) {
            throw new IllegalArgumentException(rawParameterType.getSimpleName()
                + " must include generic type (e.g., "
                + rawParameterType.getSimpleName()
                + "<String>)");
          }
          ParameterizedType parameterizedType = (ParameterizedType) type;
          Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
          if (!MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(iterableType))) {
            throw new IllegalArgumentException(
                "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
          }
          return new RawPartParameter().iterable();
        } else if (rawParameterType.isArray()) {
          Class<?> arrayComponentType = rawParameterType.getComponentType();
          if (!MultipartBody.Part.class.isAssignableFrom(arrayComponentType)) {
            throw new IllegalArgumentException(
                "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
          }
          return new RawPartParameter().array();
        } else if (MultipartBody.Part.class.isAssignableFrom(rawParameterType)) {
          return new RawPartParameter();
        } else {
          throw new IllegalArgumentException(
              "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
        }
      } else {
        Headers headers =
            Headers.of("Content-Disposition", "form-data; name=\"" + partName + "\"",
                "Content-Transfer-Encoding", part.encoding());

        if (Iterable.class.isAssignableFrom(rawParameterType)) {
          if (!(type instanceof ParameterizedType)) {
            throw new IllegalArgumentException(rawParameterType.getSimpleName()
                + " must include generic type (e.g., "
                + rawParameterType.getSimpleName()
                + "<String>)");
          }
          ParameterizedType parameterizedType = (ParameterizedType) type;
          Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
          if (MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(iterableType))) {
            throw new IllegalArgumentException(
                "@Part parameters using the MultipartBody.Part must not "
                + "include a part name in the annotation.");
          }
          Converter<?, RequestBody> converter =
              retrofit.requestBodyConverter(iterableType, annotations, methodAnnotations);
          return new PartParameter<>(headers, converter).iterable();
        } else if (rawParameterType.isArray()) {
          Class<?> arrayComponentType = Utils.boxIfPrimitive(rawParameterType.getComponentType());
          if (MultipartBody.Part.class.isAssignableFrom(arrayComponentType)) {
            throw new IllegalArgumentException(
                "@Part parameters using the MultipartBody.Part must not "
                + "include a part name in the annotation.");
          }
          Converter<?, RequestBody> converter =
              retrofit.requestBodyConverter(arrayComponentType, annotations, methodAnnotations);
          return new PartParameter<>(headers, converter).array();
        } else if (MultipartBody.Part.class.isAssignableFrom(rawParameterType)) {
          throw new IllegalArgumentException(
              "@Part parameters using the MultipartBody.Part must not "
              + "include a part name in the annotation.");
        } else {
          Converter<?, RequestBody> converter =
              retrofit.requestBodyConverter(type, annotations, methodAnnotations);
          return new PartParameter<>(headers, converter);
        }
      }
    }
    return null;
  }

  static final class PartParameter<T> extends ParameterHandler<T> {
    private final Headers headers;
    private final Converter<T, RequestBody> converter;

    PartParameter(Headers headers, Converter<T, RequestBody> converter) {
      this.headers = headers;
      this.converter = converter;
    }

    @Override
    public void apply(RequestBuilder builder, T value) {
      if (value == null) return; // Skip null values.

      RequestBody body;
      try {
        body = converter.convert(value);
      } catch (IOException e) {
        throw new RuntimeException("Unable to convert " + value + " to RequestBody", e);
      }
      builder.addPart(headers, body);
    }
  }

  static final class RawPartParameter extends ParameterHandler<MultipartBody.Part> {

    @Override
    public void apply(RequestBuilder builder, MultipartBody.Part value) throws IOException {
      if (value != null) { // Skip null values.
        builder.addPart(value);
      }
    }
  }
}
