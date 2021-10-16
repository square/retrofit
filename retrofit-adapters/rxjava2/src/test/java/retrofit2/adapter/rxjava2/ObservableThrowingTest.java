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

import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
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

public final class ObservableThrowingTest {
  @Rule public final MockWebServer server = new MockWebServer();
  @Rule public final TestRule resetRule = new RxJavaPluginsResetRule();
  @Rule public final RecordingObserver.Rule subscriberRule = new RecordingObserver.Rule();

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
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void bodyThrowingInOnNextDeliveredToError() {
    server.enqueue(new MockResponse());

    RecordingObserver<String> observer = subscriberRule.create();
    final RuntimeException e = new RuntimeException();
    service
        .body()
        .subscribe(
            new ForwardingObserver<String>(observer) {
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

    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(
        throwable -> {
          if (!throwableRef.compareAndSet(null, throwable)) {
            throw Exceptions.propagate(throwable);
          }
        });

    RecordingObserver<String> observer = subscriberRule.create();
    final RuntimeException e = new RuntimeException();
    service
        .body()
        .subscribe(
            new ForwardingObserver<String>(observer) {
              @Override
              public void onComplete() {
                throw e;
              }
            });

    observer.assertAnyValue();
    assertThat(throwableRef.get()).isInstanceOf(UndeliverableException.class).hasCause(e);
  }

  @Test
  public void bodyThrowingInOnErrorDeliveredToPlugin() {
    server.enqueue(new MockResponse().setResponseCode(404));

    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(
        throwable -> {
          if (!throwableRef.compareAndSet(null, throwable)) {
            throw Exceptions.propagate(throwable);
          }
        });

    RecordingObserver<String> observer = subscriberRule.create();
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    final RuntimeException e = new RuntimeException();
    service
        .body()
        .subscribe(
            new ForwardingObserver<String>(observer) {
              @Override
              public void onError(Throwable throwable) {
                if (!errorRef.compareAndSet(null, throwable)) {
                  throw Exceptions.propagate(throwable);
                }
                throw e;
              }
            });

    //noinspection ThrowableResultOfMethodCallIgnored
    CompositeException composite = (CompositeException) throwableRef.get();
    assertThat(composite.getExceptions()).containsExactly(errorRef.get(), e);
  }

  @Test
  public void responseThrowingInOnNextDeliveredToError() {
    server.enqueue(new MockResponse());

    RecordingObserver<Response<String>> observer = subscriberRule.create();
    final RuntimeException e = new RuntimeException();
    service
        .response()
        .subscribe(
            new ForwardingObserver<Response<String>>(observer) {
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

    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(
        throwable -> {
          if (!throwableRef.compareAndSet(null, throwable)) {
            throw Exceptions.propagate(throwable);
          }
        });

    RecordingObserver<Response<String>> observer = subscriberRule.create();
    final RuntimeException e = new RuntimeException();
    service
        .response()
        .subscribe(
            new ForwardingObserver<Response<String>>(observer) {
              @Override
              public void onComplete() {
                throw e;
              }
            });

    observer.assertAnyValue();
    assertThat(throwableRef.get()).isInstanceOf(UndeliverableException.class).hasCause(e);
  }

  @Test
  public void responseThrowingInOnErrorDeliveredToPlugin() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(
        throwable -> {
          if (!throwableRef.compareAndSet(null, throwable)) {
            throw Exceptions.propagate(throwable);
          }
        });

    RecordingObserver<Response<String>> observer = subscriberRule.create();
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    final RuntimeException e = new RuntimeException();
    service
        .response()
        .subscribe(
            new ForwardingObserver<Response<String>>(observer) {
              @Override
              public void onError(Throwable throwable) {
                if (!errorRef.compareAndSet(null, throwable)) {
                  throw Exceptions.propagate(throwable);
                }
                throw e;
              }
            });

    //noinspection ThrowableResultOfMethodCallIgnored
    CompositeException composite = (CompositeException) throwableRef.get();
    assertThat(composite.getExceptions()).containsExactly(errorRef.get(), e);
  }

  @Test
  public void resultThrowingInOnNextDeliveredToError() {
    server.enqueue(new MockResponse());

    RecordingObserver<Result<String>> observer = subscriberRule.create();
    final RuntimeException e = new RuntimeException();
    service
        .result()
        .subscribe(
            new ForwardingObserver<Result<String>>(observer) {
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

    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(
        throwable -> {
          if (!throwableRef.compareAndSet(null, throwable)) {
            throw Exceptions.propagate(throwable);
          }
        });

    RecordingObserver<Result<String>> observer = subscriberRule.create();
    final RuntimeException e = new RuntimeException();
    service
        .result()
        .subscribe(
            new ForwardingObserver<Result<String>>(observer) {
              @Override
              public void onComplete() {
                throw e;
              }
            });

    observer.assertAnyValue();
    assertThat(throwableRef.get()).isInstanceOf(UndeliverableException.class).hasCause(e);
  }

  @Test
  public void resultThrowingInOnErrorDeliveredToPlugin() {
    server.enqueue(new MockResponse());

    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(
        throwable -> {
          if (!throwableRef.compareAndSet(null, throwable)) {
            throw Exceptions.propagate(throwable);
          }
        });

    RecordingObserver<Result<String>> observer = subscriberRule.create();
    final RuntimeException first = new RuntimeException();
    final RuntimeException second = new RuntimeException();
    service
        .result()
        .subscribe(
            new ForwardingObserver<Result<String>>(observer) {
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

    //noinspection ThrowableResultOfMethodCallIgnored
    CompositeException composite = (CompositeException) throwableRef.get();
    assertThat(composite.getExceptions()).containsExactly(first, second);
  }

  private abstract static class ForwardingObserver<T> implements Observer<T> {
    private final Observer<T> delegate;

    ForwardingObserver(Observer<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public void onSubscribe(Disposable disposable) {
      delegate.onSubscribe(disposable);
    }

    @Override
    public void onNext(T value) {
      delegate.onNext(value);
    }

    @Override
    public void onError(Throwable throwable) {
      delegate.onError(throwable);
    }

    @Override
    public void onComplete() {
      delegate.onComplete();
    }
  }
}
