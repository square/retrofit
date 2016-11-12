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
import io.reactivex.MaybeObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

import java.util.concurrent.atomic.AtomicReference;

import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

public final class MaybeThrowingTest {
  @Rule public final MockWebServer server = new MockWebServer();
  @Rule public final TestRule resetRule = new RxJavaPluginsResetRule();
  @Rule public final RecordingMaybeObserver.Rule subscriberRule = new RecordingMaybeObserver.Rule();

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

  @Test public void bodyThrowingInOnSuccessDeliveredToPlugin() {
    server.enqueue(new MockResponse());

    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
      @Override public void accept(Throwable throwable) throws Exception {
        if (!throwableRef.compareAndSet(null, throwable)) {
          throw Exceptions.propagate(throwable);
        }
      }
    });

    RecordingMaybeObserver<String> observer = subscriberRule.create();
    final RuntimeException e = new RuntimeException();
    service.body().subscribe(new ForwardingObserver<String>(observer) {
      @Override public void onSuccess(String value) {
        throw e;
      }
    });

    assertThat(throwableRef.get()).isSameAs(e);
  }

  @Test public void bodyThrowingInOnErrorDeliveredToPlugin() {
    server.enqueue(new MockResponse().setResponseCode(404));

    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
      @Override public void accept(Throwable throwable) throws Exception {
        if (!throwableRef.compareAndSet(null, throwable)) {
          throw Exceptions.propagate(throwable);
        }
      }
    });

    RecordingMaybeObserver<String> observer = subscriberRule.create();
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    final RuntimeException e = new RuntimeException();
    service.body().subscribe(new ForwardingObserver<String>(observer) {
      @Override public void onError(Throwable throwable) {
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

  @Test public void responseThrowingInOnSuccessDeliveredToPlugin() {
    server.enqueue(new MockResponse());

    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
      @Override public void accept(Throwable throwable) throws Exception {
        if (!throwableRef.compareAndSet(null, throwable)) {
          throw Exceptions.propagate(throwable);
        }
      }
    });

    RecordingMaybeObserver<Response<String>> observer = subscriberRule.create();
    final RuntimeException e = new RuntimeException();
    service.response().subscribe(new ForwardingObserver<Response<String>>(observer) {
      @Override public void onSuccess(Response<String> value) {
        throw e;
      }
    });

    assertThat(throwableRef.get()).isSameAs(e);
  }

  @Test public void responseThrowingInOnErrorDeliveredToPlugin() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
      @Override public void accept(Throwable throwable) throws Exception {
        if (!throwableRef.compareAndSet(null, throwable)) {
          throw Exceptions.propagate(throwable);
        }
      }
    });

    RecordingMaybeObserver<Response<String>> observer = subscriberRule.create();
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    final RuntimeException e = new RuntimeException();
    service.response().subscribe(new ForwardingObserver<Response<String>>(observer) {
      @Override public void onError(Throwable throwable) {
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

  @Test public void resultThrowingInOnSuccessDeliveredToPlugin() {
    server.enqueue(new MockResponse());

    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
      @Override public void accept(Throwable throwable) throws Exception {
        if (!throwableRef.compareAndSet(null, throwable)) {
          throw Exceptions.propagate(throwable);
        }
      }
    });

    RecordingMaybeObserver<Result<String>> observer = subscriberRule.create();
    final RuntimeException e = new RuntimeException();
    service.result().subscribe(new ForwardingObserver<Result<String>>(observer) {
      @Override public void onSuccess(Result<String> value) {
        throw e;
      }
    });

    assertThat(throwableRef.get()).isSameAs(e);
  }

  @Ignore("Single's contract is onNext|onError so we have no way of triggering this case")
  @Test public void resultThrowingInOnErrorDeliveredToPlugin() {
    server.enqueue(new MockResponse());

    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
      @Override public void accept(Throwable throwable) throws Exception {
        if (!throwableRef.compareAndSet(null, throwable)) {
          throw Exceptions.propagate(throwable);
        }
      }
    });

    RecordingMaybeObserver<Result<String>> observer = subscriberRule.create();
    final RuntimeException first = new RuntimeException();
    final RuntimeException second = new RuntimeException();
    service.result().subscribe(new ForwardingObserver<Result<String>>(observer) {
      @Override public void onSuccess(Result<String> value) {
        // The only way to trigger onError for Result is if onSuccess throws.
        throw first;
      }

      @Override public void onError(Throwable throwable) {
        throw second;
      }
    });

    //noinspection ThrowableResultOfMethodCallIgnored
    CompositeException composite = (CompositeException) throwableRef.get();
    assertThat(composite.getExceptions()).containsExactly(first, second);
  }

  private static abstract class ForwardingObserver<T> implements MaybeObserver<T> {
    private final MaybeObserver<T> delegate;

    ForwardingObserver(MaybeObserver<T> delegate) {
      this.delegate = delegate;
    }

    @Override public void onSubscribe(Disposable disposable) {
      delegate.onSubscribe(disposable);
    }

    @Override public void onSuccess(T value) {
      delegate.onSuccess(value);
    }

    @Override public void onError(Throwable throwable) {
      delegate.onError(throwable);
    }

    @Override public void onComplete() {
      delegate.onComplete();
    }
  }
}
