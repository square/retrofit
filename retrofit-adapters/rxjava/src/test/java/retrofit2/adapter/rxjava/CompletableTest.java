/*
 * Copyright (C) 2016 Square, Inc.
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

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import rx.Completable;

public final class CompletableTest {
  @Rule public final MockWebServer server = new MockWebServer();
  @Rule public final TestRule pluginsReset = new RxJavaPluginsResetRule();
  @Rule public final RecordingSubscriber.Rule subscriberRule = new RecordingSubscriber.Rule();

  interface Service {
    @GET("/")
    Completable completable();
  }

  private Service service;

  @Before
  public void setUp() {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void completableSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    RecordingSubscriber<Void> subscriber = subscriberRule.create();
    service.completable().unsafeSubscribe(subscriber);
    subscriber.assertCompleted();
  }

  @Test
  public void completableSuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    RecordingSubscriber<Void> subscriber = subscriberRule.create();
    service.completable().unsafeSubscribe(subscriber);
    // Required for backwards compatibility.
    subscriber.assertError(HttpException.class, "HTTP 404 Client Error");
  }

  @Test
  public void completableFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    RecordingSubscriber<Void> subscriber = subscriberRule.create();
    service.completable().unsafeSubscribe(subscriber);
    subscriber.assertError(IOException.class);
  }

  @Test
  public void subscribeTwice() {
    server.enqueue(new MockResponse().setBody("Hi"));
    server.enqueue(new MockResponse().setBody("Hey"));

    Completable observable = service.completable();

    RecordingSubscriber<String> subscriber1 = subscriberRule.create();
    observable.subscribe(subscriber1);
    subscriber1.assertCompleted();

    RecordingSubscriber<String> subscriber2 = subscriberRule.create();
    observable.subscribe(subscriber2);
    subscriber2.assertCompleted();
  }
}
