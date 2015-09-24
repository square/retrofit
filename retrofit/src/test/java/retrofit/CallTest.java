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

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.SocketPolicy;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import org.junit.Rule;
import org.junit.Test;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Streaming;

import static com.squareup.okhttp.mockwebserver.SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public final class CallTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/") Call<String> getString();
    @GET("/") Call<ResponseBody> getBody();
    @GET("/") @Streaming Call<ResponseBody> getStreamingBody();
    @POST("/") Call<String> postString(@Body String body);
  }

  @Test public void http200Sync() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    Response<String> response = example.getString().execute();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void http200Async() throws InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    final AtomicReference<Response<String>> responseRef = new AtomicReference<>();
    final AtomicReference<Retrofit> retrofitRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.getString().enqueue(new Callback<String>() {
      @Override public void onResponse(Response<String> response, Retrofit retrofit) {
        responseRef.set(response);
        retrofitRef.set(retrofit);
        latch.countDown();
      }

      @Override public void onFailure(Throwable t) {
        t.printStackTrace();
      }
    });
    assertTrue(latch.await(2, SECONDS));

    Response<String> response = responseRef.get();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");

    assertThat(retrofitRef.get()).isSameAs(retrofit);
  }

  @Test public void http404Sync() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    Response<String> response = example.getString().execute();
    assertThat(response.isSuccess()).isFalse();
    assertThat(response.code()).isEqualTo(404);
    assertThat(response.errorBody().string()).isEqualTo("Hi");
  }

  @Test public void http404Async() throws InterruptedException, IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    final AtomicReference<Response<String>> responseRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.getString().enqueue(new Callback<String>() {
      @Override public void onResponse(Response<String> response, Retrofit retrofit) {
        responseRef.set(response);
        latch.countDown();
      }

      @Override public void onFailure(Throwable t) {
        t.printStackTrace();
      }
    });
    assertTrue(latch.await(2, SECONDS));

    Response<String> response = responseRef.get();
    assertThat(response.isSuccess()).isFalse();
    assertThat(response.code()).isEqualTo(404);
    assertThat(response.errorBody().string()).isEqualTo("Hi");
  }

  @Test public void transportProblemSync() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

    Call<String> call = example.getString();
    try {
      call.execute();
      fail();
    } catch (IOException ignored) {
    }
  }

  @Test public void transportProblemAsync() throws InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.getString().enqueue(new Callback<String>() {
      @Override public void onResponse(Response<String> response, Retrofit retrofit) {
        throw new AssertionError();
      }

      @Override public void onFailure(Throwable t) {
        failureRef.set(t);
        latch.countDown();
      }
    });
    assertTrue(latch.await(2, SECONDS));

    Throwable failure = failureRef.get();
    assertThat(failure).isInstanceOf(IOException.class);
  }

  @Test public void conversionProblemOutgoingSync() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory() {
          @Override
          public Converter<?, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
            return new Converter<String, RequestBody>() {
              @Override public RequestBody convert(String value) throws IOException {
                throw new UnsupportedOperationException("I am broken!");
              }
            };
          }
        })
        .build();
    Service example = retrofit.create(Service.class);

    Call<String> call = example.postString("Hi");
    try {
      call.execute();
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("I am broken!");
    }
  }

  @Test public void conversionProblemOutgoingAsync() throws InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory() {
          @Override
          public Converter<?, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
            return new Converter<String, RequestBody>() {
              @Override public RequestBody convert(String value) throws IOException {
                throw new UnsupportedOperationException("I am broken!");
              }
            };
          }
        })
        .build();
    Service example = retrofit.create(Service.class);

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.postString("Hi").enqueue(new Callback<String>() {
      @Override public void onResponse(Response<String> response, Retrofit retrofit) {
        throw new AssertionError();
      }

      @Override public void onFailure(Throwable t) {
        failureRef.set(t);
        latch.countDown();
      }
    });
    assertTrue(latch.await(2, SECONDS));

    assertThat(failureRef.get()).isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("I am broken!");
  }

  @Test public void conversionProblemIncomingSync() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory() {
          @Override
          public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
            return new Converter<ResponseBody, String>() {
              @Override public String convert(ResponseBody value) throws IOException {
                throw new UnsupportedOperationException("I am broken!");
              }
            };
          }
        })
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    Call<String> call = example.postString("Hi");
    try {
      call.execute();
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("I am broken!");
    }
  }

  @Test public void conversionProblemIncomingMaskedByConverterIsUnwrapped() throws IOException {
    // MWS has no way to trigger IOExceptions during the response body so use an interceptor.
    OkHttpClient client = new OkHttpClient();
    client.interceptors().add(new Interceptor() {
      @Override public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
        com.squareup.okhttp.Response response = chain.proceed(chain.request());
        ResponseBody body = response.body();
        BufferedSource source = Okio.buffer(new ForwardingSource(body.source()) {
          @Override public long read(Buffer sink, long byteCount) throws IOException {
            throw new IOException("cause");
          }
        });
        body = ResponseBody.create(body.contentType(), body.contentLength(), source);
        return response.newBuilder().body(body).build();
      }
    });

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(client)
        .addConverterFactory(new ToStringConverterFactory() {
          @Override
          public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
            return new Converter<ResponseBody, String>() {
              @Override public String convert(ResponseBody value) throws IOException {
                try {
                  return value.string();
                } catch (IOException e) {
                  // Some serialization libraries mask transport problems in runtime exceptions. Bad!
                  throw new RuntimeException("wrapper", e);
                }
              }
            };
          }
        })
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    Call<String> call = example.getString();
    try {
      call.execute();
      fail();
    } catch (IOException e) {
      assertThat(e).hasMessage("cause");
    }
  }

  @Test public void conversionProblemIncomingAsync() throws InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory() {
          @Override
          public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
            return new Converter<ResponseBody, String>() {
              @Override public String convert(ResponseBody value) throws IOException {
                throw new UnsupportedOperationException("I am broken!");
              }
            };
          }
        })
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.postString("Hi").enqueue(new Callback<String>() {
      @Override public void onResponse(Response<String> response, Retrofit retrofit) {
        throw new AssertionError();
      }

      @Override public void onFailure(Throwable t) {
        failureRef.set(t);
        latch.countDown();
      }
    });
    assertTrue(latch.await(2, SECONDS));

    assertThat(failureRef.get()).isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("I am broken!");
  }

  @Test public void http204SkipsConverter() throws IOException {
    final Converter<ResponseBody, String> converter = spy(new Converter<ResponseBody, String>() {
      @Override public String convert(ResponseBody value) throws IOException {
        return value.string();
      }
    });
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory() {
          @Override
          public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
            return converter;
          }
        })
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setStatus("HTTP/1.1 204 Nothin"));

    Response<String> response = example.getString().execute();
    assertThat(response.code()).isEqualTo(204);
    assertThat(response.body()).isNull();
    verifyNoMoreInteractions(converter);
  }

  @Test public void http205SkipsConverter() throws IOException {
    final Converter<ResponseBody, String> converter = spy(new Converter<ResponseBody, String>() {
      @Override public String convert(ResponseBody value) throws IOException {
        return value.string();
      }
    });
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory() {
          @Override
          public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
            return converter;
          }
        })
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setStatus("HTTP/1.1 205 Nothin"));

    Response<String> response = example.getString().execute();
    assertThat(response.code()).isEqualTo(205);
    assertThat(response.body()).isNull();
    verifyNoMoreInteractions(converter);
  }

  @Test public void successfulRequestResponseWhenMimeTypeMissing() throws Exception {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi").removeHeader("Content-Type"));

    Response<String> response = example.getString().execute();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void responseBody() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("1234"));

    Response<ResponseBody> response = example.getBody().execute();
    assertThat(response.body().string()).isEqualTo("1234");
  }

  @Test public void responseBodyBuffers() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse()
        .setBody("1234")
        .setSocketPolicy(DISCONNECT_DURING_RESPONSE_BODY));

    Call<ResponseBody> buffered = example.getBody();
    // When buffering we will detect all socket problems before returning the Response.
    try {
      buffered.execute();
      fail();
    } catch (IOException e) {
      assertThat(e).hasMessage("unexpected end of stream");
    }
  }

  @Test public void responseBodyStreams() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse()
        .setBody("1234")
        .setSocketPolicy(DISCONNECT_DURING_RESPONSE_BODY));

    Response<ResponseBody> response = example.getStreamingBody().execute();

    ResponseBody streamedBody = response.body();
    // When streaming we only detect socket problems as the ResponseBody is read.
    try {
      streamedBody.string();
      fail();
    } catch (IOException e) {
      assertThat(e).hasMessage("unexpected end of stream");
    }
  }

  @Test public void rawResponseContentTypeAndLengthButNoSource() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi").addHeader("Content-Type", "text/greeting"));

    Response<String> response = example.getString().execute();
    assertThat(response.body()).isEqualTo("Hi");
    ResponseBody rawBody = response.raw().body();
    assertThat(rawBody.contentLength()).isEqualTo(2);
    assertThat(rawBody.contentType().toString()).isEqualTo("text/greeting");
    try {
      rawBody.source();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Cannot read raw response body of a converted body.");
    }
  }

  @Test public void emptyResponse() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("").addHeader("Content-Type", "text/stringy"));

    Response<String> response = example.getString().execute();
    assertThat(response.body()).isEqualTo("");
    ResponseBody rawBody = response.raw().body();
    assertThat(rawBody.contentLength()).isEqualTo(0);
    assertThat(rawBody.contentType().toString()).isEqualTo("text/stringy");
  }

  @Test public void cancelBeforeExecute() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);
    Call<String> call = service.getString();

    call.cancel();

    try {
      call.execute();
      fail();
    } catch (IOException e) {
      assertThat(e).hasMessage("Canceled");
    }
  }

  @Test public void cancelBeforeEnqueue() throws Exception {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);
    Call<String> call = service.getString();

    call.cancel();

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<String>() {
      @Override public void onResponse(Response<String> response, Retrofit retrofit) {
        throw new AssertionError();
      }

      @Override public void onFailure(Throwable t) {
        failureRef.set(t);
        latch.countDown();
      }
    });
    latch.await();
    assertThat(failureRef.get()).hasMessage("Canceled");
  }

  @Test public void cloningExecutedRequestDoesNotCopyState() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));
    server.enqueue(new MockResponse().setBody("Hello"));

    Call<String> call = service.getString();
    assertThat(call.execute().body()).isEqualTo("Hi");

    Call<String> cloned = call.clone();
    assertThat(cloned.execute().body()).isEqualTo("Hello");
  }

  @Test public void cancelRequest() throws InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

    Call<String> call = service.getString();

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<String>() {
      @Override public void onResponse(Response<String> response, Retrofit retrofit) {
        throw new AssertionError();
      }

      @Override public void onFailure(Throwable t) {
        failureRef.set(t);
        latch.countDown();
      }
    });

    call.cancel();

    assertTrue(latch.await(2, SECONDS));
    assertThat(failureRef.get()).isInstanceOf(IOException.class).hasMessage("Canceled");
  }
}
