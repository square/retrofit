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
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

/**
 * A sample showing a custom {@link CallAdapter} which adapts the built-in {@link Call} to a custom
 * version whose callback has more granular methods.
 */
public final class ErrorHandlingCallAdapter {
  /** A callback which offers granular callbacks for various conditions. */
  interface MyCallback<T> {
    /** Called for [200, 300) responses. */
    void success(Response<T> response);
    /** Called for 401 responses. */
    void unauthenticated(Response<?> response);
    /** Called for [400, 500) responses, except 401. */
    void clientError(Response<?> response);
    /** Called for [500, 600) response. */
    void serverError(Response<?> response);
    /** Called for network errors while making the call. */
    void networkError(IOException e);
    /** Called for unexpected errors while making the call. */
    void unexpectedError(Throwable t);
  }

  interface MyCall<T> {
    void cancel();
    void enqueue(MyCallback<T> callback);
    MyCall<T> clone();

    // Left as an exercise for the reader...
    // TODO MyResponse<T> execute() throws MyHttpException;
  }

  public static class ErrorHandlingCallAdapterFactory implements CallAdapter.Factory {
    @Override public CallAdapter<MyCall<?>> get(Type returnType, Annotation[] annotations,
        Retrofit retrofit) {
      TypeToken<?> token = TypeToken.of(returnType);
      if (token.getRawType() != MyCall.class) {
        return null;
      }
      if (!(returnType instanceof ParameterizedType)) {
        throw new IllegalStateException(
            "MyCall must have generic type (e.g., MyCall<ResponseBody>)");
      }
      final Type responseType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
      return new CallAdapter<MyCall<?>>() {
        @Override public Type responseType() {
          return responseType;
        }

        @Override public <R> MyCall<R> adapt(Call<R> call) {
          return new MyCallAdapter<>(call);
        }
      };
    }
  }

  /** Adapts a {@link Call} to {@link MyCall}. */
  static class MyCallAdapter<T> implements MyCall<T> {
    private final Call<T> call;

    MyCallAdapter(Call<T> call) {
      this.call = call;
    }

    @Override public void cancel() {
      call.cancel();
    }

    @Override public void enqueue(final MyCallback<T> callback) {
      call.enqueue(new Callback<T>() {
        @Override public void onResponse(Response<T> response) {
          int code = response.code();
          if (code >= 200 && code < 300) {
            callback.success(response);
          } else if (code == 401) {
            callback.unauthenticated(response);
          } else if (code >= 400 && code < 500) {
            callback.clientError(response);
          } else if (code >= 500 && code < 600) {
            callback.serverError(response);
          } else {
            callback.unexpectedError(new RuntimeException("Unexpected response " + response));
          }
        }

        @Override public void onFailure(Throwable t) {
          if (t instanceof IOException) {
            callback.networkError((IOException) t);
          } else {
            callback.unexpectedError(t);
          }
        }
      });
    }

    @Override public MyCall<T> clone() {
      return new MyCallAdapter<>(call.clone());
    }
  }

  interface HttpBinService {
    @GET("/ip")
    MyCall<Ip> getIp();
  }

  static class Ip {
    String origin;
  }

  public static void main(String... args) {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://httpbin.org")
        .addCallAdapterFactory(new ErrorHandlingCallAdapterFactory())
        .addConverterFactory(GsonConverterFactory.create())
        .build();

    HttpBinService service = retrofit.create(HttpBinService.class);
    MyCall<Ip> ip = service.getIp();
    ip.enqueue(new MyCallback<Ip>() {
      @Override public void success(Response<Ip> response) {
        System.out.println("SUCCESS! " + response.body().origin);
      }

      @Override public void unauthenticated(Response<?> response) {
        System.out.println("UNAUTHENTICATED");
      }

      @Override public void clientError(Response<?> response) {
        System.out.println("CLIENT ERROR " + response.code() + " " + response.message());
      }

      @Override public void serverError(Response<?> response) {
        System.out.println("SERVER ERROR " + response.code() + " " + response.message());
      }

      @Override public void networkError(IOException e) {
        System.err.println("NETOWRK ERROR " + e.getMessage());
      }

      @Override public void unexpectedError(Throwable t) {
        System.err.println("FATAL ERROR " + t.getMessage());
      }
    });
  }
}
