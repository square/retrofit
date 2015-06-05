package retrofit2;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class StringListConverterFactory extends Converter.Factory {
  private static final MediaType MEDIA_TYPE = MediaType.parse("text/plain");

  @Override
  public Converter<ResponseBody, List<String>> responseBodyConverter(Type type,
      Annotation[] annotations, Retrofit retrofit2) {
    return new Converter<ResponseBody, List<String>>() {
      @Override public List<String> convert(ResponseBody value) throws IOException {
        return Collections.singletonList(value.string());
      }
    };
  }

  @Override public Converter<List<String>, RequestBody> requestBodyConverter(Type type,
      Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
    return new Converter<List<String>, RequestBody>() {
      @Override public RequestBody convert(List<String> value) throws IOException {
        return RequestBody.create(MEDIA_TYPE, value.toString());
      }
    };
  }
}
