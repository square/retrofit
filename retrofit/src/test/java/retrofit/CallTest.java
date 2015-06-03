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

import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.SocketPolicy;
import com.squareup.okhttp.mockwebserver.rule.MockWebServerRule;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;
import retrofit.converter.Converter;
import retrofit.converter.GsonConverter;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public final class CallTest {
  @Rule public final MockWebServerRule server = new MockWebServerRule();

  interface Service {
    @GET("/") Call<String> getMethod();
    @POST("/") Call<String> postMethod(@Body Object body);
  }

  @Test public void http200Sync() throws IOException {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    Service example = ra.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    Response<String> response = example.getMethod().execute();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void http200Async() throws InterruptedException {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    Service example = ra.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    final AtomicReference<Response<String>> responseRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.getMethod().enqueue(new Callback<String>() {
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
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    Service example = ra.create(Service.class);

    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    Response<String> response = example.getMethod().execute();
    assertThat(response.isSuccess()).isFalse();
    assertThat(response.code()).isEqualTo(404);
    assertThat(response.errorBody().string()).isEqualTo("Hi");
  }

  @Test public void http404Async() throws InterruptedException, IOException {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    Service example = ra.create(Service.class);

    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    final AtomicReference<Response<String>> responseRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.getMethod().enqueue(new Callback<String>() {
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
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    Service example = ra.create(Service.class);

    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

    Call<String> call = example.getMethod();
    try {
      call.execute();
      fail();
    } catch (IOException ignored) {
    }
  }

  @Test public void transportProblemAsync() throws InterruptedException {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    Service example = ra.create(Service.class);

    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.getMethod().enqueue(new Callback<String>() {
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
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(new ForwardingConverter(new GsonConverter()) {
          @Override public RequestBody toBody(Object object, Type type) {
            throw new UnsupportedOperationException("I am broken!");
          }
        })
        .build();
    Service example = ra.create(Service.class);

    Call<String> call = example.postMethod("Hi");
    try {
      call.execute();
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("I am broken!");
    }
  }

  @Test public void conversionProblemOutgoingAsync() throws InterruptedException {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(new ForwardingConverter(new GsonConverter()) {
          @Override public RequestBody toBody(Object object, Type type) {
            throw new UnsupportedOperationException("I am broken!");
          }
        })
        .build();
    Service example = ra.create(Service.class);

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.postMethod("Hi").enqueue(new Callback<String>() {
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
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(new ForwardingConverter(new GsonConverter()) {
          @Override public Object fromBody(ResponseBody body, Type type) throws IOException {
            throw new UnsupportedOperationException("I am broken!");
          }
        })
        .build();
    Service example = ra.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    Call<String> call = example.postMethod("Hi");
    try {
      call.execute();
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("I am broken!");
    }
  }

  @Test public void conversionProblemIncomingAsync() throws InterruptedException {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(new ForwardingConverter(new GsonConverter()) {
          @Override public Object fromBody(ResponseBody body, Type type) throws IOException {
            throw new UnsupportedOperationException("I am broken!");
          }
        })
        .build();
    Service example = ra.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.postMethod("Hi").enqueue(new Callback<String>() {
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
    Converter converter = spy(new GsonConverter());
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(converter)
        .build();
    Service example = ra.create(Service.class);

    server.enqueue(new MockResponse().setStatus("HTTP/1.1 204 Nothin"));

    Response<String> response = example.getMethod().execute();
    assertThat(response.code()).isEqualTo(204);
    assertThat(response.body()).isNull();
    verifyNoMoreInteractions(converter);
  }

  @Test public void http205SkipsConverter() throws IOException {
    Converter converter = spy(new GsonConverter());
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(converter)
        .build();
    Service example = ra.create(Service.class);

    server.enqueue(new MockResponse().setStatus("HTTP/1.1 205 Nothin"));

    Response<String> response = example.getMethod().execute();
    assertThat(response.code()).isEqualTo(205);
    assertThat(response.body()).isNull();
    verifyNoMoreInteractions(converter);
  }

  @Test public void successfulRequestResponseWhenMimeTypeMissing() throws Exception {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    Service example = ra.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi").removeHeader("Content-Type"));

    Response<String> response = example.getMethod().execute();
    assertThat(response.body()).isEqualTo("Hi");
  }

  static abstract class ForwardingConverter implements Converter {
    private final Converter delegate;

    protected ForwardingConverter(Converter delegate) {
      this.delegate = delegate;
    }

    @Override public Object fromBody(ResponseBody body, Type type) throws IOException {
      return delegate.fromBody(body, type);
    }

    @Override public RequestBody toBody(Object object, Type type) {
      return delegate.toBody(object, type);
    }
  }
}
