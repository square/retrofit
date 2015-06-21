/*
 * Copyright (C) 2015 Square, Inc.
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

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;

final class DefaultCallAdapterFactory implements CallAdapter.Factory {
  private final Executor callbackExecutor;

  DefaultCallAdapterFactory(Executor callbackExecutor) {
    this.callbackExecutor = callbackExecutor;
  }

  @Override public String toString() {
    return "Default CallAdapterFactory";
  }

  @Override public CallAdapter<?> get(Type returnType) {
    if (Utils.getRawType(returnType) != Call.class) {
      return null;
    }
    if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalArgumentException(
          "Call return type must be parameterized as Call<Foo> or Call<? extends Foo>");
    }
    final Type responseType = Utils.getSingleParameterUpperBound((ParameterizedType) returnType);

    // Ensure the Call response type is not Response, we automatically deliver the Response object.
    if (Utils.getRawType(responseType) == Response.class) {
      throw new IllegalArgumentException(
          "Call<T> cannot use Response as its generic parameter. "
              + "Specify the response body type only (e.g., Call<TweetResponse>).");
    }

    return new CallAdapter<Object>() {
      @Override public Type responseType() {
        return responseType;
      }

      @Override public Call<Object> adapt(Call<Object> call) {
        return new ExecutorCallbackCall<>(callbackExecutor, call);
      }
    };
  }

  static final class ExecutorCallbackCall<T> implements Call<T> {
    private final Executor callbackExecutor;
    private final Call<T> delegate;

    ExecutorCallbackCall(Executor callbackExecutor, Call<T> delegate) {
      this.callbackExecutor = callbackExecutor;
      this.delegate = delegate;
    }

    @Override public void enqueue(Callback<T> callback) {
      delegate.enqueue(new ExecutorCallback<>(callbackExecutor, callback));
    }

    @Override public Response<T> execute() throws IOException {
      return delegate.execute();
    }

    @Override public void cancel() {
      delegate.cancel();
    }

    @Override public Call<T> clone() {
      return new ExecutorCallbackCall<>(callbackExecutor, delegate.clone());
    }
  }

  static final class ExecutorCallback<T> implements Callback<T> {
    private final Executor callbackExecutor;
    private final Callback<T> delegate;

    ExecutorCallback(Executor callbackExecutor, Callback<T> delegate) {
      this.callbackExecutor = callbackExecutor;
      this.delegate = delegate;
    }

    @Override public void success(final Response<T> response) {
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          delegate.success(response);
        }
      });
    }

    @Override public void failure(final Throwable t) {
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          delegate.failure(t);
        }
      });
    }
  }
}
