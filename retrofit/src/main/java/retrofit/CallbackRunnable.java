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

/**
 * A {@link Runnable} executed on a background thread to invoke {@link #obtainResponse()} which
 * performs an HTTP request. The response of the request, whether it be an object or exception, is
 * then marshaled to the supplied {@link Executor} in the form of a method call on a
 * {@link Callback}.
 */
abstract class CallbackRunnable<T> implements Runnable {
  private Callback<T> callback;
  private final Executor callbackExecutor;

  CallbackRunnable(Callback<T> callback, Executor callbackExecutor) {
    this.callback = callback;
    this.callbackExecutor = callbackExecutor;

    if (callback instanceof CancelableCallback) {
      CancelableCallback<T> cancelableCallback = (CancelableCallback<T>) callback;
      cancelableCallback.setRunnable(this);
    }
  }

  void cancel() {
    callback = null;
  }

  @SuppressWarnings("unchecked")
  @Override public final void run() {
    // Make sure we haven't been cancelled.
    if (callback == null) {
      return;
    }

    try {
      final ResponseWrapper wrapper = obtainResponse();

      // Check again if we are cancelled since obtaining the response may have taken a while.
      if (callback == null) {
        return;
      }

      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          // One last cancellation check that might have happened while crossing threads.
          if (callback != null) {
            callback.success((T) wrapper.responseBody, wrapper.response);
          }
        }
      });
    } catch (final RetrofitError e) {
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          // One last cancellation check that might have happened while crossing threads.
          if (callback != null) {
            callback.failure(e);
          }
        }
      });
    }
  }

  abstract ResponseWrapper obtainResponse();
}
