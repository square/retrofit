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
package retrofit.mock;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Subscriber;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public final class ObservableBehaviorAdapterTest {
  interface DoWorkService {
    Observable<String> response();
  }

  private final Behavior behavior = Behavior.create(new Random(2847));
  private DoWorkService service;

  @Before public void setUp() {
    DoWorkService mockService = new DoWorkService() {
      @Override public Observable<String> response() {
        return Observable.just("Hi!");
      }
    };

    BehaviorAdapter<?> adapter = ObservableBehaviorAdapter.create();
    MockRetrofit mockRetrofit = new MockRetrofit(adapter, behavior);
    service = mockRetrofit.create(DoWorkService.class, mockService);
  }

  @Test public void failureAfterDelay() throws InterruptedException {
    behavior.setDelay(100, MILLISECONDS);
    behavior.setVariancePercent(0);
    behavior.setFailurePercent(100);

    Observable<String> observable = service.response();

    final long startNanos = System.nanoTime();
    final AtomicLong tookMs = new AtomicLong();
    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    observable.subscribe(new Subscriber<String>() {
      @Override public void onNext(String s) {
        throw new AssertionError();
      }

      @Override public void onError(Throwable throwable) {
        tookMs.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        failureRef.set(throwable);
        latch.countDown();
      }

      @Override public void onCompleted() {
      }
    });
    assertTrue(latch.await(1, SECONDS));

    assertThat(failureRef.get()).isSameAs(behavior.failureException());
    assertThat(tookMs.get()).isGreaterThanOrEqualTo(100);
  }

  @Test public void successAfterDelay() throws InterruptedException {
    behavior.setDelay(100, MILLISECONDS);
    behavior.setVariancePercent(0);
    behavior.setFailurePercent(0);

    Observable<String> observable = service.response();

    final long startNanos = System.nanoTime();
    final AtomicLong tookMs = new AtomicLong();
    final AtomicReference<String> actual = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    observable.subscribe(new Subscriber<String>() {
      @Override public void onNext(String value) {
        tookMs.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        actual.set(value);
        latch.countDown();
      }

      @Override public void onError(Throwable throwable) {
        throw new AssertionError();
      }

      @Override public void onCompleted() {
      }
    });
    assertTrue(latch.await(1, SECONDS));

    assertThat(actual.get()).isEqualTo("Hi!");
    assertThat(tookMs.get()).isGreaterThanOrEqualTo(100);
  }
}
