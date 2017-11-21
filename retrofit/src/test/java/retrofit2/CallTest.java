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
package retrofit2;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.helpers.ToStringConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

import static java.util.concurrent.TimeUnit.SECONDS;
import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY;
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
    @POST("/{a}") Call<String> postRequestBody(@Path("a") Object a);
  }

  @Test public void http200Sync() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    Response<String> response = example.getString().execute();
    assertThat(response.isSuccessful()).isTrue();
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
    final CountDownLatch latch = new CountDownLatch(1);
    example.getString().enqueue(new Callback<String>() {
      @Override public void onResponse(Call<String> call, Response<String> response) {
        responseRef.set(response);
        latch.countDown();
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
        t.printStackTrace();
      }
    });
    assertTrue(latch.await(10, SECONDS));

    Response<String> response = responseRef.get();
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void http404Sync() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    Response<String> response = example.getString().execute();
    assertThat(response.isSuccessful()).isFalse();
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
      @Override public void onResponse(Call<String> call, Response<String> response) {
        responseRef.set(response);
        latch.countDown();
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
        t.printStackTrace();
      }
    });
    assertTrue(latch.await(10, SECONDS));

    Response<String> response = responseRef.get();
    assertThat(response.isSuccessful()).isFalse();
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
      @Override public void onResponse(Call<String> call, Response<String> response) {
        throw new AssertionError();
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
        failureRef.set(t);
        latch.countDown();
      }
    });
    assertTrue(latch.await(10, SECONDS));

    Throwable failure = failureRef.get();
    assertThat(failure).isInstanceOf(IOException.class);
  }

  @Test public void conversionProblemOutgoingSync() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory() {
          @Override
          public Converter<?, RequestBody> requestBodyConverter(Type type,
              Annotation[] parameterAnnotations, Annotation[] methodAnnotations,
              Retrofit retrofit) {
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
          public Converter<?, RequestBody> requestBodyConverter(Type type,
              Annotation[] parameterAnnotations, Annotation[] methodAnnotations,
              Retrofit retrofit) {
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
      @Override public void onResponse(Call<String> call, Response<String> response) {
        throw new AssertionError();
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
        failureRef.set(t);
        latch.countDown();
      }
    });
    assertTrue(latch.await(10, SECONDS));

    assertThat(failureRef.get()).isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("I am broken!");
  }

  @Test public void conversionProblemIncomingSync() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory() {
          @Override
          public Converter<ResponseBody, ?> responseBodyConverter(Type type,
              Annotation[] annotations, Retrofit retrofit) {
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
    OkHttpClient client = new OkHttpClient.Builder() //
        .addInterceptor(new Interceptor() {
          @Override public okhttp3.Response intercept(Chain chain) throws IOException {
            okhttp3.Response response = chain.proceed(chain.request());
            ResponseBody body = response.body();
            BufferedSource source = Okio.buffer(new ForwardingSource(body.source()) {
              @Override public long read(Buffer sink, long byteCount) throws IOException {
                throw new IOException("cause");
              }
            });
            body = ResponseBody.create(body.contentType(), body.contentLength(), source);
            return response.newBuilder().body(body).build();
          }
        }).build();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(client)
        .addConverterFactory(new ToStringConverterFactory() {
          @Override
          public Converter<ResponseBody, ?> responseBodyConverter(Type type,
              Annotation[] annotations, Retrofit retrofit) {
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
          public Converter<ResponseBody, ?> responseBodyConverter(Type type,
              Annotation[] annotations, Retrofit retrofit) {
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
      @Override public void onResponse(Call<String> call, Response<String> response) {
        throw new AssertionError();
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
        failureRef.set(t);
        latch.countDown();
      }
    });
    assertTrue(latch.await(10, SECONDS));

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
          public Converter<ResponseBody, ?> responseBodyConverter(Type type,
              Annotation[] annotations, Retrofit retrofit) {
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
          public Converter<ResponseBody, ?> responseBodyConverter(Type type,
              Annotation[] annotations, Retrofit retrofit) {
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

  @Test public void executeCallOnce() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);
    server.enqueue(new MockResponse());
    Call<String> call = example.getString();
    call.execute();
    try {
      call.execute();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Already executed.");
    }
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

  @Test public void reportsExecutedSync() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    Call<String> call = example.getString();
    assertThat(call.isExecuted()).isFalse();

    call.execute();
    assertThat(call.isExecuted()).isTrue();
  }

  @Test public void reportsExecutedAsync() throws InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    Call<String> call = example.getString();
    assertThat(call.isExecuted()).isFalse();

    call.enqueue(new Callback<String>() {
      @Override public void onResponse(Call<String> call, Response<String> response) {}
      @Override public void onFailure(Call<String> call, Throwable t) {}
    });
    assertThat(call.isExecuted()).isTrue();
  }

  @Test public void cancelBeforeExecute() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);
    Call<String> call = service.getString();

    call.cancel();
    assertThat(call.isCanceled()).isTrue();

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
    assertThat(call.isCanceled()).isTrue();

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<String>() {
      @Override public void onResponse(Call<String> call, Response<String> response) {
        throw new AssertionError();
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
        failureRef.set(t);
        latch.countDown();
      }
    });
    assertTrue(latch.await(10, SECONDS));
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
      @Override public void onResponse(Call<String> call, Response<String> response) {
        throw new AssertionError();
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
        failureRef.set(t);
        latch.countDown();
      }
    });

    call.cancel();
    assertThat(call.isCanceled()).isTrue();

    assertTrue(latch.await(10, SECONDS));
    assertThat(failureRef.get()).isInstanceOf(IOException.class).hasMessage("Canceled");
  }

  @Test public void cancelOkHttpRequest() throws InterruptedException {
    OkHttpClient client = new OkHttpClient();
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(client)
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

    Call<String> call = service.getString();

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<String>() {
      @Override public void onResponse(Call<String> call, Response<String> response) {
        throw new AssertionError();
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
        failureRef.set(t);
        latch.countDown();
      }
    });

    // Cancel the underlying HTTP Call. Should be reflected accurately back in the Retrofit Call.
    client.dispatcher().cancelAll();
    assertThat(call.isCanceled()).isTrue();

    assertTrue(latch.await(10, SECONDS));
    assertThat(failureRef.get()).isInstanceOf(IOException.class).hasMessage("Canceled");
  }

  @Test public void requestBeforeExecuteCreates() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    final AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        return "Hello";
      }
    };
    Call<String> call = service.postRequestBody(a);

    call.request();
    assertThat(writeCount.get()).isEqualTo(1);

    call.execute();
    assertThat(writeCount.get()).isEqualTo(1);
  }

  @Test public void requestThrowingBeforeExecuteFailsExecute() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    final AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        throw new RuntimeException("Broken!");
      }
    };
    Call<String> call = service.postRequestBody(a);

    try {
      call.request();
      fail();
    } catch (RuntimeException e) {
      assertThat(e).hasMessage("Broken!");
    }
    assertThat(writeCount.get()).isEqualTo(1);

    try {
      call.execute();
      fail();
    } catch (RuntimeException e) {
      assertThat(e).hasMessage("Broken!");
    }
    assertThat(writeCount.get()).isEqualTo(1);
  }

  @Test public void requestThrowingRecoverableErrorBeforeExecuteFailsExecute() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    final AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        throw new AssertionError("Broken!");
      }
    };
    Call<String> call = service.postRequestBody(a);

    try {
      call.request();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected).hasMessage("Broken!");
    }
    assertThat(writeCount.get()).isEqualTo(1);

    try {
      call.execute();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected).hasMessage("Broken!");
    }
    assertThat(writeCount.get()).isEqualTo(1);
  }

  @Test public void requestAfterExecuteReturnsCachedValue() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    final AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        return "Hello";
      }
    };
    Call<String> call = service.postRequestBody(a);

    call.execute();
    assertThat(writeCount.get()).isEqualTo(1);

    call.request();
    assertThat(writeCount.get()).isEqualTo(1);
  }

  @Test public void requestAfterExecuteThrowingAlsoThrows() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    final AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        throw new RuntimeException("Broken!");
      }
    };
    Call<String> call = service.postRequestBody(a);

    try {
      call.execute();
      fail();
    } catch (RuntimeException e) {
      assertThat(e).hasMessage("Broken!");
    }
    assertThat(writeCount.get()).isEqualTo(1);

    try {
      call.request();
      fail();
    } catch (RuntimeException e) {
      assertThat(e).hasMessage("Broken!");
    }
    assertThat(writeCount.get()).isEqualTo(1);
  }

  @Test public void requestAfterExecuteThrowingAlsoThrowsForRecoverableErrors() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    final AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        throw new AssertionError("Broken!");
      }
    };
    Call<String> call = service.postRequestBody(a);

    try {
      call.execute();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected).hasMessage("Broken!");
    }
    assertThat(writeCount.get()).isEqualTo(1);

    try {
      call.request();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected).hasMessage("Broken!");
    }
    assertThat(writeCount.get()).isEqualTo(1);
  }

  @Test public void requestBeforeEnqueueCreates() throws IOException, InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    final AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        return "Hello";
      }
    };
    Call<String> call = service.postRequestBody(a);

    call.request();
    assertThat(writeCount.get()).isEqualTo(1);

    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<String>() {
      @Override public void onResponse(Call<String> call, Response<String> response) {
        assertThat(writeCount.get()).isEqualTo(1);
        latch.countDown();
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
      }
    });
    assertTrue(latch.await(10, SECONDS));
  }

  @Test public void requestThrowingBeforeEnqueueFailsEnqueue()
      throws IOException, InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    final AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        throw new RuntimeException("Broken!");
      }
    };
    Call<String> call = service.postRequestBody(a);

    try {
      call.request();
      fail();
    } catch (RuntimeException e) {
      assertThat(e).hasMessage("Broken!");
    }
    assertThat(writeCount.get()).isEqualTo(1);

    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<String>() {
      @Override public void onResponse(Call<String> call, Response<String> response) {
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
        assertThat(t).isExactlyInstanceOf(RuntimeException.class).hasMessage("Broken!");
        assertThat(writeCount.get()).isEqualTo(1);
        latch.countDown();
      }
    });
    assertTrue(latch.await(10, SECONDS));
  }

  @Test public void requestThrowingRecoverableErrorBeforeEnqueueFailsEnqueue()
      throws IOException, InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    final AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        throw new AssertionError("Broken!");
      }
    };
    Call<String> call = service.postRequestBody(a);

    try {
      call.request();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected).hasMessage("Broken!");
    }
    assertThat(writeCount.get()).isEqualTo(1);

    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<String>() {
      @Override public void onResponse(Call<String> call, Response<String> response) {
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
        assertThat(t).isExactlyInstanceOf(AssertionError.class).hasMessage("Broken!");
        assertThat(writeCount.get()).isEqualTo(1);
        latch.countDown();
      }
    });
    assertTrue(latch.await(10, SECONDS));
  }

  @Test public void requestAfterEnqueueReturnsCachedValue() throws IOException,
      InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    final AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        return "Hello";
      }
    };
    Call<String> call = service.postRequestBody(a);

    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<String>() {
      @Override public void onResponse(Call<String> call, Response<String> response) {
        assertThat(writeCount.get()).isEqualTo(1);
        latch.countDown();
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
      }
    });
    assertTrue(latch.await(10, SECONDS));

    call.request();
    assertThat(writeCount.get()).isEqualTo(1);
  }

  @Test public void requestAfterEnqueueFailingThrows() throws IOException,
      InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    final AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        throw new RuntimeException("Broken!");
      }
    };
    Call<String> call = service.postRequestBody(a);

    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<String>() {
      @Override public void onResponse(Call<String> call, Response<String> response) {
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
        assertThat(t).isExactlyInstanceOf(RuntimeException.class).hasMessage("Broken!");
        assertThat(writeCount.get()).isEqualTo(1);
        latch.countDown();
      }
    });
    assertTrue(latch.await(10, SECONDS));

    try {
      call.request();
      fail();
    } catch (RuntimeException e) {
      assertThat(e).hasMessage("Broken!");
    }
    assertThat(writeCount.get()).isEqualTo(1);
  }

  @Test public void requestAfterEnqueueFailingThrowsForRecoverableErrors() throws IOException,
      InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    final AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        throw new AssertionError("Broken!");
      }
    };
    Call<String> call = service.postRequestBody(a);

    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<String>() {
      @Override public void onResponse(Call<String> call, Response<String> response) {
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
        assertThat(t).isExactlyInstanceOf(AssertionError.class).hasMessage("Broken!");
        assertThat(writeCount.get()).isEqualTo(1);
        latch.countDown();
      }
    });
    assertTrue(latch.await(10, SECONDS));

    try {
      call.request();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected).hasMessage("Broken!");
    }
    assertThat(writeCount.get()).isEqualTo(1);
  }

  @Test public void unrecoverableErrorsAreNotCaughtForCallback() throws Exception {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    final AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        throw new OutOfMemoryError("Broken!");
      }
    };
    Call<String> call = service.postRequestBody(a);

    try {
      call.enqueue(new Callback<String>() {
        @Override public void onResponse(Call<String> call, Response<String> response) {
        }

        @Override public void onFailure(Call<String> call, Throwable t) {
        }
      });
      fail();
    } catch (OutOfMemoryError expected) {
      assertThat(expected).hasMessage("Broken!");
    }

    try {
      call.request();
      fail();
    } catch (OutOfMemoryError expected) {
      assertThat(expected).hasMessage("Broken!");
    }
    assertThat(writeCount.get()).isEqualTo(2);
  }
}
