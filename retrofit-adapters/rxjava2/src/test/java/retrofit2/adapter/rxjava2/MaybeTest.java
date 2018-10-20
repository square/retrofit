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
package retrofit2.adapter.rxjava2;

import io.reactivex.Maybe;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

public final class MaybeTest {
  @Rule public final MockWebServer server = new MockWebServer();
  @Rule public final RecordingMaybeObserver.Rule observerRule = new RecordingMaybeObserver.Rule();

  interface Service {
    @GET("/") Maybe<String> body();
    @GET("/") Maybe<Response<String>> response();
    @GET("/") Maybe<Result<String>> result();
  }

  private Service service;

  @Before public void setUp() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build();
    service = retrofit.create(Service.class);
  }

  @Test public void bodySuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    RecordingMaybeObserver<String> observer = observerRule.create();
    service.body().subscribe(observer);
    observer.assertValue("Hi");
  }

  @Test public void bodySuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    RecordingMaybeObserver<String> observer = observerRule.create();
    service.body().subscribe(observer);
    // Required for backwards compatibility.
    observer.assertError(HttpException.class, "HTTP 404 Client Error");
  }

  @Test public void bodyFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    RecordingMaybeObserver<String> observer = observerRule.create();
    service.body().subscribe(observer);
    observer.assertError(IOException.class);
  }

  @Test public void responseSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    RecordingMaybeObserver<Response<String>> observer = observerRule.create();
    service.response().subscribe(observer);
    Response<String> response = observer.takeValue();
    assertThat(response.isSuccessful()).isTrue();
  }

  @Test public void responseSuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    RecordingMaybeObserver<Response<String>> observer = observerRule.create();
    service.response().subscribe(observer);
    assertThat(observer.takeValue().isSuccessful()).isFalse();
  }

  @Test public void responseFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    RecordingMaybeObserver<Response<String>> observer = observerRule.create();
    service.response().subscribe(observer);
    observer.assertError(IOException.class);
  }

  @Test public void resultSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    RecordingMaybeObserver<Result<String>> observer = observerRule.create();
    service.result().subscribe(observer);
    Result<String> result = observer.takeValue();
    assertThat(result.isError()).isFalse();
    assertThat(result.response().isSuccessful()).isTrue();
  }

  @Test public void resultSuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    RecordingMaybeObserver<Result<String>> observer = observerRule.create();
    service.result().subscribe(observer);
    Result<String> result = observer.takeValue();
    assertThat(result.isError()).isFalse();
    assertThat(result.response().isSuccessful()).isFalse();
  }

  @Test public void resultFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    RecordingMaybeObserver<Result<String>> observer = observerRule.create();
    service.result().subscribe(observer);
    Result<String> result = observer.takeValue();
    assertThat(result.isError()).isTrue();
    assertThat(result.error()).isInstanceOf(IOException.class);
  }

  @Test public void subscribeTwice() {
    server.enqueue(new MockResponse().setBody("Hi"));
    server.enqueue(new MockResponse().setBody("Hey"));

    Maybe<String> observable = service.body();

    RecordingMaybeObserver<Object> observer1 = observerRule.create();
    observable.subscribe(observer1);
    observer1.assertValue("Hi");

    RecordingMaybeObserver<Object> observer2 = observerRule.create();
    observable.subscribe(observer2);
    observer2.assertValue("Hey");
  }
}
