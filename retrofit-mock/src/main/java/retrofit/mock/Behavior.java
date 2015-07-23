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
package retrofit.mock;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class Behavior {
  private static final int DEFAULT_DELAY_MS = 2000; // Network calls will take 2 seconds.
  private static final int DEFAULT_VARIANCE_PERCENT = 40; // Network delay varies by ±40%.
  private static final int DEFAULT_FAILURE_PERCENT = 3; // 3% of network calls will fail.

  public static Behavior create() {
    return new Behavior(new Random());
  }

  public static Behavior create(Random random) {
    return new Behavior(random);
  }

  private final Random random;

  private volatile long delayMs = DEFAULT_DELAY_MS;
  private volatile int variancePercent = DEFAULT_VARIANCE_PERCENT;
  private volatile int failurePercent = DEFAULT_FAILURE_PERCENT;
  private volatile IOException failureException = new IOException("Mock failure!");

  private Behavior(Random random) {
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
  public void setFailureException(IOException exception) {
    if (exception == null) {
      throw new NullPointerException("exception == null");
    }
    this.failureException = exception;
  }

  /** The exception to be used when a failure is triggered. */
  public IOException failureException() {
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
