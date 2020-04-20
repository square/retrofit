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
package retrofit2.adapter.java8;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * @deprecated Retrofit includes support for CompletableFuture. This no longer needs to be added to
 *     the Retrofit instance explicitly.
 *     <p>A {@linkplain CallAdapter.Factory call adapter} which creates Java 8 futures.
 *     <p>Adding this class to {@link Retrofit} allows you to return {@link CompletableFuture} from
 *     service methods.
 *     <pre><code>
 * interface MyService {
 *   &#64;GET("user/me")
 *   CompletableFuture&lt;User&gt; getUser()
 * }
 * </code></pre>
 *     There are two configurations supported for the {@code CompletableFuture} type parameter:
 *     <ul>
 *       <li>Direct body (e.g., {@code CompletableFuture<User>}) returns the deserialized body for
 *           2XX responses, sets {@link retrofit2.HttpException HttpException} errors for non-2XX
 *           responses, and sets {@link IOException} for network errors.
 *       <li>Response wrapped body (e.g., {@code CompletableFuture<Response<User>>}) returns a
 *           {@link Response} object for all HTTP responses and sets {@link IOException} for network
 *           errors
 *     </ul>
 */
@Deprecated
public final class Java8CallAdapterFactory extends CallAdapter.Factory {
  public static Java8CallAdapterFactory create() {
    return new Java8CallAdapterFactory();
  }

  private Java8CallAdapterFactory() {}

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
      final CompletableFuture<R> future =
          new CompletableFuture<R>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
              if (mayInterruptIfRunning) {
                call.cancel();
              }
              return super.cancel(mayInterruptIfRunning);
            }
          };

      call.enqueue(
          new Callback<R>() {
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
          });

      return future;
    }
  }

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
      final CompletableFuture<Response<R>> future =
          new CompletableFuture<Response<R>>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
              if (mayInterruptIfRunning) {
                call.cancel();
              }
              return super.cancel(mayInterruptIfRunning);
            }
          };

      call.enqueue(
          new Callback<R>() {
            @Override
            public void onResponse(Call<R> call, Response<R> response) {
              future.complete(response);
            }

            @Override
            public void onFailure(Call<R> call, Throwable t) {
              future.completeExceptionally(t);
            }
          });

      return future;
    }
  }
}
