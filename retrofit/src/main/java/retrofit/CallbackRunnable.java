/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit;

import java.util.concurrent.Executor;

import static retrofit.RetrofitError.unexpectedError;

/**
 * A {@link Runnable} executed on a background thread to invoke {@link #obtainResponse()} which
 * performs an HTTP request. The response of the request, whether it be an object or exception, is
 * then marshaled to the supplied {@link Executor} in the form of a method call on a
 * {@link Callback}.
 */
abstract class CallbackRunnable<T> implements Runnable {
  private final Callback<T> callback;
  private final Executor callbackExecutor;
  private final ErrorHandler errorHandler;

  CallbackRunnable(Callback<T> callback, Executor callbackExecutor, ErrorHandler errorHandler) {
    this.callback = callback;
    this.callbackExecutor = callbackExecutor;
    this.errorHandler = errorHandler;
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
    } catch (RetrofitError e) {
      Throwable cause = errorHandler.handleError(e);
      final RetrofitError handled = cause == e ? e : unexpectedError(e.getUrl(), cause);
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          callback.failure(handled);
        }
      });
    }
  }

  public abstract ResponseWrapper obtainResponse();
}
