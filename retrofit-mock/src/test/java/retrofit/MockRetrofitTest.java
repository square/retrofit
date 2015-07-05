/*
 * Copyright (C) 2013 Square, Inc.
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
package retrofit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MockRetrofitTest {
  private MockRetrofit mockRetrofit;

  @Before public void setUp() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .build();
    mockRetrofit = MockRetrofit.from(retrofit, Executors.newCachedThreadPool());

    // Seed the random with a value so the tests are deterministic.
    mockRetrofit.random.setSeed(2847);
  }

  @Test public void delayRestrictsRange() {
    try {
      mockRetrofit.setDelay(-1, SECONDS);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Amount must be positive value.");
    }
    try {
      mockRetrofit.setDelay(Long.MAX_VALUE, SECONDS);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageStartingWith("Delay value too large.");
    }
  }

  @Test public void varianceRestrictsRange() {
    try {
      mockRetrofit.setVariancePercent(-13);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Variance percentage must be between 0 and 100.");
    }
    try {
      mockRetrofit.setVariancePercent(174);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Variance percentage must be between 0 and 100.");
    }
  }

  @Test public void errorRestrictsRange() {
    try {
      mockRetrofit.setErrorPercent(-13);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Error percentage must be between 0 and 100.");
    }
    try {
      mockRetrofit.setErrorPercent(174);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Error percentage must be between 0 and 100.");
    }
  }

  @Test public void errorPercentageIsAccurate() {
    mockRetrofit.setErrorPercent(0);
    for (int i = 0; i < 10000; i++) {
      assertThat(mockRetrofit.calculateIsFailure()).isFalse();
    }

    mockRetrofit.setErrorPercent(3);
    int failures = 0;
    for (int i = 0; i < 100000; i++) {
      if (mockRetrofit.calculateIsFailure()) {
        failures += 1;
      }
    }
    assertThat(failures).isEqualTo(2964); // ~3% of 100k
  }

  @Test public void delayVarianceIsAccurate() {
    mockRetrofit.setDelay(2, SECONDS);

    mockRetrofit.setVariancePercent(0);
    for (int i = 0; i < 100000; i++) {
      assertThat(mockRetrofit.calculateDelayForCall()).isEqualTo(2000);
    }

    mockRetrofit.setVariancePercent(40);
    int lowerBound = Integer.MAX_VALUE;
    int upperBound = Integer.MIN_VALUE;
    for (int i = 0; i < 100000; i++) {
      int delay = mockRetrofit.calculateDelayForCall();
      if (delay > upperBound) {
        upperBound = delay;
      }
      if (delay < lowerBound) {
        lowerBound = delay;
      }
    }
    assertThat(upperBound).isEqualTo(2799); // ~40% above 2000
    assertThat(lowerBound).isEqualTo(1200); // ~40% below 2000
  }

  @Test public void errorVarianceIsAccurate() {
    mockRetrofit.setDelay(2, SECONDS);

    int lowerBound = Integer.MAX_VALUE;
    int upperBound = Integer.MIN_VALUE;
    for (int i = 0; i < 100000; i++) {
      int delay = mockRetrofit.calculateDelayForError();
      if (delay > upperBound) {
        upperBound = delay;
      }
      if (delay < lowerBound) {
        lowerBound = delay;
      }
    }
    assertThat(upperBound).isEqualTo(5999); // 3 * 2000
    assertThat(lowerBound).isEqualTo(0);
  }

  @Test public void syncErrorThrows() {
    mockRetrofit.setErrorPercent(100);
    mockRetrofit.setDelay(1, MILLISECONDS);

    Call<String> call = mockRetrofit.newSuccessCall("Hi");

    try {
      call.execute();
      fail();
    } catch (IOException e) {
      assertThat(e).hasMessage("Mock exception");
    }
  }

  @Test public void asyncErrorTriggersFailure() throws InterruptedException {
    mockRetrofit.setErrorPercent(100);
    mockRetrofit.setDelay(1, MILLISECONDS);

    Call<String> call = mockRetrofit.newSuccessCall("Hi");

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<String>() {
      @Override public void onResponse(Response<String> response) {
        throw new AssertionError();
      }

      @Override public void onFailure(Throwable t) {
        failureRef.set(t);
        latch.countDown();
      }
    });
    assertTrue(latch.await(1, SECONDS));

    assertThat(failureRef.get()).hasMessage("Mock exception");
  }

  @Test public void syncSuccessReturnsAfterDelay() throws IOException {
    mockRetrofit.setDelay(100, MILLISECONDS);
    mockRetrofit.setVariancePercent(0);
    mockRetrofit.setErrorPercent(0);

    Call<String> call = mockRetrofit.newSuccessCall("Hi");

    long startNanos = System.nanoTime();
    Response<String> response = call.execute();
    long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

    assertThat(response.body()).isEqualTo("Hi");
    assertThat(tookMs).isGreaterThanOrEqualTo(100);
  }

  @Test public void asyncSuccessCalledAfterDelay() throws InterruptedException {
    mockRetrofit.setDelay(100, MILLISECONDS);
    mockRetrofit.setVariancePercent(0);
    mockRetrofit.setErrorPercent(0);

    Call<String> call = mockRetrofit.newSuccessCall("Hi");

    final long startNanos = System.nanoTime();
    final AtomicLong tookMs = new AtomicLong();
    final AtomicReference<Object> actual = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<String>() {
      @Override public void onResponse(Response<String> response) {
        tookMs.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        actual.set(response.body());
        latch.countDown();
      }

      @Override public void onFailure(Throwable t) {
        throw new AssertionError();
      }
    });
    assertTrue(latch.await(1, SECONDS));

    assertThat(actual.get()).isEqualTo("Hi");
    assertThat(tookMs.get()).isGreaterThanOrEqualTo(100);
  }

  @Test public void syncFailureThrownAfterDelay() {
    mockRetrofit.setDelay(100, MILLISECONDS);
    mockRetrofit.setVariancePercent(0);
    mockRetrofit.setErrorPercent(0);

    IOException failure = new FileNotFoundException("Oh noes");
    Call<Object> call = mockRetrofit.newFailureCall(failure);

    long startNanos = System.nanoTime();
    try {
      call.execute();
      fail();
    } catch (IOException e) {
      long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
      assertThat(tookMs).isGreaterThanOrEqualTo(100);
      assertThat(e).isSameAs(failure);
    }
  }

  @Test public void asyncFailureCalledAfterDelay() throws InterruptedException {
    mockRetrofit.setDelay(100, MILLISECONDS);
    mockRetrofit.setVariancePercent(0);
    mockRetrofit.setErrorPercent(0);

    IOException failure = new FileNotFoundException("Oh noes");
    Call<Object> call = mockRetrofit.newFailureCall(failure);

    final AtomicLong tookMs = new AtomicLong();
    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    final long startNanos = System.nanoTime();
    call.enqueue(new Callback<Object>() {
      @Override public void onResponse(Response<Object> response) {
        throw new AssertionError();
      }

      @Override public void onFailure(Throwable t) {
        tookMs.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        failureRef.set(t);
        latch.countDown();
      }
    });
    assertTrue(latch.await(1, SECONDS));

    assertThat(tookMs.get()).isGreaterThanOrEqualTo(100);
    assertThat(failureRef.get()).isSameAs(failure);
  }

  @Test public void syncCanBeCanceled() throws IOException {
    mockRetrofit.setDelay(10, SECONDS);
    mockRetrofit.setVariancePercent(0);
    mockRetrofit.setErrorPercent(0);

    final Call<String> call = mockRetrofit.newSuccessCall("Hi");

    new Thread(new Runnable() {
      @Override public void run() {
        try {
          Thread.sleep(100);
          call.cancel();
        } catch (InterruptedException ignored) {
        }
      }
    }).start();

    try {
      call.execute();
      fail();
    } catch (InterruptedIOException e) {
      assertThat(e).hasMessage("canceled");
    }
  }

  @Test public void syncCanceledBeforeStart() throws IOException {
    mockRetrofit.setDelay(100, MILLISECONDS);
    mockRetrofit.setVariancePercent(0);
    mockRetrofit.setErrorPercent(0);

    final Call<String> call = mockRetrofit.newSuccessCall("Hi");

    call.cancel();
    try {
      call.execute();
      fail();
    } catch (InterruptedIOException e) {
      assertThat(e).hasMessage("canceled");
    }
  }

  @Test public void asyncCanBeCanceled() throws InterruptedException {
    mockRetrofit.setDelay(10, SECONDS);
    mockRetrofit.setVariancePercent(0);
    mockRetrofit.setErrorPercent(0);

    final Call<String> call = mockRetrofit.newSuccessCall("Hi");

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<String>() {
      @Override public void onResponse(Response<String> response) {
        latch.countDown();
      }

      @Override public void onFailure(Throwable t) {
        failureRef.set(t);
        latch.countDown();
      }
    });

    // TODO we shouldn't need to sleep
    Thread.sleep(100); // Ensure the task has started.
    call.cancel();

    assertTrue(latch.await(1, SECONDS));
    assertThat(failureRef.get()).isInstanceOf(InterruptedIOException.class).hasMessage("canceled");
  }
}
