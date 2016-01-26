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

import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public final class GuavaCallAdapterFactory implements CallAdapter.Factory {
  public static GuavaCallAdapterFactory create() {
    return new GuavaCallAdapterFactory();
  }

  private GuavaCallAdapterFactory() {
  }

  @Override
  public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
    if (TypeToken.of(returnType).getRawType() != ListenableFuture.class) {
      return null;
    }
    if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalStateException("ListenableFuture return type must be parameterized"
          + " as ListenableFuture<Foo> or ListenableFuture<? extends Foo>");
    }
    Type innerType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
    if (innerType instanceof WildcardType) {
      innerType = ((WildcardType) innerType).getUpperBounds()[0];
    }

    if (TypeToken.of(innerType).getRawType() != Response.class) {
      // Generic type is not Response<T>. Use it for body-only adapter.
      return new BodyCallAdapter(innerType);
    }

    // Generic type is Response<T>. Extract T and create the Response version of the adapter.
    if (!(innerType instanceof ParameterizedType)) {
      throw new IllegalStateException("Response must be parameterized"
          + " as Response<Foo> or Response<? extends Foo>");
    }
    Type responseType = ((ParameterizedType) innerType).getActualTypeArguments()[0];
    if (responseType instanceof WildcardType) {
      responseType = ((WildcardType) responseType).getUpperBounds()[0];
    }
    return new ResponseCallAdapter(responseType);
  }

  private static class BodyCallAdapter implements CallAdapter<ListenableFuture<?>> {
    private final Type responseType;

    BodyCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override public Type responseType() {
      return responseType;
    }

    @Override public <R> ListenableFuture<R> adapt(final Call<R> call) {
      return new AbstractFuture<R>() {
        {
          call.enqueue(new Callback<R>() {
            @Override public void onResponse(Call<R> call, Response<R> response) {
              if (response.isSuccess()) {
                set(response.body());
              } else {
                setException(new HttpException(response));
              }
            }

            @Override public void onFailure(Call<R> call, Throwable t) {
              setException(t);
            }
          });
        }

        @Override protected void interruptTask() {
          call.cancel();
        }
      };
    }
  }

  private static class ResponseCallAdapter implements CallAdapter<ListenableFuture<?>> {
    private final Type responseType;

    ResponseCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override public Type responseType() {
      return responseType;
    }

    @Override public <R> ListenableFuture<Response<R>> adapt(final Call<R> call) {
      return new AbstractFuture<Response<R>>() {
        {
          call.enqueue(new Callback<R>() {
            @Override public void onResponse(Call<R> call, Response<R> response) {
              set(response);
            }

            @Override public void onFailure(Call<R> call, Throwable t) {
              setException(t);
            }
          });
        }

        @Override protected void interruptTask() {
          call.cancel();
        }
      };
    }
  }
}
