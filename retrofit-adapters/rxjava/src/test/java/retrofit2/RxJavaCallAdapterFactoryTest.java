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

import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.http.GET;
import rx.Observable;
import rx.Single;
import rx.observables.BlockingObservable;
import rx.singles.BlockingSingle;

import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class RxJavaCallAdapterFactoryTest {
  private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/") Observable<String> observableBody();
    @GET("/") Observable<Response<String>> observableResponse();
    @GET("/") Observable<Result<String>> observableResult();
    @GET("/") Single<String> singleBody();
    @GET("/") Single<Response<String>> singleResponse();
    @GET("/") Single<Result<String>> singleResult();
  }

  private Retrofit retrofit;
  private Service service;

  @Before public void setUp() {
    retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build();
    service = retrofit.create(Service.class);
  }

  @Test public void bodySuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    BlockingObservable<String> o = service.observableBody().toBlocking();
    assertThat(o.first()).isEqualTo("Hi");
  }

  @Test public void bodySuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    BlockingObservable<String> o = service.observableBody().toBlocking();
    try {
      o.first();
      fail();
    } catch (RuntimeException e) {
      Throwable cause = e.getCause();
      assertThat(cause).isInstanceOf(HttpException.class).hasMessage("HTTP 404 Client Error");
    }
  }

  @Test public void bodyFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    BlockingObservable<String> o = service.observableBody().toBlocking();
    try {
      o.first();
      fail();
    } catch (RuntimeException e) {
      assertThat(e.getCause()).isInstanceOf(IOException.class);
    }
  }

  @Test public void responseSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    BlockingObservable<Response<String>> o = service.observableResponse().toBlocking();
    Response<String> response = o.first();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void responseSuccess404() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    BlockingObservable<Response<String>> o = service.observableResponse().toBlocking();
    Response<String> response = o.first();
    assertThat(response.isSuccess()).isFalse();
    assertThat(response.errorBody().string()).isEqualTo("Hi");
  }

  @Test public void responseFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    BlockingObservable<Response<String>> o = service.observableResponse().toBlocking();
    try {
      o.first();
      fail();
    } catch (RuntimeException t) {
      assertThat(t.getCause()).isInstanceOf(IOException.class);
    }
  }

  @Test public void resultSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    BlockingObservable<Result<String>> o = service.observableResult().toBlocking();
    Result<String> result = o.first();
    assertThat(result.isError()).isFalse();
    Response<String> response = result.response();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void resultSuccess404() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    BlockingObservable<Result<String>> o = service.observableResult().toBlocking();
    Result<String> result = o.first();
    assertThat(result.isError()).isFalse();
    Response<String> response = result.response();
    assertThat(response.isSuccess()).isFalse();
    assertThat(response.errorBody().string()).isEqualTo("Hi");
  }

  @Test public void resultFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    BlockingObservable<Result<String>> o = service.observableResult().toBlocking();
    Result<String> result = o.first();
    assertThat(result.isError()).isTrue();
    assertThat(result.error()).isInstanceOf(IOException.class);
  }

  @Test public void responseType() {
    CallAdapter.Factory factory = RxJavaCallAdapterFactory.create();
    Type classType = new TypeToken<Observable<String>>() {}.getType();
    assertThat(factory.get(classType, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(String.class);
    Type wilcardType = new TypeToken<Observable<? extends String>>() {}.getType();
    assertThat(factory.get(wilcardType, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(String.class);
    Type genericType = new TypeToken<Observable<List<String>>>() {}.getType();
    assertThat(factory.get(genericType, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(new TypeToken<List<String>>() {}.getType());
    Type responseType = new TypeToken<Observable<Response<String>>>() {}.getType();
    assertThat(factory.get(responseType, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(String.class);
    Type resultType = new TypeToken<Observable<Response<String>>>() {}.getType();
    assertThat(factory.get(resultType, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(String.class);
  }

  @Test public void nonObservableTypeReturnsNull() {
    CallAdapter.Factory factory = RxJavaCallAdapterFactory.create();
    CallAdapter<?> adapter = factory.get(String.class, NO_ANNOTATIONS, retrofit);
    assertThat(adapter).isNull();
  }

  @Test public void rawTypeThrows() {
    CallAdapter.Factory factory = RxJavaCallAdapterFactory.create();
    Type observableType = new TypeToken<Observable>() {}.getType();
    try {
      factory.get(observableType, NO_ANNOTATIONS, retrofit);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Observable return type must be parameterized as Observable<Foo> or Observable<? extends Foo>");
    }
    Type singleType = new TypeToken<Single>() {}.getType();
    try {
      factory.get(singleType, NO_ANNOTATIONS, retrofit);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Single return type must be parameterized as Single<Foo> or Single<? extends Foo>");
    }
  }

  @Test public void rawObservableResponseTypeThrows() {
    CallAdapter.Factory factory = RxJavaCallAdapterFactory.create();
    Type observableType = new TypeToken<Observable<Response>>() {}.getType();
    try {
      factory.get(observableType, NO_ANNOTATIONS, retrofit);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Response must be parameterized as Response<Foo> or Response<? extends Foo>");
    }
    Type singleType = new TypeToken<Single<Response>>() {}.getType();
    try {
      factory.get(singleType, NO_ANNOTATIONS, retrofit);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Response must be parameterized as Response<Foo> or Response<? extends Foo>");
    }
  }

  @Test public void rawResultTypeThrows() {
    CallAdapter.Factory factory = RxJavaCallAdapterFactory.create();
    Type observableType = new TypeToken<Observable<Result>>() {}.getType();
    try {
      factory.get(observableType, NO_ANNOTATIONS, retrofit);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Result must be parameterized as Result<Foo> or Result<? extends Foo>");
    }
    Type singleType = new TypeToken<Single<Result>>() {}.getType();
    try {
      factory.get(singleType, NO_ANNOTATIONS, retrofit);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Result must be parameterized as Result<Foo> or Result<? extends Foo>");
    }
  }

  @Test public void singleBodySuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    BlockingSingle<String> o = service.singleBody().toBlocking();
    assertThat(o.value()).isEqualTo("Hi");
  }

  @Test public void singleBodySuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    BlockingSingle<String> o = service.singleBody().toBlocking();
    try {
      o.value();
      fail();
    } catch (RuntimeException e) {
      Throwable cause = e.getCause();
      assertThat(cause).isInstanceOf(HttpException.class).hasMessage("HTTP 404 Client Error");
    }
  }

  @Test public void singleBodyFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    BlockingSingle<String> o = service.singleBody().toBlocking();
    try {
      o.value();
      fail();
    } catch (RuntimeException e) {
      assertThat(e.getCause()).isInstanceOf(IOException.class);
    }
  }

  @Test public void singleResponseSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    BlockingSingle<Response<String>> o = service.singleResponse().toBlocking();
    Response<String> response = o.value();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void singleResponseSuccess404() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    BlockingSingle<Response<String>> o = service.singleResponse().toBlocking();
    Response<String> response = o.value();
    assertThat(response.isSuccess()).isFalse();
    assertThat(response.errorBody().string()).isEqualTo("Hi");
  }

  @Test public void singleResponseFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    BlockingSingle<Response<String>> o = service.singleResponse().toBlocking();
    try {
      o.value();
      fail();
    } catch (RuntimeException t) {
      assertThat(t.getCause()).isInstanceOf(IOException.class);
    }
  }

  @Test public void singleResultSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    BlockingSingle<Result<String>> o = service.singleResult().toBlocking();
    Result<String> result = o.value();
    assertThat(result.isError()).isFalse();
    Response<String> response = result.response();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void singleResultSuccess404() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    BlockingSingle<Result<String>> o = service.singleResult().toBlocking();
    Result<String> result = o.value();
    assertThat(result.isError()).isFalse();
    Response<String> response = result.response();
    assertThat(response.isSuccess()).isFalse();
    assertThat(response.errorBody().string()).isEqualTo("Hi");
  }

  @Test public void singleResultFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    BlockingSingle<Result<String>> o = service.singleResult().toBlocking();
    Result<String> result = o.value();
    assertThat(result.isError()).isTrue();
    assertThat(result.error()).isInstanceOf(IOException.class);
  }

  static class StringConverterFactory extends Converter.Factory {
    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
        Retrofit retrofit) {
      return new Converter<ResponseBody, String>() {
        @Override public String convert(ResponseBody value) throws IOException {
          return value.string();
        }
      };
    }

    @Override public Converter<?, RequestBody> requestBodyConverter(Type type,
        Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
      return new Converter<String, RequestBody>() {
        @Override public RequestBody convert(String value) throws IOException {
          return RequestBody.create(MediaType.parse("text/plain"), value);
        }
      };
    }
  }
}
