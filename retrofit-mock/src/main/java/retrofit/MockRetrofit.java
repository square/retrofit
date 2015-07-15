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

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Creates {@link Call} instances which simulate the delay and error characteristics of a real
 * network.
 * <p>
 * Because APIs are defined as interfaces, versions of the API that use mock data can be created by
 * simply implementing the API interface on a class. These mock implementations execute
 * synchronously which is a large deviation from the behavior of those backed by an API call over
 * the network. By using the {@code Call} instances created using this class, the interface will
 * still use mock data but exhibit the delays and errors of a real network.
 * <p>
 * Example:
 * <pre>
 * public interface UserService {
 *   &#64;GET("/user/{id}")
 *   Call&lt;User> getUser(@Path("id") String userId);
 * }
 *
 * public class MockUserService implements UserService {
 *   private final MockRetrofit mock = // ...
 *
 *   &#64;Override public Call&lt;User> getUser(String userId) {
 *     return mock.newSuccessCall(new User("Jake"));
 *   }
 * }
 * </pre>
 */
public final class MockRetrofit {
  private static final int DEFAULT_DELAY_MS = 2000; // Network calls will take 2 seconds.
  private static final int DEFAULT_VARIANCE_PCT = 40; // Network delay varies by ±40%.
  private static final int DEFAULT_ERROR_PCT = 3; // 3% of network calls will fail.
  private static final int DEFAULT_ERROR_DELAY_FACTOR = 3; // Errors will be scaled by this value.

  /**
   * Create an instance with a normal {@link Retrofit} instance and an executor service on which
   * the simulated delays will be created. Instances of this class should be re-used so that the
   * behavior of every mock service is consistent.
   */
  public static MockRetrofit from(Retrofit restAdapter, ExecutorService backgroundExecutor) {
    return new MockRetrofit(restAdapter, backgroundExecutor);
  }

  private final ExecutorService backgroundExecutor;
  private final Executor callbackExecutor;

  final Random random = new Random();

  private volatile int delayMs = DEFAULT_DELAY_MS;
  private volatile int variancePercent = DEFAULT_VARIANCE_PCT;
  private volatile int errorPercent = DEFAULT_ERROR_PCT;

  private MockRetrofit(Retrofit retrofit, ExecutorService backgroundExecutor) {
    this.backgroundExecutor = backgroundExecutor;
    this.callbackExecutor = retrofit.callbackExecutor();
  }

  /** Create a call which succeeds with {@code body} in its response. */
  public <T> Call<T> newSuccessCall(T body) {
    return newSuccessCall(Response.fakeSuccess(body));
  }

  /** Create a call which succeeds with {@code response}. */
  public <T> Call<T> newSuccessCall(Response<T> response) {
    return new MockCall<>(this, backgroundExecutor, callbackExecutor, response, null);
  }

  /** Create a call which fails with {@code e}.  */
  public <T> Call<T> newFailureCall(IOException e) {
    return new MockCall<>(this, backgroundExecutor, callbackExecutor, null, e);
  }

  /** Set the network round trip delay. */
  public void setDelay(long amount, TimeUnit unit) {
    if (amount < 0) {
      throw new IllegalArgumentException("Amount must be positive value.");
    }
    long delayMs = unit.toMillis(amount);
    if (delayMs > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Delay value too large. Max: " + Integer.MAX_VALUE);
    }
    this.delayMs = (int) delayMs;
  }

  /** The network round trip delay. */
  public long delay(TimeUnit unit) {
    return MILLISECONDS.convert(delayMs, unit);
  }

  /** Set the plus-or-minus variance percentage of the network round trip delay. */
  public void setVariancePercent(int variancePct) {
    if (variancePct < 0 || variancePct > 100) {
      throw new IllegalArgumentException("Variance percentage must be between 0 and 100.");
    }
    this.variancePercent = variancePct;
  }

  /** The plus-or-minus variance percentage of the network round trip delay. */
  public int variancePercent() {
    return variancePercent;
  }

  /** Set the percentage of calls to {@link #calculateIsFailure()} that return {@code true}. */
  public void setErrorPercent(int errorPct) {
    if (errorPct < 0 || errorPct > 100) {
      throw new IllegalArgumentException("Error percentage must be between 0 and 100.");
    }
    this.errorPercent = errorPct;
  }

  /** The percentage of calls to {@link #calculateIsFailure()} that return {@code true}. */
  public int errorPercent() {
    return errorPercent;
  }

  /**
   * Randomly determine whether this call should result in a network failure.
   * <p>
   * This method is exposed for implementing other, non-Retrofit services which exhibit similar
   * network behavior. Retrofit services automatically will exhibit network behavior when wrapped
   * using {@link #newSuccessCall} or {@link #newFailureCall}.
   */
  public boolean calculateIsFailure() {
    int randomValue = random.nextInt(100);
    return randomValue < errorPercent;
  }

  /**
   * Get the delay (in milliseconds) that should be used for triggering a network error.
   * <p>
   * Because we are triggering an error, use a random delay between 0 and three times the normal
   * network delay to simulate a flaky connection failing anywhere from quickly to slowly.
   * <p>
   * This method is exposed for implementing other, non-Retrofit services which exhibit similar
   * network behavior. Retrofit services automatically will exhibit network behavior when wrapped
   * using {@link #newSuccessCall} or {@link #newFailureCall}.
   */
  public int calculateDelayForError() {
    if (delayMs == 0) return 0;

    return random.nextInt(delayMs * DEFAULT_ERROR_DELAY_FACTOR);
  }

  /**
   * Get the delay (in milliseconds) that should be used for delaying a network call response.
   * <p>
   * This method is exposed for implementing other, non-Retrofit services which exhibit similar
   * network behavior. Retrofit services automatically will exhibit network behavior when wrapped
   * using {@link #newSuccessCall} or {@link #newFailureCall}.
   */
  public int calculateDelayForCall() {
    float delta = variancePercent / 100f; // e.g., 20 / 100f == 0.2f
    float lowerBound = 1f - delta; // 0.2f --> 0.8f
    float upperBound = 1f + delta; // 0.2f --> 1.2f
    float bound = upperBound - lowerBound; // 1.2f - 0.8f == 0.4f
    float delayPercent = (random.nextFloat() * bound) + lowerBound; // 0.8 + (rnd * 0.4)
    return (int) (delayMs * delayPercent);
  }
}
