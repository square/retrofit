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
package retrofit2.adapter.guava;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * A {@linkplain CallAdapter.Factory call adapter} which creates Guava futures.
 *
 * <p>Adding this class to {@link Retrofit} allows you to return {@link ListenableFuture} from
 * service methods.
 *
 * <pre><code>
 * interface MyService {
 *   &#64;GET("user/me")
 *   ListenableFuture&lt;User&gt; getUser()
 * }
 * </code></pre>
 *
 * There are two configurations supported for the {@code ListenableFuture} type parameter:
 *
 * <ul>
 *   <li>Direct body (e.g., {@code ListenableFuture<User>}) returns the deserialized body for 2XX
 *       responses, sets {@link retrofit2.HttpException HttpException} errors for non-2XX responses,
 *       and sets {@link IOException} for network errors.
 *   <li>Response wrapped body (e.g., {@code ListenableFuture<Response<User>>}) returns a {@link
 *       Response} object for all HTTP responses and sets {@link IOException} for network errors
 * </ul>
 */
public final class GuavaCallAdapterFactory extends CallAdapter.Factory {
  public static GuavaCallAdapterFactory create() {
    return new GuavaCallAdapterFactory();
  }

  private GuavaCallAdapterFactory() {}

  @Override
  public @Nullable CallAdapter<?, ?> get(
      Type returnType, Annotation[] annotations, Retrofit retrofit) {
    if (getRawType(returnType) != ListenableFuture.class) {
      return null;
    }
    if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalStateException(
          "ListenableFuture return type must be parameterized"
              + " as ListenableFuture<Foo> or ListenableFuture<? extends Foo>");
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

  private static final class BodyCallAdapter<R> implements CallAdapter<R, ListenableFuture<R>> {
    private final Type responseType;

    BodyCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override
    public Type responseType() {
      return responseType;
    }

    @Override
    public ListenableFuture<R> adapt(final Call<R> call) {
      CallCancelListenableFuture<R> future = new CallCancelListenableFuture<>(call);

      call.enqueue(
          new Callback<R>() {
            @Override
            public void onResponse(Call<R> call, Response<R> response) {
              if (response.isSuccessful()) {
                future.set(response.body());
              } else {
                future.setException(new HttpException(response));
              }
            }

            @Override
            public void onFailure(Call<R> call, Throwable t) {
              future.setException(t);
            }
          });

      return future;
    }
  }

  private static final class ResponseCallAdapter<R>
      implements CallAdapter<R, ListenableFuture<Response<R>>> {
    private final Type responseType;

    ResponseCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override
    public Type responseType() {
      return responseType;
    }

    @Override
    public ListenableFuture<Response<R>> adapt(final Call<R> call) {
      CallCancelListenableFuture<Response<R>> future = new CallCancelListenableFuture<>(call);

      call.enqueue(
          new Callback<R>() {
            @Override
            public void onResponse(Call<R> call, Response<R> response) {
              future.set(response);
            }

            @Override
            public void onFailure(Call<R> call, Throwable t) {
              future.setException(t);
            }
          });

      return future;
    }
  }

  private static final class CallCancelListenableFuture<T> extends AbstractFuture<T> {
    private final Call<?> call;

    CallCancelListenableFuture(Call<?> call) {
      this.call = call;
    }

    @Override
    public boolean set(@org.checkerframework.checker.nullness.qual.Nullable T value) {
      return super.set(value);
    }

    @Override
    public boolean setException(Throwable throwable) {
      return super.setException(throwable);
    }

    @Override
    protected void interruptTask() {
      call.cancel();
    }
  }
}
