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

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A simple emulation of the behavior of network calls.
 * <p>
 * This class models three properties of a network:
 * <ul>
 * <li>Delay – the time it takes before a response is received (successful or otherwise).</li>
 * <li>Variance – the amount of fluctuation of the delay to be faster or slower.</li>
 * <li>Failure - the percentage of operations which fail (such as {@link IOException}).</li>
 * </ul>
 * Behavior can be applied to a Retrofit interface with {@link MockRetrofit}. Behavior can also
 * be applied elsewhere using {@link #calculateDelay(TimeUnit)} and {@link #calculateIsFailure()}.
 * <p>
 * By default, instances of this class will use a 2 second delay with 40% variance and failures
 * will occur 3% of the time.
 */
public final class NetworkBehavior {
  private static final int DEFAULT_DELAY_MS = 2000; // Network calls will take 2 seconds.
  private static final int DEFAULT_VARIANCE_PERCENT = 40; // Network delay varies by ±40%.
  private static final int DEFAULT_FAILURE_PERCENT = 3; // 3% of network calls will fail.

  /** Applies {@link NetworkBehavior} to instances of {@code T}. */
  public interface Adapter<T> {
    /**
     * Apply {@code behavior} to {@code value} so that it exhibits the configured network behavior
     * traits when interacted with.
     */
    T applyBehavior(NetworkBehavior behavior, T value);
  }

  /** Create an instance with default behavior. */
  public static NetworkBehavior create() {
    return new NetworkBehavior(new Random());
  }

  /**
   * Create an instance with default behavior which uses {@code random} to control variance and
   * failure calculation.
   */
  public static NetworkBehavior create(Random random) {
    if (random == null) throw new NullPointerException("random == null");
    return new NetworkBehavior(random);
  }

  private final Random random;

  private volatile long delayMs = DEFAULT_DELAY_MS;
  private volatile int variancePercent = DEFAULT_VARIANCE_PERCENT;
  private volatile int failurePercent = DEFAULT_FAILURE_PERCENT;
  private volatile Throwable failureException = new IOException("Mock failure!");

  private NetworkBehavior(Random random) {
    this.random = random;
  }

  /** Set the network round trip delay. */
  public void setDelay(long amount, TimeUnit unit) {
    if (amount < 0) {
      throw new IllegalArgumentException("Amount must be positive value.");
    }
    this.delayMs = unit.toMillis(amount);
  }

  /** The network round trip delay. */
  public long delay(TimeUnit unit) {
    return MILLISECONDS.convert(delayMs, unit);
  }

  /** Set the plus-or-minus variance percentage of the network round trip delay. */
  public void setVariancePercent(int variancePercent) {
    if (variancePercent < 0 || variancePercent > 100) {
      throw new IllegalArgumentException("Variance percentage must be between 0 and 100.");
    }
    this.variancePercent = variancePercent;
  }

  /** The plus-or-minus variance percentage of the network round trip delay. */
  public int variancePercent() {
    return variancePercent;
  }

  /** Set the percentage of calls to {@link #calculateIsFailure()} that return {@code true}. */
  public void setFailurePercent(int failurePercent) {
    if (failurePercent < 0 || failurePercent > 100) {
      throw new IllegalArgumentException("Failure percentage must be between 0 and 100.");
    }
    this.failurePercent = failurePercent;
  }

  /** The percentage of calls to {@link #calculateIsFailure()} that return {@code true}. */
  public int failurePercent() {
    return failurePercent;
  }

  /** Set the exception to be used when a failure is triggered. */
  public void setFailureException(Throwable t) {
    if (t == null) {
      throw new NullPointerException("t == null");
    }
    this.failureException = t;
  }

  /** The exception to be used when a failure is triggered. */
  public Throwable failureException() {
    return failureException;
  }

  /**
   * Randomly determine whether this call should result in a network failure in accordance with
   * configured behavior. When true, {@link #failureException()} should be thrown.
   */
  public boolean calculateIsFailure() {
    int randomValue = random.nextInt(100);
    return randomValue < failurePercent;
  }

  /**
   * Get the delay that should be used for delaying a response in accordance with configured
   * behavior.
   */
  public long calculateDelay(TimeUnit unit) {
    float delta = variancePercent / 100f; // e.g., 20 / 100f == 0.2f
    float lowerBound = 1f - delta; // 0.2f --> 0.8f
    float upperBound = 1f + delta; // 0.2f --> 1.2f
    float bound = upperBound - lowerBound; // 1.2f - 0.8f == 0.4f
    float delayPercent = lowerBound + (random.nextFloat() * bound); // 0.8 + (rnd * 0.4)
    long callDelayMs = (long) (delayMs * delayPercent);
    return MILLISECONDS.convert(callDelayMs, unit);
  }
}
