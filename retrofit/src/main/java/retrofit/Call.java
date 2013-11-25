// Copyright 2013 Square, Inc.
package retrofit;

import java.util.concurrent.Executor;

/**
 * API 2.0 future request invocation/call object.
 */
public class Call<T> {

  private final RestAdapter.RestHandler handler;
  private final RequestInterceptor interceptor;
  private final RestMethodInfo methodInfo;
  private final Object[] args;
  private final Executor httpExecutor;

  /**
   * Constructor
   *
   * @param handler RestHandler that created this Call<T> and
   * that will carry out the actual request.
   * @param interceptor Interceptor to call invokeRequest() with.
   * @param methodInfo RestMethodInfo to call invokeRequest() with.
   * @param args Object[] to call invokeRequest() with.
   * @param httpExecutor Executor for HTTP requests.
   */
  public Call(RestAdapter.RestHandler handler,
      RequestInterceptor interceptor, RestMethodInfo methodInfo,
      Object[] args, Executor httpExecutor) {
    this.handler = handler;
    this.interceptor = interceptor;
    this.methodInfo = methodInfo;
    this.args = args;
    this.httpExecutor = httpExecutor;
  }

  /**
   * Execute the request synchronously.
   * @return the request's response of type T
   */
  @SuppressWarnings("unchecked")
  public T execute() {
    return (T) handler.invokeRequest(interceptor, methodInfo, args);
  }

  /**
   * Execute the request asynchronously with a (API 2.0) callback.
   * @param callback (API 2.0) Callback2 to process the request with.
   * @return Asynchronous execute() return void.
   */
  public void execute(final Callback2<T> callback) {
    // Not handling the interceptor or observable for now. The synchronous
    // version of RestHandler.invoke() does not handle this either.

    httpExecutor.execute(new Runnable() {
      @SuppressWarnings("unchecked")
      @Override
      public void run() {
        T response;

        try {
          response = (T) handler.invokeRequest(interceptor, methodInfo, args);
        } catch (RetrofitError err) {
          callback.failure(err);
          return;
        }

        callback.success(response);
      }
    });
  }
}
