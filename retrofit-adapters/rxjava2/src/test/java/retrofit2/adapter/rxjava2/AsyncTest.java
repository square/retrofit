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
package retrofit2.adapter.rxjava2;

import static java.util.concurrent.TimeUnit.SECONDS;
import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import io.reactivex.Completable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.CompletableThrowingTest.ForwardingCompletableObserver;
import retrofit2.http.GET;

public final class AsyncTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/")
    Completable completable();
  }

  private Service service;
  private final List<Throwable> uncaughtExceptions = new ArrayList<>();

  @Before
  public void setUp() {
    ExecutorService executorService =
        Executors.newCachedThreadPool(
            r -> {
              Thread thread = new Thread(r);
              thread.setUncaughtExceptionHandler((t, e) -> uncaughtExceptions.add(e));
              return thread;
            });

    OkHttpClient client =
        new OkHttpClient.Builder().dispatcher(new Dispatcher(executorService)).build();
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
            .build();
    service = retrofit.create(Service.class);
  }

  @After
  public void tearDown() {
    assertTrue("Uncaught exceptions: " + uncaughtExceptions, uncaughtExceptions.isEmpty());
  }

  @Test
  public void success() throws InterruptedException {
    TestObserver<Void> observer = new TestObserver<>();
    service.completable().subscribe(observer);
    assertFalse(observer.await(1, SECONDS));

    server.enqueue(new MockResponse());
    observer.awaitTerminalEvent(1, SECONDS);
    observer.assertComplete();
  }

  @Test
  public void failure() throws InterruptedException {
    TestObserver<Void> observer = new TestObserver<>();
    service.completable().subscribe(observer);
    assertFalse(observer.await(1, SECONDS));

    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));
    observer.awaitTerminalEvent(1, SECONDS);
    observer.assertError(IOException.class);
  }

  @Test
  public void throwingInOnCompleteDeliveredToPlugin() throws InterruptedException {
    server.enqueue(new MockResponse());

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(
        throwable -> {
          if (!errorRef.compareAndSet(null, throwable)) {
            throw Exceptions.propagate(throwable); // Don't swallow secondary errors!
          }
          latch.countDown();
        });

    TestObserver<Void> observer = new TestObserver<>();
    final RuntimeException e = new RuntimeException();
    service
        .completable()
        .subscribe(
            new ForwardingCompletableObserver(observer) {
              @Override
              public void onComplete() {
                throw e;
              }
            });

    latch.await(1, SECONDS);
    assertThat(errorRef.get()).isInstanceOf(UndeliverableException.class).hasCause(e);
  }

  @Test
  public void bodyThrowingInOnErrorDeliveredToPlugin() throws InterruptedException {
    server.enqueue(new MockResponse().setResponseCode(404));

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> pluginRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(
        throwable -> {
          if (!pluginRef.compareAndSet(null, throwable)) {
            throw Exceptions.propagate(throwable); // Don't swallow secondary errors!
          }
          latch.countDown();
        });

    TestObserver<Void> observer = new TestObserver<>();
    final RuntimeException e = new RuntimeException();
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    service
        .completable()
        .subscribe(
            new ForwardingCompletableObserver(observer) {
              @Override
              public void onError(Throwable throwable) {
                errorRef.set(throwable);
                throw e;
              }
            });

    latch.await(1, SECONDS);
    //noinspection ThrowableResultOfMethodCallIgnored
    CompositeException composite = (CompositeException) pluginRef.get();
    assertThat(composite.getExceptions()).containsExactly(errorRef.get(), e);
  }

  @Test
  public void bodyThrowingFatalInOnErrorPropagates() throws InterruptedException {
    server.enqueue(new MockResponse().setResponseCode(404));

    final CountDownLatch latch = new CountDownLatch(1);

    TestObserver<Void> observer = new TestObserver<>();
    final Error e = new OutOfMemoryError("Not real");
    service
        .completable()
        .subscribe(
            new ForwardingCompletableObserver(observer) {
              @Override
              public void onError(Throwable throwable) {
                throw e;
              }
            });

    latch.await(1, SECONDS);

    assertEquals(1, uncaughtExceptions.size());
    assertSame(e, uncaughtExceptions.remove(0));
  }
}
