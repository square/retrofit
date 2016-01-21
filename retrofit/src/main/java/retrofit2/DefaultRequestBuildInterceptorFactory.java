package retrofit2;

import okhttp3.Request;

import java.lang.reflect.Method;

/**
 * a default implementation which is passing the request through, without
 * modifying it
 */
final class DefaultRequestBuildInterceptorFactory implements RequestBuildInterceptor {
  static final Factory FACTORY = new Factory() {
    @Override
    public RequestBuildInterceptor get(Method method) {
      return new DefaultRequestBuildInterceptorFactory();
    }
  };

  DefaultRequestBuildInterceptorFactory() {
  }

  @Override
  public Request build(Request request) {
    return request;
  }
}
