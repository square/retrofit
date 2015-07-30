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

import com.google.common.reflect.TypeToken;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit.http.GET;
import rx.Single;
import rx.observables.BlockingObservable;

import static com.squareup.okhttp.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class ObservableCallAdapterFactoryTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/") Single<String> body();
    @GET("/") Single<Response<String>> response();
    @GET("/") Single<Result<String>> result();
  }

  private Service service;

  @Before public void setUp() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .converterFactory(new StringConverterFactory())
        .callAdapterFactory(ObservableCallAdapterFactory.create())
        .build();
    service = retrofit.create(Service.class);
  }

  @Test public void bodySuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    BlockingObservable<String> o = service.body().toObservable().toBlocking();
    assertThat(o.first()).isEqualTo("Hi");
  }

  @Test public void bodySuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    BlockingObservable<String> o = service.body().toObservable().toBlocking();
    try {
      o.first();
      fail();
    } catch (RuntimeException e) {
      // TODO assert on some indicator of 404.
    }
  }

  @Test public void bodyFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    BlockingObservable<String> o = service.body().toObservable().toBlocking();
    try {
      o.first();
      fail();
    } catch (RuntimeException e) {
      assertThat(e.getCause()).isInstanceOf(IOException.class);
    }
  }

  @Test public void responseSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    BlockingObservable<Response<String>> o = service.response().toObservable().toBlocking();
    Response<String> response = o.first();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void responseSuccess404() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    BlockingObservable<Response<String>> o = service.response().toObservable().toBlocking();
    Response<String> response = o.first();
    assertThat(response.isSuccess()).isFalse();
    assertThat(response.errorBody().string()).isEqualTo("Hi");
  }

  @Test public void responseFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    BlockingObservable<Response<String>> o = service.response().toObservable().toBlocking();
    try {
      o.first();
      fail();
    } catch (RuntimeException t) {
      assertThat(t.getCause()).isInstanceOf(IOException.class);
    }
  }

  @Test public void resultSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    BlockingObservable<Result<String>> o = service.result().toObservable().toBlocking();
    Result<String> result = o.first();
    assertThat(result.isError()).isFalse();
    Response<String> response = result.response();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void resultSuccess404() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    BlockingObservable<Result<String>> o = service.result().toObservable().toBlocking();
    Result<String> result = o.first();
    assertThat(result.isError()).isFalse();
    Response<String> response = result.response();
    assertThat(response.isSuccess()).isFalse();
    assertThat(response.errorBody().string()).isEqualTo("Hi");
  }

  @Test public void resultFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    BlockingObservable<Result<String>> o = service.result().toObservable().toBlocking();
    Result<String> result = o.first();
    assertThat(result.isError()).isTrue();
    assertThat(result.error()).isInstanceOf(IOException.class);
  }

  @Test public void responseType() {
    CallAdapter.Factory factory = ObservableCallAdapterFactory.create();
    Type classType = new TypeToken<Single<String>>() {}.getType();
    assertThat(factory.get(classType).responseType()).isEqualTo(String.class);
    Type wilcardType = new TypeToken<Single<? extends String>>() {}.getType();
    assertThat(factory.get(wilcardType).responseType()).isEqualTo(String.class);
    Type genericType = new TypeToken<Single<List<String>>>() {}.getType();
    assertThat(factory.get(genericType).responseType()) //
        .isEqualTo(new TypeToken<List<String>>() {}.getType());
    Type responseType = new TypeToken<Single<Response<String>>>() {}.getType();
    assertThat(factory.get(responseType).responseType()).isEqualTo(String.class);
    Type resultType = new TypeToken<Single<Response<String>>>() {}.getType();
    assertThat(factory.get(resultType).responseType()).isEqualTo(String.class);
  }

  @Test public void nonObservableTypeReturnsNull() {
    CallAdapter.Factory factory = ObservableCallAdapterFactory.create();
    CallAdapter<?> adapter = factory.get(String.class);
    assertThat(adapter).isNull();
  }

  @Test public void rawTypeThrows() {
    Type type = new TypeToken<Single>() {}.getType();
    CallAdapter.Factory factory = ObservableCallAdapterFactory.create();
    try {
      factory.get(type);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Observable return type must be parameterized as Observable<Foo> or Observable<? extends Foo>");
    }
  }

  @Test public void rawResponseTypeThrows() {
    Type type = new TypeToken<Single<Response>>() {}.getType();
    CallAdapter.Factory factory = ObservableCallAdapterFactory.create();
    try {
      factory.get(type);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Response must be parameterized as Response<Foo> or Response<? extends Foo>");
    }
  }

  @Test public void rawResultTypeThrows() {
    Type type = new TypeToken<Single<Result>>() {}.getType();
    CallAdapter.Factory factory = ObservableCallAdapterFactory.create();
    try {
      factory.get(type);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Result must be parameterized as Result<Foo> or Result<? extends Foo>");
    }
  }

  static class StringConverterFactory implements Converter.Factory {
    @Override public Converter<?> get(Type type) {
      return new Converter<String>() {
        @Override public String fromBody(ResponseBody body) throws IOException {
          return body.string();
        }

        @Override public RequestBody toBody(String value) {
          return RequestBody.create(MediaType.parse("text/plain"), value);
        }
      };
    }
  }
}
