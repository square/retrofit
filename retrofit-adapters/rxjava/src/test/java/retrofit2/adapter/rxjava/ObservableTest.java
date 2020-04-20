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
package retrofit2.adapter.rxjava;

import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import rx.Observable;

public final class ObservableTest {
  @Rule public final MockWebServer server = new MockWebServer();
  @Rule public final TestRule pluginsReset = new RxJavaPluginsResetRule();
  @Rule public final RecordingSubscriber.Rule subscriberRule = new RecordingSubscriber.Rule();

  interface Service {
    @GET("/")
    Observable<String> body();

    @GET("/")
    Observable<Response<String>> response();

    @GET("/")
    Observable<Result<String>> result();
  }

  private Service service;

  @Before
  public void setUp() {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(new StringConverterFactory())
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void bodySuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    RecordingSubscriber<String> subscriber = subscriberRule.create();
    service.body().unsafeSubscribe(subscriber);
    subscriber.assertValue("Hi").assertCompleted();
  }

  @Test
  public void bodySuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    RecordingSubscriber<String> subscriber = subscriberRule.create();
    service.body().unsafeSubscribe(subscriber);
    // Required for backwards compatibility.
    subscriber.assertError(HttpException.class, "HTTP 404 Client Error");
  }

  @Test
  public void bodyFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    RecordingSubscriber<String> subscriber = subscriberRule.create();
    service.body().unsafeSubscribe(subscriber);
    subscriber.assertError(IOException.class);
  }

  @Test
  public void bodyRespectsBackpressure() {
    server.enqueue(new MockResponse().setBody("Hi"));

    RecordingSubscriber<String> subscriber = subscriberRule.createWithInitialRequest(0);
    service.body().unsafeSubscribe(subscriber);
    assertThat(server.getRequestCount()).isEqualTo(1);
    subscriber.assertNoEvents();

    subscriber.requestMore(1);
    subscriber.assertAnyValue().assertCompleted();

    subscriber.requestMore(Long.MAX_VALUE); // Subsequent requests do not trigger HTTP requests.
    assertThat(server.getRequestCount()).isEqualTo(1);
  }

  @Test
  public void responseSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    RecordingSubscriber<Response<String>> subscriber = subscriberRule.create();
    service.response().unsafeSubscribe(subscriber);
    assertThat(subscriber.takeValue().body()).isEqualTo("Hi");
    subscriber.assertCompleted();
  }

  @Test
  public void responseSuccess404() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(404));

    RecordingSubscriber<Response<String>> subscriber = subscriberRule.create();
    service.response().unsafeSubscribe(subscriber);
    assertThat(subscriber.takeValue().code()).isEqualTo(404);
    subscriber.assertCompleted();
  }

  @Test
  public void responseFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    RecordingSubscriber<Response<String>> subscriber = subscriberRule.create();
    service.response().unsafeSubscribe(subscriber);
    subscriber.assertError(IOException.class);
  }

  @Test
  public void responseRespectsBackpressure() {
    server.enqueue(new MockResponse().setBody("Hi"));

    RecordingSubscriber<Response<String>> subscriber = subscriberRule.createWithInitialRequest(0);
    service.response().unsafeSubscribe(subscriber);
    assertThat(server.getRequestCount()).isEqualTo(1);
    subscriber.assertNoEvents();

    subscriber.requestMore(1);
    subscriber.assertAnyValue().assertCompleted();

    subscriber.requestMore(Long.MAX_VALUE); // Subsequent requests do not trigger HTTP requests.
    assertThat(server.getRequestCount()).isEqualTo(1);
  }

  @Test
  public void responseUnsubscribedDoesNotCallCompleted() throws InterruptedException {
    server.enqueue(new MockResponse().setBody("Hi"));

    final RecordingSubscriber<Response<String>> subscriber = subscriberRule.create();
    service.response().doOnNext(response -> subscriber.unsubscribe()).subscribe(subscriber);

    assertThat(subscriber.takeValue().body()).isEqualTo("Hi");
  }

  @Test
  public void resultSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    RecordingSubscriber<Result<String>> subscriber = subscriberRule.create();
    service.result().unsafeSubscribe(subscriber);
    assertThat(subscriber.takeValue().response().body()).isEqualTo("Hi");
    subscriber.assertCompleted();
  }

  @Test
  public void resultSuccess404() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(404));

    RecordingSubscriber<Result<String>> subscriber = subscriberRule.create();
    service.result().unsafeSubscribe(subscriber);
    assertThat(subscriber.takeValue().response().code()).isEqualTo(404);
    subscriber.assertCompleted();
  }

  @Test
  public void resultFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    RecordingSubscriber<Result<String>> subscriber = subscriberRule.create();
    service.result().unsafeSubscribe(subscriber);
    assertThat(subscriber.takeValue().error()).isInstanceOf(IOException.class);
    subscriber.assertCompleted();
  }

  @Test
  public void resultRespectsBackpressure() {
    server.enqueue(new MockResponse().setBody("Hi"));

    RecordingSubscriber<Result<String>> subscriber = subscriberRule.createWithInitialRequest(0);
    service.result().unsafeSubscribe(subscriber);
    assertThat(server.getRequestCount()).isEqualTo(1);
    subscriber.assertNoEvents();

    subscriber.requestMore(1);
    subscriber.assertAnyValue().assertCompleted();

    subscriber.requestMore(Long.MAX_VALUE); // Subsequent requests do not trigger HTTP requests.
    assertThat(server.getRequestCount()).isEqualTo(1);
  }

  @Test
  public void subscribeTwice() {
    server.enqueue(new MockResponse().setBody("Hi"));
    server.enqueue(new MockResponse().setBody("Hey"));

    Observable<String> observable = service.body();

    RecordingSubscriber<String> subscriber1 = subscriberRule.create();
    observable.subscribe(subscriber1);
    subscriber1.assertValue("Hi").assertCompleted();

    RecordingSubscriber<String> subscriber2 = subscriberRule.create();
    observable.subscribe(subscriber2);
    subscriber2.assertValue("Hey").assertCompleted();
  }
}
