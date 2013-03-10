// Copyright 2012 Square, Inc.
package retrofit.http;

import java.util.concurrent.Executor;

/**
 * A {@link Runnable} executed on a background thread to invoke {@link #obtainResponse()} which
 * performs an HTTP request. The response of the request, whether it be an object or exception, is
 * then marshaled to the supplied {@link Executor} in the form of a method call on a
 * {@link Callback}.
 */
abstract class CallbackRunnable<T> implements Runnable {
  private final Callback<T> callback;
  private final Executor callbackExecutor;

  CallbackRunnable(Callback<T> callback, Executor callbackExecutor) {
    this.callback = callback;
    this.callbackExecutor = callbackExecutor;
  }

  @SuppressWarnings("unchecked")
  @Override public final void run() {
    try {
      final ResponseWrapper wrapper = obtainResponse();
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          callback.success((T) wrapper.responseBody, wrapper.response);
        }
      });
    } catch (final RetrofitError e) {
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          callback.failure(e);
        }
      });
    }
  }

  public abstract ResponseWrapper obtainResponse();
}
