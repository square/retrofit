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
package retrofit2.adapter.rxjava2;

import static org.assertj.core.api.Assertions.assertThat;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
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
import retrofit2.Retrofit;
import retrofit2.http.GET;

public final class CompletableThrowingTest {
  @Rule public final MockWebServer server = new MockWebServer();
  @Rule public final TestRule resetRule = new RxJavaPluginsResetRule();

  @Rule
  public final RecordingCompletableObserver.Rule observerRule =
      new RecordingCompletableObserver.Rule();

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
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void throwingInOnCompleteDeliveredToPlugin() {
    server.enqueue(new MockResponse());

    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(
        throwable -> {
          if (!errorRef.compareAndSet(null, throwable)) {
            throw Exceptions.propagate(throwable); // Don't swallow secondary errors!
          }
        });

    RecordingCompletableObserver observer = observerRule.create();
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

    assertThat(errorRef.get()).isInstanceOf(UndeliverableException.class).hasCause(e);
  }

  @Test
  public void bodyThrowingInOnErrorDeliveredToPlugin() {
    server.enqueue(new MockResponse().setResponseCode(404));

    final AtomicReference<Throwable> pluginRef = new AtomicReference<>();
    RxJavaPlugins.setErrorHandler(
        throwable -> {
          if (!pluginRef.compareAndSet(null, throwable)) {
            throw Exceptions.propagate(throwable); // Don't swallow secondary errors!
          }
        });

    RecordingCompletableObserver observer = observerRule.create();
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

    //noinspection ThrowableResultOfMethodCallIgnored
    CompositeException composite = (CompositeException) pluginRef.get();
    assertThat(composite.getExceptions()).containsExactly(errorRef.get(), e);
  }

  abstract static class ForwardingCompletableObserver implements CompletableObserver {
    private final CompletableObserver delegate;

    ForwardingCompletableObserver(CompletableObserver delegate) {
      this.delegate = delegate;
    }

    @Override
    public void onSubscribe(Disposable disposable) {
      delegate.onSubscribe(disposable);
    }

    @Override
    public void onComplete() {
      delegate.onComplete();
    }

    @Override
    public void onError(Throwable throwable) {
      delegate.onError(throwable);
    }
  }
}
