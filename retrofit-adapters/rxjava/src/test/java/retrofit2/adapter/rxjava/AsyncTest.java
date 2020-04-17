/*
 * Copyright (C) 2017 Square, Inc.
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

import static java.util.concurrent.TimeUnit.SECONDS;
import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import rx.Completable;
import rx.exceptions.CompositeException;
import rx.exceptions.Exceptions;
import rx.exceptions.OnErrorFailedException;
import rx.observers.AsyncCompletableSubscriber;
import rx.observers.TestSubscriber;
import rx.plugins.RxJavaErrorHandler;
import rx.plugins.RxJavaPlugins;

public final class AsyncTest {
  @Rule public final MockWebServer server = new MockWebServer();
  @Rule public final TestRule pluginsReset = new RxJavaPluginsResetRule();

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
            .addCallAdapterFactory(RxJavaCallAdapterFactory.createAsync())
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void success() throws InterruptedException {
    TestSubscriber<Void> subscriber = new TestSubscriber<>();
    service.completable().subscribe(subscriber);
    assertFalse(subscriber.awaitValueCount(1, 1, SECONDS));

    server.enqueue(new MockResponse());
    subscriber.awaitTerminalEvent(1, SECONDS);
    subscriber.assertCompleted();
  }

  @Test
  public void failure() throws InterruptedException {
    TestSubscriber<Void> subscriber = new TestSubscriber<>();
    service.completable().subscribe(subscriber);
    assertFalse(subscriber.awaitValueCount(1, 1, SECONDS));

    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));
    subscriber.awaitTerminalEvent(1, SECONDS);
    subscriber.assertError(IOException.class);
  }

  @Test
  public void throwingInOnCompleteDeliveredToPlugin() throws InterruptedException {
    server.enqueue(new MockResponse());

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    RxJavaPlugins.getInstance()
        .registerErrorHandler(
            new RxJavaErrorHandler() {
              @Override
              public void handleError(Throwable throwable) {
                if (!errorRef.compareAndSet(null, throwable)) {
                  throw Exceptions.propagate(throwable); // Don't swallow secondary errors!
                }
                latch.countDown();
              }
            });

    final TestSubscriber<Void> subscriber = new TestSubscriber<>();
    final RuntimeException e = new RuntimeException();
    service
        .completable()
        .unsafeSubscribe(
            new AsyncCompletableSubscriber() {
              @Override
              public void onCompleted() {
                throw e;
              }

              @Override
              public void onError(Throwable t) {
                subscriber.onError(t);
              }
            });

    latch.await(1, SECONDS);
    assertThat(errorRef.get()).isSameAs(e);
  }

  @Test
  public void bodyThrowingInOnErrorDeliveredToPlugin() throws InterruptedException {
    server.enqueue(new MockResponse().setResponseCode(404));

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> pluginRef = new AtomicReference<>();
    RxJavaPlugins.getInstance()
        .registerErrorHandler(
            new RxJavaErrorHandler() {
              @Override
              public void handleError(Throwable throwable) {
                if (!pluginRef.compareAndSet(null, throwable)) {
                  throw Exceptions.propagate(throwable); // Don't swallow secondary errors!
                }
                latch.countDown();
              }
            });

    final TestSubscriber<Void> subscriber = new TestSubscriber<>();
    final RuntimeException e = new RuntimeException();
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    service
        .completable()
        .unsafeSubscribe(
            new AsyncCompletableSubscriber() {
              @Override
              public void onCompleted() {
                subscriber.onCompleted();
              }

              @Override
              public void onError(Throwable t) {
                errorRef.set(t);
                throw e;
              }
            });

    assertTrue(latch.await(1, SECONDS));
    CompositeException composite = (CompositeException) pluginRef.get();
    assertThat(composite.getExceptions()).containsExactly(errorRef.get(), e);
  }

  @Test
  public void bodyThrowingInOnSafeSubscriberErrorDeliveredToPlugin() throws InterruptedException {
    server.enqueue(new MockResponse().setResponseCode(404));

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> pluginRef = new AtomicReference<>();
    RxJavaPlugins.getInstance()
        .registerErrorHandler(
            new RxJavaErrorHandler() {
              @Override
              public void handleError(Throwable throwable) {
                if (throwable instanceof OnErrorFailedException) {
                  if (!pluginRef.compareAndSet(null, throwable)) {
                    throw Exceptions.propagate(throwable); // Don't swallow secondary errors!
                  }
                  latch.countDown();
                }
              }
            });

    final TestSubscriber<Void> subscriber = new TestSubscriber<>();
    final RuntimeException e = new RuntimeException();
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    service
        .completable()
        .subscribe(
            new AsyncCompletableSubscriber() {
              @Override
              public void onCompleted() {
                subscriber.onCompleted();
              }

              @Override
              public void onError(Throwable t) {
                errorRef.set(t);
                throw e;
              }
            });

    assertTrue(latch.await(1, SECONDS));
    OnErrorFailedException failed = (OnErrorFailedException) pluginRef.get();
    CompositeException composite = (CompositeException) failed.getCause();
    assertThat(composite.getExceptions()).containsExactly(errorRef.get(), e);
  }
}
