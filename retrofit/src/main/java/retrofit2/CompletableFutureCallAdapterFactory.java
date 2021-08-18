/*
 * Copyright (C) 2016 Square, Inc.
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
package retrofit2;

import android.annotation.TargetApi;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

@IgnoreJRERequirement // Only added when CompletableFuture is available (Java 8+ / Android API 24+).
@TargetApi(24)
final class CompletableFutureCallAdapterFactory extends CallAdapter.Factory {
  @Override
  public @Nullable CallAdapter<?, ?> get(
      Type returnType, Annotation[] annotations, Retrofit retrofit) {
    if (getRawType(returnType) != CompletableFuture.class) {
      return null;
    }
    if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalStateException(
          "CompletableFuture return type must be parameterized"
              + " as CompletableFuture<Foo> or CompletableFuture<? extends Foo>");
    }
    Type innerType = getParameterUpperBound(0, (ParameterizedType) returnType);

    if (getRawType(innerType) != Response.class) {
      // Generic type is not Response<T>. Use it for body-only adapter.
      return new BodyCallAdapter<>(innerType);
    }

    // Generic type is Response<T>. Extract T and create the Response version of the adapter.
    if (!(innerType instanceof ParameterizedType)) {
      throw new IllegalStateException(
          "Response must be parameterized" + " as Response<Foo> or Response<? extends Foo>");
    }
    Type responseType = getParameterUpperBound(0, (ParameterizedType) innerType);
    return new ResponseCallAdapter<>(responseType);
  }

  @IgnoreJRERequirement
  private static final class BodyCallAdapter<R> implements CallAdapter<R, CompletableFuture<R>> {
    private final Type responseType;

    BodyCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override
    public Type responseType() {
      return responseType;
    }

    @Override
    public CompletableFuture<R> adapt(final Call<R> call) {
      CompletableFuture<R> future = new CallCancelCompletableFuture<>(call);
      call.enqueue(new BodyCallback(future));
      return future;
    }

    @IgnoreJRERequirement
    private class BodyCallback implements Callback<R> {
      private final CompletableFuture<R> future;

      public BodyCallback(CompletableFuture<R> future) {
        this.future = future;
      }

      @Override
      public void onResponse(Call<R> call, Response<R> response) {
        if (response.isSuccessful()) {
          future.complete(response.body());
        } else {
          future.completeExceptionally(new HttpException(response));
        }
      }

      @Override
      public void onFailure(Call<R> call, Throwable t) {
        future.completeExceptionally(t);
      }
    }
  }

  @IgnoreJRERequirement
  private static final class ResponseCallAdapter<R>
      implements CallAdapter<R, CompletableFuture<Response<R>>> {
    private final Type responseType;

    ResponseCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override
    public Type responseType() {
      return responseType;
    }

    @Override
    public CompletableFuture<Response<R>> adapt(final Call<R> call) {
      CompletableFuture<Response<R>> future = new CallCancelCompletableFuture<>(call);
      call.enqueue(new ResponseCallback(future));
      return future;
    }

    @IgnoreJRERequirement
    private class ResponseCallback implements Callback<R> {
      private final CompletableFuture<Response<R>> future;

      public ResponseCallback(CompletableFuture<Response<R>> future) {
        this.future = future;
      }

      @Override
      public void onResponse(Call<R> call, Response<R> response) {
        future.complete(response);
      }

      @Override
      public void onFailure(Call<R> call, Throwable t) {
        future.completeExceptionally(t);
      }
    }
  }

  @IgnoreJRERequirement
  private static final class CallCancelCompletableFuture<T> extends CompletableFuture<T> {
    private final Call<?> call;

    CallCancelCompletableFuture(Call<?> call) {
      this.call = call;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      if (mayInterruptIfRunning) {
        call.cancel();
      }
      return super.cancel(mayInterruptIfRunning);
    }
  }
}
