package retrofit;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class StringListConverterFactory extends Converter.Factory {
  private static final MediaType MEDIA_TYPE = MediaType.parse("text/plain");

  @Override
  public Converter<ResponseBody, List<String>> fromResponseBody(Type type,
      Annotation[] annotations) {
    return new Converter<ResponseBody, List<String>>() {
      @Override public List<String> convert(ResponseBody value) throws IOException {
        return Collections.singletonList(value.string());
      }
    };
  }

  @Override public Converter<List<String>, RequestBody> toRequestBody(Type type,
      Annotation[] annotations) {
    return new Converter<List<String>, RequestBody>() {
      @Override public RequestBody convert(List<String> value) throws IOException {
        return RequestBody.create(MEDIA_TYPE, value.toString());
      }
    };
  }
}
