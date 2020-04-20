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

import java.util.concurrent.atomic.AtomicReference;
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
import rx.exceptions.CompositeException;
import rx.exceptions.Exceptions;
import rx.exceptions.OnCompletedFailedException;
import rx.exceptions.OnErrorFailedException;
import rx.plugins.RxJavaErrorHandler;
import rx.plugins.RxJavaPlugins;

public final class ObservableThrowingSafeSubscriberTest {
  @Rule public final MockWebServer server = new MockWebServer();
  @Rule public final TestRule resetRule = new RxJavaPluginsResetRule();
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
  public void bodyThrowingInOnNextDeliveredToError() {
    server.enqueue(new MockResponse());

    RecordingSubscriber<String> observer = subscriberRule.create();
    final RuntimeException e = new RuntimeException();
    service
        .body()
        .subscribe(
            new ForwardingSubscriber<String>(observer) {
              @Override
              public void onNext(String value) {
                throw e;
              }
            });

    observer.assertError(e);
  }

  @Test
  public void bodyThrowingInOnCompleteDeliveredToPlugin() {
    server.enqueue(new MockResponse());

    final AtomicReference<Throwable> pluginRef = new AtomicReference<>();
    RxJavaPlugins.getInstance()
        .registerErrorHandler(
            new RxJavaErrorHandler() {
              @Override
              public void handleError(Throwable throwable) {
                if (throwable instanceof OnCompletedFailedException) {
                  if (!pluginRef.compareAndSet(null, throwable)) {
                    throw Exceptions.propagate(throwable); // Don't swallow secondary errors!
                  }
                }
              }
            });

    RecordingSubscriber<String> observer = subscriberRule.create();
    final RuntimeException e = new RuntimeException();
    service
        .body()
        .subscribe(
            new ForwardingSubscriber<String>(observer) {
              @Override
              public void onCompleted() {
                throw e;
              }
            });

    observer.assertAnyValue();
    assertThat(pluginRef.get().getCause()).isSameAs(e);
  }

  @Test
  public void bodyThrowingInOnErrorDeliveredToPlugin() {
    server.enqueue(new MockResponse().setResponseCode(404));

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
                }
              }
            });

    RecordingSubscriber<String> observer = subscriberRule.create();
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    final RuntimeException e = new RuntimeException();
    service
        .body()
        .subscribe(
            new ForwardingSubscriber<String>(observer) {
              @Override
              public void onError(Throwable throwable) {
                if (!errorRef.compareAndSet(null, throwable)) {
                  throw Exceptions.propagate(throwable);
                }
                throw e;
              }
            });

    CompositeException composite = (CompositeException) pluginRef.get().getCause();
    assertThat(composite.getExceptions()).containsExactly(errorRef.get(), e);
  }

  @Test
  public void responseThrowingInOnNextDeliveredToError() {
    server.enqueue(new MockResponse());

    RecordingSubscriber<Response<String>> observer = subscriberRule.create();
    final RuntimeException e = new RuntimeException();
    service
        .response()
        .subscribe(
            new ForwardingSubscriber<Response<String>>(observer) {
              @Override
              public void onNext(Response<String> value) {
                throw e;
              }
            });

    observer.assertError(e);
  }

  @Test
  public void responseThrowingInOnCompleteDeliveredToPlugin() {
    server.enqueue(new MockResponse());

    final AtomicReference<Throwable> pluginRef = new AtomicReference<>();
    RxJavaPlugins.getInstance()
        .registerErrorHandler(
            new RxJavaErrorHandler() {
              @Override
              public void handleError(Throwable throwable) {
                if (throwable instanceof OnCompletedFailedException) {
                  if (!pluginRef.compareAndSet(null, throwable)) {
                    throw Exceptions.propagate(throwable); // Don't swallow secondary errors!
                  }
                }
              }
            });

    RecordingSubscriber<Response<String>> observer = subscriberRule.create();
    final RuntimeException e = new RuntimeException();
    service
        .response()
        .subscribe(
            new ForwardingSubscriber<Response<String>>(observer) {
              @Override
              public void onCompleted() {
                throw e;
              }
            });

    observer.assertAnyValue();
    assertThat(pluginRef.get().getCause()).isSameAs(e);
  }

  @Test
  public void responseThrowingInOnErrorDeliveredToPlugin() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

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
                }
              }
            });

    RecordingSubscriber<Response<String>> observer = subscriberRule.create();
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    final RuntimeException e = new RuntimeException();
    service
        .response()
        .subscribe(
            new ForwardingSubscriber<Response<String>>(observer) {
              @Override
              public void onError(Throwable throwable) {
                if (!errorRef.compareAndSet(null, throwable)) {
                  throw Exceptions.propagate(throwable);
                }
                throw e;
              }
            });

    CompositeException composite = (CompositeException) pluginRef.get().getCause();
    assertThat(composite.getExceptions()).containsExactly(errorRef.get(), e);
  }

  @Test
  public void resultThrowingInOnNextDeliveredToError() {
    server.enqueue(new MockResponse());

    RecordingSubscriber<Result<String>> observer = subscriberRule.create();
    final RuntimeException e = new RuntimeException();
    service
        .result()
        .subscribe(
            new ForwardingSubscriber<Result<String>>(observer) {
              @Override
              public void onNext(Result<String> value) {
                throw e;
              }
            });

    observer.assertError(e);
  }

  @Test
  public void resultThrowingInOnCompletedDeliveredToPlugin() {
    server.enqueue(new MockResponse());

    final AtomicReference<Throwable> pluginRef = new AtomicReference<>();
    RxJavaPlugins.getInstance()
        .registerErrorHandler(
            new RxJavaErrorHandler() {
              @Override
              public void handleError(Throwable throwable) {
                if (throwable instanceof OnCompletedFailedException) {
                  if (!pluginRef.compareAndSet(null, throwable)) {
                    throw Exceptions.propagate(throwable); // Don't swallow secondary errors!
                  }
                }
              }
            });

    RecordingSubscriber<Result<String>> observer = subscriberRule.create();
    final RuntimeException e = new RuntimeException();
    service
        .result()
        .subscribe(
            new ForwardingSubscriber<Result<String>>(observer) {
              @Override
              public void onCompleted() {
                throw e;
              }
            });

    observer.assertAnyValue();
    assertThat(pluginRef.get().getCause()).isSameAs(e);
  }

  @Test
  public void resultThrowingInOnErrorDeliveredToPlugin() {
    server.enqueue(new MockResponse());

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
                }
              }
            });

    RecordingSubscriber<Result<String>> observer = subscriberRule.create();
    final RuntimeException first = new RuntimeException();
    final RuntimeException second = new RuntimeException();
    service
        .result()
        .subscribe(
            new ForwardingSubscriber<Result<String>>(observer) {
              @Override
              public void onNext(Result<String> value) {
                // The only way to trigger onError for a result is if onNext throws.
                throw first;
              }

              @Override
              public void onError(Throwable throwable) {
                throw second;
              }
            });

    CompositeException composite = (CompositeException) pluginRef.get().getCause();
    assertThat(composite.getExceptions()).containsExactly(first, second);
  }
}
