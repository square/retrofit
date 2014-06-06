// Copyright 2013 Square, Inc.
package retrofit;

import java.lang.reflect.Type;
import retrofit.client.Response;

class MockHttpRetrofitError extends RetrofitError {
  private final Object body;

  MockHttpRetrofitError(String message, String url, Response response, Object body) {
    super(message, url, response, null, body.getClass(), false, null);
    this.body = body;
  }

  @Override public Object getBody() {
    return body;
  }

  @Override public Object getBodyAs(Type type) {
    return body;
  }
}
