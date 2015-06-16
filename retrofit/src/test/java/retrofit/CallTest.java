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
import com.squareup.okhttp.mockwebserver.SocketPolicy;
import com.squareup.okhttp.mockwebserver.rule.MockWebServerRule;
import java.io.IOException;
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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public final class CallTest {
  @Rule public final MockWebServerRule server = new MockWebServerRule();

  interface Service {
    @GET("/") Call<String> getString();
    @GET("/") Call<ResponseBody> getBody();
    @GET("/") @Streaming Call<ResponseBody> getStreamingBody();
    @POST("/") Call<String> postString(@Body String body);
  }

  @Test public void http200Sync() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    Response<String> response = example.getString().execute();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void http200Async() throws InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    final AtomicReference<Response<String>> responseRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.getString().enqueue(new Callback<String>() {
      @Override public void success(Response<String> response) {
        responseRef.set(response);
        latch.countDown();
      }

      @Override public void failure(Throwable t) {
        t.printStackTrace();
      }
    });
    assertTrue(latch.await(2, SECONDS));

    Response<String> response = responseRef.get();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void http404Sync() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter())
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
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    final AtomicReference<Response<String>> responseRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.getString().enqueue(new Callback<String>() {
      @Override public void success(Response<String> response) {
        responseRef.set(response);
        latch.countDown();
      }

      @Override public void failure(Throwable t) {
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
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter())
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
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.getString().enqueue(new Callback<String>() {
      @Override public void success(Response<String> response) {
        throw new AssertionError();
      }

      @Override public void failure(Throwable t) {
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
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter() {
          @Override public RequestBody toBody(Object object, Type type) {
            throw new UnsupportedOperationException("I am broken!");
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
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter() {
          @Override public RequestBody toBody(Object object, Type type) {
            throw new UnsupportedOperationException("I am broken!");
          }
        })
        .build();
    Service example = retrofit.create(Service.class);

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.postString("Hi").enqueue(new Callback<String>() {
      @Override public void success(Response<String> response) {
        throw new AssertionError();
      }

      @Override public void failure(Throwable t) {
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
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter() {
          @Override public Object fromBody(ResponseBody body, Type type) throws IOException {
            throw new UnsupportedOperationException("I am broken!");
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
        .endpoint(server.getUrl("/").toString())
        .client(client)
        .converter(new StringConverter() {
          @Override public Object fromBody(ResponseBody body, Type type) throws IOException {
            try {
              return super.fromBody(body, type);
            } catch (IOException e) {
              // Some serialization libraries mask transport problems in runtime exceptions. Bad!
              throw new RuntimeException("wrapper", e);
            }
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
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter() {
          @Override public Object fromBody(ResponseBody body, Type type) throws IOException {
            throw new UnsupportedOperationException("I am broken!");
          }
        })
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.postString("Hi").enqueue(new Callback<String>() {
      @Override public void success(Response<String> response) {
        throw new AssertionError();
      }

      @Override public void failure(Throwable t) {
        failureRef.set(t);
        latch.countDown();
      }
    });
    assertTrue(latch.await(2, SECONDS));

    assertThat(failureRef.get()).isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("I am broken!");
  }

  @Test public void http204SkipsConverter() throws IOException {
    Converter converter = spy(new StringConverter());
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(converter)
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setStatus("HTTP/1.1 204 Nothin"));

    Response<String> response = example.getString().execute();
    assertThat(response.code()).isEqualTo(204);
    assertThat(response.body()).isNull();
    verifyNoMoreInteractions(converter);
  }

  @Test public void http205SkipsConverter() throws IOException {
    Converter converter = spy(new StringConverter());
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(converter)
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
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi").removeHeader("Content-Type"));

    Response<String> response = example.getString().execute();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void responseBody() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("1234"));

    Response<ResponseBody> response = example.getBody().execute();
    assertThat(response.body().string()).isEqualTo("1234");
  }

  @Test public void responseBodyBuffers() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("1234").throttleBody(1, 500, MILLISECONDS));

    long exeuteStart = System.nanoTime();
    Response<ResponseBody> response = example.getBody().execute();
    long executeTook = System.nanoTime() - exeuteStart;
    assertThat(executeTook).isGreaterThan(MILLISECONDS.toNanos(1000));

    long readStart = System.nanoTime();
    String body = response.body().string();
    long readTook = System.nanoTime() - readStart;
    assertThat(readTook).isLessThan(MILLISECONDS.toNanos(500));
    assertThat(body).isEqualTo("1234");
  }

  @Test public void responseBodyStreams() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter())
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("1234").throttleBody(1, 500, MILLISECONDS));

    long exeuteStart = System.nanoTime();
    Response<ResponseBody> response = example.getStreamingBody().execute();
    long executeTook = System.nanoTime() - exeuteStart;
    assertThat(executeTook).isLessThan(MILLISECONDS.toNanos(500));

    long readStart = System.nanoTime();
    String body = response.body().string();
    long readTook = System.nanoTime() - readStart;
    assertThat(readTook).isGreaterThan(MILLISECONDS.toNanos(1000));
    assertThat(body).isEqualTo("1234");
  }

  @Test public void rawResponseContentTypeAndLengthButNoSource() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter())
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
}
