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

import io.reactivex.Flowable;
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

public final class FlowableTest {
  @Rule public final MockWebServer server = new MockWebServer();
  @Rule public final RecordingSubscriber.Rule subscriberRule = new RecordingSubscriber.Rule();

  interface Service {
    @GET("/") Flowable<String> body();
    @GET("/") Flowable<Response<String>> response();
    @GET("/") Flowable<Result<String>> result();
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

    RecordingSubscriber<String> subscriber = subscriberRule.create();
    service.body().subscribe(subscriber);
    subscriber.assertValue("Hi").assertComplete();
  }

  @Test public void bodySuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    RecordingSubscriber<String> subscriber = subscriberRule.create();
    service.body().subscribe(subscriber);
    // Required for backwards compatibility.
    subscriber.assertError(HttpException.class, "HTTP 404 Client Error");
  }

  @Test public void bodyFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    RecordingSubscriber<String> subscriber = subscriberRule.create();
    service.body().subscribe(subscriber);
    subscriber.assertError(IOException.class);
  }

  @Test public void bodyRespectsBackpressure() {
    server.enqueue(new MockResponse().setBody("Hi"));

    RecordingSubscriber<String> subscriber = subscriberRule.createWithInitialRequest(0);
    Flowable<String> o = service.body();

    o.subscribe(subscriber);
    assertThat(server.getRequestCount()).isEqualTo(1);
    subscriber.assertNoEvents();

    subscriber.request(1);
    subscriber.assertAnyValue().assertComplete();

    subscriber.request(Long.MAX_VALUE); // Subsequent requests do not trigger HTTP or notifications.
    assertThat(server.getRequestCount()).isEqualTo(1);
  }

  @Test public void responseSuccess200() {
    server.enqueue(new MockResponse());

    RecordingSubscriber<Response<String>> subscriber = subscriberRule.create();
    service.response().subscribe(subscriber);
    assertThat(subscriber.takeValue().isSuccessful()).isTrue();
    subscriber.assertComplete();
  }

  @Test public void responseSuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    RecordingSubscriber<Response<String>> subscriber = subscriberRule.create();
    service.response().subscribe(subscriber);
    assertThat(subscriber.takeValue().isSuccessful()).isFalse();
    subscriber.assertComplete();
  }

  @Test public void responseFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    RecordingSubscriber<Response<String>> subscriber = subscriberRule.create();
    service.response().subscribe(subscriber);
    subscriber.assertError(IOException.class);
  }

  @Test public void responseRespectsBackpressure() {
    server.enqueue(new MockResponse().setBody("Hi"));

    RecordingSubscriber<Response<String>> subscriber = subscriberRule.createWithInitialRequest(0);
    Flowable<Response<String>> o = service.response();

    o.subscribe(subscriber);
    assertThat(server.getRequestCount()).isEqualTo(1);
    subscriber.assertNoEvents();

    subscriber.request(1);
    subscriber.assertAnyValue().assertComplete();

    subscriber.request(Long.MAX_VALUE); // Subsequent requests do not trigger HTTP or notifications.
    assertThat(server.getRequestCount()).isEqualTo(1);
  }

  @Test public void resultSuccess200() {
    server.enqueue(new MockResponse());

    RecordingSubscriber<Result<String>> subscriber = subscriberRule.create();
    service.result().subscribe(subscriber);
    Result<String> result = subscriber.takeValue();
    assertThat(result.isError()).isFalse();
    assertThat(result.response().isSuccessful()).isTrue();
    subscriber.assertComplete();
  }

  @Test public void resultSuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    RecordingSubscriber<Result<String>> subscriber = subscriberRule.create();
    service.result().subscribe(subscriber);
    Result<String> result = subscriber.takeValue();
    assertThat(result.isError()).isFalse();
    assertThat(result.response().isSuccessful()).isFalse();
    subscriber.assertComplete();
  }

  @Test public void resultFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    RecordingSubscriber<Result<String>> subscriber = subscriberRule.create();
    service.result().subscribe(subscriber);
    Result<String> result = subscriber.takeValue();
    assertThat(result.isError()).isTrue();
    assertThat(result.error()).isInstanceOf(IOException.class);
    subscriber.assertComplete();
  }

  @Test public void resultRespectsBackpressure() {
    server.enqueue(new MockResponse().setBody("Hi"));

    RecordingSubscriber<Result<String>> subscriber = subscriberRule.createWithInitialRequest(0);
    Flowable<Result<String>> o = service.result();

    o.subscribe(subscriber);
    assertThat(server.getRequestCount()).isEqualTo(1);
    subscriber.assertNoEvents();

    subscriber.request(1);
    subscriber.assertAnyValue().assertComplete();

    subscriber.request(Long.MAX_VALUE); // Subsequent requests do not trigger HTTP or notifications.
    assertThat(server.getRequestCount()).isEqualTo(1);
  }

  @Test public void subscribeTwice() {
    server.enqueue(new MockResponse().setBody("Hi"));
    server.enqueue(new MockResponse().setBody("Hey"));

    Flowable<String> observable = service.body();

    RecordingSubscriber<Object> subscriber1 = subscriberRule.create();
    observable.subscribe(subscriber1);
    subscriber1.assertValue("Hi").assertComplete();

    RecordingSubscriber<Object> subscriber2 = subscriberRule.create();
    observable.subscribe(subscriber2);
    subscriber2.assertValue("Hey").assertComplete();
  }
}
