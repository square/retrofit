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
package com.example.retrofit;

import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

/**
 * A sample showing a custom {@link CallAdapter} which adapts Guava's {@link ListenableFuture} as
 * a service method return type.
 */
public final class CustomCallAdapter {
  public static class ListenableFutureCallAdapterFactory implements CallAdapter.Factory {
    @Override public CallAdapter<ListenableFuture<?>> get(Type returnType, Annotation[] annotations,
        Retrofit retrofit) {
      TypeToken<?> token = TypeToken.of(returnType);
      if (token.getRawType() != ListenableFuture.class) {
        return null;
      }
      if (!(returnType instanceof ParameterizedType)) {
        throw new IllegalStateException(
            "ListenableFuture must have generic type (e.g., ListenableFuture<ResponseBody>)");
      }
      final Type responseType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
      return new CallAdapter<ListenableFuture<?>>() {
        @Override public Type responseType() {
          return responseType;
        }

        @Override public <R> ListenableFuture<R> adapt(Call<R> call) {
          CallFuture<R> future = new CallFuture<>(call);
          call.enqueue(future);
          return future;
        }
      };
    }

    private static final class CallFuture<T> extends AbstractFuture<T> implements Callback<T> {
      private final Call<T> call;

      private CallFuture(Call<T> call) {
        this.call = call;
      }

      @Override protected void interruptTask() {
        call.cancel();
      }

      @Override public void onResponse(Response<T> response) {
        if (response.isSuccess()) {
          set(response.body());
        } else {
          setException(new IOException()); // TODO something more useful.
        }
      }

      @Override public void onFailure(Throwable t) {
        setException(t);
      }
    }
  }

  interface HttpBinService {
    @GET("/ip")
    ListenableFuture<Ip> getIp();
  }

  static class Ip {
    String origin;
  }

  public static void main(String... args) {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://httpbin.org")
        .addCallAdapterFactory(new ListenableFutureCallAdapterFactory())
        .addConverterFactory(GsonConverterFactory.create())
        .build();

    HttpBinService service = retrofit.create(HttpBinService.class);
    final ListenableFuture<Ip> ip = service.getIp();
    ip.addListener(new Runnable() {
      @Override public void run() {
        try {
          System.out.println("IP: " + ip.get().origin);
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      }
    }, Executors.newSingleThreadExecutor());
  }
}
