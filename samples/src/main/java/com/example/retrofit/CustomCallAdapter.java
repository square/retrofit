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
import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import retrofit.Call;
import retrofit.CallAdapter;
import retrofit.Callback;
import retrofit.Response;
import retrofit.RestAdapter;
import retrofit.http.GET;

/**
 * A sample showing a custom {@link CallAdapter} which adapts Guava's {@link ListenableFuture} as
 * a service method return type.
 */
public final class CustomCallAdapter {
  public static class ListenableFutureCallAdapterFactory implements CallAdapter.Factory {
    @Override public CallAdapter<?> get(Type returnType) {
      TypeToken<?> token = TypeToken.of(returnType);
      if (token.getRawType() != ListenableFuture.class) {
        return null;
      }

      TypeToken<?> componentType = token.getComponentType();
      if (componentType == null) {
        throw new IllegalStateException(); // TODO
      }
      final Type responseType = componentType.getType();

      return new CallAdapter<Object>() {
        @Override public Type responseType() {
          return responseType;
        }

        @Override public ListenableFuture<?> adapt(Call<Object> call) {
          CallFuture<Object> future = new CallFuture<>(call);
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

      @Override public void success(Response<T> response) {
        if (response.isSuccess()) {
          set(response.body());
        } else {
          setException(new IOException()); // TODO something more useful.
        }
      }

      @Override public void failure(Throwable t) {
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
    RestAdapter restAdapter = RestAdapter.builder("http://httpbin.org")
        .callAdapterFactory(new ListenableFutureCallAdapterFactory())
        .build();

    HttpBinService service = restAdapter.create(HttpBinService.class);
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
