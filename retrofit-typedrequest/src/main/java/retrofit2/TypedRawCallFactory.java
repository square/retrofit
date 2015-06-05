package retrofit2;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;

class TypedRawCallFactory<T> implements CallFactory<T> {
  private final Retrofit retrofit;
  private final TypedRequest request;
  private final Converter<ResponseBody, T> responseConverter;

  TypedRawCallFactory(Retrofit retrofit, Type responseType, TypedRequest request) {
    this.retrofit = retrofit;
    this.request = request;
    responseConverter = retrofit.responseBodyConverter(responseType, new Annotation[0]);
  }

  @Override public okhttp3.Call create(Object... args) throws IOException {
    TypedRequestRawRequestBuilder requestBuilder =
        new TypedRequestRawRequestBuilder(retrofit, request);
    return retrofit.callFactory().newCall(requestBuilder.build());
  }

  @Override public T toResponse(ResponseBody body) throws IOException {
    return responseConverter.convert(body);
  }
}
