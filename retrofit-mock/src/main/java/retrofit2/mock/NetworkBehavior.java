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

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * A simple emulation of the behavior of network calls.
 *
 * <p>This class models three properties of a network:
 *
 * <ul>
 *   <li>Delay – the time it takes before a response is received (successful or otherwise).
 *   <li>Variance – the amount of fluctuation of the delay to be faster or slower.
 *   <li>Failure - the percentage of operations which fail (such as {@link IOException}).
 * </ul>
 *
 * Behavior can be applied to a Retrofit interface with {@link MockRetrofit}. Behavior can also be
 * applied elsewhere using {@link #calculateDelay(TimeUnit)} and {@link #calculateIsFailure()}.
 *
 * <p>By default, instances of this class will use a 2 second delay with 40% variance. Failures will
 * occur 3% of the time. HTTP errors will occur 0% of the time.
 */
public final class NetworkBehavior {
  private static final int DEFAULT_DELAY_MS = 2000; // Network calls will take 2 seconds.
  private static final int DEFAULT_VARIANCE_PERCENT = 40; // Network delay varies by ±40%.
  private static final int DEFAULT_FAILURE_PERCENT = 3; // 3% of network calls will fail.
  private static final int DEFAULT_ERROR_PERCENT = 0; // 0% of network calls will return errors.

  /** Create an instance with default behavior. */
  public static NetworkBehavior create() {
    return new NetworkBehavior(new Random());
  }

  /**
   * Create an instance with default behavior which uses {@code random} to control variance and
   * failure calculation.
   */
  @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
  public static NetworkBehavior create(Random random) {
    if (random == null) throw new NullPointerException("random == null");
    return new NetworkBehavior(random);
  }

  private final Random random;

  private volatile long delayMs = DEFAULT_DELAY_MS;
  private volatile int variancePercent = DEFAULT_VARIANCE_PERCENT;
  private volatile int failurePercent = DEFAULT_FAILURE_PERCENT;
  private volatile Throwable failureException;
  private volatile int errorPercent = DEFAULT_ERROR_PERCENT;
  private volatile Callable<Response<?>> errorFactory =
      () -> Response.error(500, ResponseBody.create(null, new byte[0]));

  private NetworkBehavior(Random random) {
    this.random = random;

    failureException = new MockRetrofitIOException();
    failureException.setStackTrace(new StackTraceElement[0]);
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
    checkPercentageValidity(variancePercent, "Variance percentage must be between 0 and 100.");
    this.variancePercent = variancePercent;
  }

  /** The plus-or-minus variance percentage of the network round trip delay. */
  public int variancePercent() {
    return variancePercent;
  }

  /** Set the percentage of calls to {@link #calculateIsFailure()} that return {@code true}. */
  public void setFailurePercent(int failurePercent) {
    checkPercentageValidity(failurePercent, "Failure percentage must be between 0 and 100.");
    this.failurePercent = failurePercent;
  }

  /** The percentage of calls to {@link #calculateIsFailure()} that return {@code true}. */
  public int failurePercent() {
    return failurePercent;
  }

  /**
   * Set the exception to be used when a failure is triggered.
   *
   * <p>It is a best practice to remove the stack trace from {@code exception} since it can
   * misleadingly point to code unrelated to this class.
   */
  @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
  public void setFailureException(Throwable exception) {
    if (exception == null) {
      throw new NullPointerException("exception == null");
    }
    this.failureException = exception;
  }

  /** The exception to be used when a failure is triggered. */
  public Throwable failureException() {
    return failureException;
  }

  /** The percentage of calls to {@link #calculateIsError()} that return {@code true}. */
  public int errorPercent() {
    return errorPercent;
  }

  /** Set the percentage of calls to {@link #calculateIsError()} that return {@code true}. */
  public void setErrorPercent(int errorPercent) {
    checkPercentageValidity(errorPercent, "Error percentage must be between 0 and 100.");
    this.errorPercent = errorPercent;
  }

  /**
   * Set the error response factory to be used when an error is triggered. This factory may only
   * return responses for which {@link Response#isSuccessful()} returns false.
   */
  @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
  public void setErrorFactory(Callable<Response<?>> errorFactory) {
    if (errorFactory == null) {
      throw new NullPointerException("errorFactory == null");
    }
    this.errorFactory = errorFactory;
  }

  /** The HTTP error to be used when an error is triggered. */
  public Response<?> createErrorResponse() {
    Response<?> call;
    try {
      call = errorFactory.call();
    } catch (Exception e) {
      throw new IllegalStateException("Error factory threw an exception.", e);
    }
    if (call == null) {
      throw new IllegalStateException("Error factory returned null.");
    }
    if (call.isSuccessful()) {
      throw new IllegalStateException("Error factory returned successful response.");
    }
    return call;
  }

  /**
   * Randomly determine whether this call should result in a network failure in accordance with
   * configured behavior. When true, {@link #failureException()} should be thrown.
   */
  public boolean calculateIsFailure() {
    return random.nextInt(100) < failurePercent;
  }

  /**
   * Randomly determine whether this call should result in an HTTP error in accordance with
   * configured behavior. When true, {@link #createErrorResponse()} should be returned.
   */
  public boolean calculateIsError() {
    return random.nextInt(100) < errorPercent;
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

  private static void checkPercentageValidity(int percentage, String message) {
    if (percentage < 0 || percentage > 100) {
      throw new IllegalArgumentException(message);
    }
  }
}
