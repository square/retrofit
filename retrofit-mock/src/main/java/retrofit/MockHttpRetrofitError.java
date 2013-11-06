// Copyright 2013 Square, Inc.
package retrofit;

import java.lang.reflect.Type;
import retrofit.client.Response;

class MockHttpRetrofitError extends RetrofitError {
  private final Object body;

  MockHttpRetrofitError(String url, Response response, Object body) {
    super(url, response, null, null, false, null);
    this.body = body;
  }

  @Override public Object getBody() {
    return body;
  }

  @Override public Object getBodyAs(Type type) {
    return body;
  }
}
