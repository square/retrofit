package retrofit;

import com.squareup.okhttp.Request;

class TypedRawRequestFactory implements RequestFactory {
  private final Retrofit retrofit;
  private final TypedRequest request;

  TypedRawRequestFactory(Retrofit retrofit, TypedRequest request) {
    this.retrofit = retrofit;
    this.request = request;
  }

  @Override public Request create(Object... args) {
    TypedRequestRawRequestBuilder requestBuilder =
        new TypedRequestRawRequestBuilder(retrofit, request);
    return requestBuilder.build();
  }
}
