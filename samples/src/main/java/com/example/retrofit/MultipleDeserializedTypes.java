/*
 * Copyright (C) 2017 Square, Inc.
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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.Executor;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.GET;

/**
 * An example {@link CallAdapter} for returning different deserialized values for 202 responses than
 * other successful responses.
 */
final class MultipleDeserializedTypes {
  public interface Call<R, T> extends Cloneable {
    final class Response<R, T> {
      /**
       * Null iff the response is 202.
       */
      public final retrofit2.Response<R> response;

      /**
       * Not null iff the response is 202.
       */
      public final retrofit2.Response<T> accepted;

      Response(retrofit2.Response<R> response, retrofit2.Response<T> accepted) {
        this.response = response;
        this.accepted = accepted;
      }
    }

    interface Callback<R, T> {
      void onResponse(Call<R, T> call, retrofit2.Response<R> response);

      void onAccepted(Call<R, T> call, retrofit2.Response<T> response);

      void onFailure(Call<R, T> call, Throwable t);
    }

    Response<R, T> execute() throws IOException;

    void enqueue(Callback<R, T> callback);

    boolean isExecuted();

    void cancel();

    boolean isCanceled();

    Call<R, T> clone();

    Request request();
  }

  public static final class AcceptedResponseDeserializingCallAdapter
      implements CallAdapter<ResponseBody, Call<?, ?>> {
    public static final Factory FACTORY = new Factory() {
      @Override
      public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        if (getRawType(returnType) != Call.class) {
          return null;
        }
        ParameterizedType type = (ParameterizedType) returnType;
        Converter<ResponseBody, ?> responseConverter =
            retrofit.responseBodyConverter(getParameterUpperBound(0, type), annotations);
        Converter<ResponseBody, ?> acceptedConverter =
            retrofit.responseBodyConverter(getParameterUpperBound(1, type), annotations);
        Executor executor = retrofit.callbackExecutor();
        return new AcceptedResponseDeserializingCallAdapter(responseConverter, acceptedConverter,
            executor);
      }
    };

    private final Converter<ResponseBody, ?> responseConverter;
    private final Converter<ResponseBody, ?> acceptedConverter;
    private final Executor executor;

    AcceptedResponseDeserializingCallAdapter(Converter<ResponseBody, ?> responseConverter,
        Converter<ResponseBody, ?> acceptedConverter, Executor executor) {
      this.responseConverter = responseConverter;
      this.acceptedConverter = acceptedConverter;
      this.executor = executor;
    }

    @Override public Type responseType() {
      return ResponseBody.class;
    }

    @Override public Call<?, ?> adapt(retrofit2.Call<ResponseBody> call) {
      return new RealCall<>(call, responseConverter, acceptedConverter, executor);
    }

    private static final class RealCall<R, T> implements Call<R, T> {
      private final retrofit2.Call<ResponseBody> call;
      final Converter<ResponseBody, R> responseConverter;
      final Converter<ResponseBody, T> acceptedConverter;
      final Executor executor;

      RealCall(retrofit2.Call<ResponseBody> call, Converter<ResponseBody, R> responseConverter,
          Converter<ResponseBody, T> acceptedConverter, Executor executor) {
        this.call = call;
        this.responseConverter = responseConverter;
        this.acceptedConverter = acceptedConverter;
        this.executor = executor;
      }

      @Override public Response<R, T> execute() throws IOException {
        retrofit2.Response<ResponseBody> response = call.execute();
        okhttp3.Response raw = response.raw();
        if (!response.isSuccessful()) {
          return new Response<>(retrofit2.Response.<R>error(raw.body(), raw), null);
        }
        if (response.code() == 202) {
          return new Response<>(null,
              retrofit2.Response.success(acceptedConverter.convert(response.body()), raw));
        }
        return new Response<>(
            retrofit2.Response.success(responseConverter.convert(response.body()), raw), null);
      }

      @Override public void enqueue(final Callback<R, T> callback) {
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
          @Override public void onResponse(retrofit2.Call<ResponseBody> call,
              final retrofit2.Response<ResponseBody> response) {
            if (executor == null) {
              callSuccess(response);
            } else {
              executor.execute(new Runnable() {
                @Override public void run() {
                  if (isCanceled()) {
                    // Emulate OkHttp's behavior of throwing/delivering
                    // an IOException on cancellation.
                    callback.onFailure(RealCall.this, new IOException("Canceled"));
                    return;
                  }
                  callSuccess(response);
                }
              });
            }
          }

          @Override public void onFailure(retrofit2.Call<ResponseBody> call, final Throwable t) {
            if (executor == null) {
              callFailure(t);
            } else {
              executor.execute(new Runnable() {
                @Override public void run() {
                  callFailure(t);
                }
              });
            }
          }

          void callSuccess(retrofit2.Response<ResponseBody> response) {
            okhttp3.Response raw = response.raw();
            if (!response.isSuccessful()) {
              callback.onResponse(RealCall.this, retrofit2.Response.<R>error(raw.body(), raw));
              return;
            }
            if (response.code() == 202) {
              try {
                T accepted = acceptedConverter.convert(response.body());
                callback.onAccepted(RealCall.this, retrofit2.Response.success(accepted, raw));
              } catch (Throwable e) {
                callback.onFailure(RealCall.this, e);
              }
              return;
            }
            try {
              R converted = responseConverter.convert(response.body());
              callback.onResponse(RealCall.this, retrofit2.Response.success(converted, raw));
            } catch (Throwable e) {
              callback.onFailure(RealCall.this, e);
            }
          }

          void callFailure(Throwable t) {
            callback.onFailure(RealCall.this, t);
          }
        });
      }

      @Override public boolean isExecuted() {
        return call.isExecuted();
      }

      @Override public void cancel() {
        call.cancel();
      }

      @Override public boolean isCanceled() {
        return call.isCanceled();
      }

      @Override public Call<R, T> clone() {
        return new RealCall<>(call.clone(), responseConverter, acceptedConverter, executor);
      }

      @Override public Request request() {
        return call.request();
      }
    }
  }

  public static final class ResponseBodyNormal {
    final long id;
    final String greeting;

    private ResponseBodyNormal(long id, String greeting) {
      this.id = id;
      this.greeting = greeting;
    }

    @Override public String toString() {
      return "ResponseBodyNormal{" + "id=" + id + ", greeting='" + greeting + '\'' + '}';
    }
  }

  public static final class ResponseBody202 {
    final int code;
    final List<String> actions;

    private ResponseBody202(int code, List<String> actions) {
      this.code = code;
      this.actions = actions;
    }

    @Override public String toString() {
      return "ResponseBody202{" + "code=" + code + ", actions=" + actions + '}';
    }
  }

  public interface Service {
    @GET("/") Call<ResponseBodyNormal, ResponseBody202> greet();
  }

  public static void main(String[] args) throws IOException {
    MockWebServer server = new MockWebServer();
    server.start();
    server.enqueue(new MockResponse().setBody("{\"id\": 842945105309, \"greeting\":\"Hello!\"}"));
    server.enqueue(new MockResponse().setBody("{\"code\": -1, \"actions\": [\"/call\", \"/mail\"]}")
        .setResponseCode(202));

    Retrofit retrofit = new Retrofit.Builder().baseUrl(server.url("/"))
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(AcceptedResponseDeserializingCallAdapter.FACTORY)
        .build();
    Service service = retrofit.create(Service.class);

    System.out.println(service.greet().execute().response.body());
    System.out.println(service.greet().execute().accepted.body());

    server.shutdown();
  }
}
