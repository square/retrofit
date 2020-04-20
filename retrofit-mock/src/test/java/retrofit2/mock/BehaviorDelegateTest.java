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

package retrofit2.mock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public final class BehaviorDelegateTest {
  interface DoWorkService {
    Call<String> response();

    Call<String> failure();
  }

  private final IOException mockFailure = new IOException("Timeout!");
  private final NetworkBehavior behavior = NetworkBehavior.create(new Random(2847));
  private DoWorkService service;

  @Before
  public void setUp() {
    Retrofit retrofit = new Retrofit.Builder().baseUrl("http://example.com").build();
    MockRetrofit mockRetrofit =
        new MockRetrofit.Builder(retrofit).networkBehavior(behavior).build();
    final BehaviorDelegate<DoWorkService> delegate = mockRetrofit.create(DoWorkService.class);

    service =
        new DoWorkService() {
          @Override
          public Call<String> response() {
            Call<String> response = Calls.response("Response!");
            return delegate.returning(response).response();
          }

          @Override
          public Call<String> failure() {
            Call<String> failure = Calls.failure(mockFailure);
            return delegate.returning(failure).failure();
          }
        };
  }

  @Test
  public void syncFailureThrowsAfterDelay() {
    behavior.setDelay(100, MILLISECONDS);
    behavior.setVariancePercent(0);
    behavior.setFailurePercent(100);

    Call<String> call = service.response();

    long startNanos = System.nanoTime();
    try {
      call.execute();
      fail();
    } catch (IOException e) {
      long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
      assertThat(e).isSameAs(behavior.failureException());
      assertThat(tookMs).isGreaterThanOrEqualTo(100);
    }
  }

  @Test
  public void asyncFailureTriggersFailureAfterDelay() throws InterruptedException {
    behavior.setDelay(100, MILLISECONDS);
    behavior.setVariancePercent(0);
    behavior.setFailurePercent(100);

    Call<String> call = service.response();

    final long startNanos = System.nanoTime();
    final AtomicLong tookMs = new AtomicLong();
    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(
        new Callback<String>() {
          @Override
          public void onResponse(Call<String> call, Response<String> response) {
            throw new AssertionError();
          }

          @Override
          public void onFailure(Call<String> call, Throwable t) {
            tookMs.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
            failureRef.set(t);
            latch.countDown();
          }
        });
    assertTrue(latch.await(1, SECONDS));

    assertThat(failureRef.get()).isSameAs(behavior.failureException());
    assertThat(tookMs.get()).isGreaterThanOrEqualTo(100);
  }

  @Test
  public void syncSuccessReturnsAfterDelay() throws IOException {
    behavior.setDelay(100, MILLISECONDS);
    behavior.setVariancePercent(0);
    behavior.setFailurePercent(0);

    Call<String> call = service.response();

    long startNanos = System.nanoTime();
    Response<String> response = call.execute();
    long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

    assertThat(response.body()).isEqualTo("Response!");
    assertThat(tookMs).isGreaterThanOrEqualTo(100);
  }

  @Test
  public void asyncSuccessCalledAfterDelay() throws InterruptedException, IOException {
    behavior.setDelay(100, MILLISECONDS);
    behavior.setVariancePercent(0);
    behavior.setFailurePercent(0);

    Call<String> call = service.response();

    final long startNanos = System.nanoTime();
    final AtomicLong tookMs = new AtomicLong();
    final AtomicReference<String> actual = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(
        new Callback<String>() {
          @Override
          public void onResponse(Call<String> call, Response<String> response) {
            tookMs.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
            actual.set(response.body());
            latch.countDown();
          }

          @Override
          public void onFailure(Call<String> call, Throwable t) {
            throw new AssertionError();
          }
        });
    assertTrue(latch.await(1, SECONDS));

    assertThat(actual.get()).isEqualTo("Response!");
    assertThat(tookMs.get()).isGreaterThanOrEqualTo(100);
  }

  @Test
  public void syncFailureThrownAfterDelay() {
    behavior.setDelay(100, MILLISECONDS);
    behavior.setVariancePercent(0);
    behavior.setFailurePercent(0);

    Call<String> call = service.failure();

    long startNanos = System.nanoTime();
    try {
      call.execute();
      fail();
    } catch (IOException e) {
      long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
      assertThat(tookMs).isGreaterThanOrEqualTo(100);
      assertThat(e).isSameAs(mockFailure);
    }
  }

  @Test
  public void asyncFailureCalledAfterDelay() throws InterruptedException {
    behavior.setDelay(100, MILLISECONDS);
    behavior.setVariancePercent(0);
    behavior.setFailurePercent(0);

    Call<String> call = service.failure();

    final AtomicLong tookMs = new AtomicLong();
    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    final long startNanos = System.nanoTime();
    call.enqueue(
        new Callback<String>() {
          @Override
          public void onResponse(Call<String> call, Response<String> response) {
            throw new AssertionError();
          }

          @Override
          public void onFailure(Call<String> call, Throwable t) {
            tookMs.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
            failureRef.set(t);
            latch.countDown();
          }
        });
    assertTrue(latch.await(1, SECONDS));

    assertThat(tookMs.get()).isGreaterThanOrEqualTo(100);
    assertThat(failureRef.get()).isSameAs(mockFailure);
  }

  @Test
  public void syncCanBeCanceled() throws IOException {
    behavior.setDelay(10, SECONDS);
    behavior.setVariancePercent(0);
    behavior.setFailurePercent(0);

    final Call<String> call = service.response();

    new Thread(
            () -> {
              try {
                Thread.sleep(100);
                call.cancel();
              } catch (InterruptedException ignored) {
              }
            })
        .start();

    try {
      call.execute();
      fail();
    } catch (IOException e) {
      assertThat(e).isExactlyInstanceOf(IOException.class).hasMessage("canceled");
    }
  }

  @Test
  public void asyncCanBeCanceled() throws InterruptedException {
    behavior.setDelay(10, SECONDS);
    behavior.setVariancePercent(0);
    behavior.setFailurePercent(0);

    final Call<String> call = service.response();

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(
        new Callback<String>() {
          @Override
          public void onResponse(Call<String> call, Response<String> response) {
            latch.countDown();
          }

          @Override
          public void onFailure(Call<String> call, Throwable t) {
            failureRef.set(t);
            latch.countDown();
          }
        });

    // TODO we shouldn't need to sleep
    Thread.sleep(100); // Ensure the task has started.
    call.cancel();

    assertTrue(latch.await(1, SECONDS));
    assertThat(failureRef.get()).isExactlyInstanceOf(IOException.class).hasMessage("canceled");
  }

  @Test
  public void syncCanceledBeforeStart() throws IOException {
    behavior.setDelay(100, MILLISECONDS);
    behavior.setVariancePercent(0);
    behavior.setFailurePercent(0);

    final Call<String> call = service.response();

    call.cancel();
    try {
      call.execute();
      fail();
    } catch (IOException e) {
      assertThat(e).isExactlyInstanceOf(IOException.class).hasMessage("canceled");
    }
  }

  @Test
  public void asyncCanBeCanceledBeforeStart() throws InterruptedException {
    behavior.setDelay(10, SECONDS);
    behavior.setVariancePercent(0);
    behavior.setFailurePercent(0);

    final Call<String> call = service.response();
    call.cancel();

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(
        new Callback<String>() {
          @Override
          public void onResponse(Call<String> call, Response<String> response) {
            latch.countDown();
          }

          @Override
          public void onFailure(Call<String> call, Throwable t) {
            failureRef.set(t);
            latch.countDown();
          }
        });

    assertTrue(latch.await(1, SECONDS));
    assertThat(failureRef.get()).isExactlyInstanceOf(IOException.class).hasMessage("canceled");
  }
}
