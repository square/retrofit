package retrofit2;

import java.lang.reflect.Method;

import okhttp3.Request;

/**
 * With this interceptor, you can intercept the request build process and modify
 * the request as you want it to be executed. i.e. adding headers based on annotations
 */
public interface RequestBuildInterceptor {

  /**
   * @param request the request object as it has been generated based on the method
   * @return a (modified) request, as you want it to be called
   */
  Request build(Request request);

  /**
   * Creates {@link RequestBuildInterceptor} based on the method which has been called
   */
  interface Factory {
    RequestBuildInterceptor get(Method method);
  }
}
