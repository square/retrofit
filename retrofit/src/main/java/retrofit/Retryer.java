/*
 * Copyright (C) 2012 Square, Inc.
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

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created for each invocation to {@link retrofit.client.Client#execute(retrofit.client.Request)}.
 * Implementations may keep state to determine if retry operations should
 * continue or not.
 */
public interface Retryer {

  interface Factory {
    Retryer create();
  }

  /**
   * if retry is permitted, return (possibly after sleeping). Otherwise
   * propagate the exception.
   */
  void continueOrPropagate(RetryableError e);

  /**
   * A {@link Factory} which looks at {@code Retry-After} header, if present.
   * Otherwise, exponentially backs off.
   */
  Factory DEFAULT_FACTORY = new Factory() {
    @Override public Retryer create() {
      return new Default();
    }
  };

  public static class Default implements Retryer {

    private final int maxAttempts;
    private final long period;
    private final long maxPeriod;

    // visible for testing;
    protected long currentTimeMillis() {
      return System.currentTimeMillis();
    }

    int attempt;
    long sleptForMillis;

    public Default() {
      this(100, SECONDS.toMillis(1), 5);
    }

    public Default(long period, long maxPeriod, int maxAttempts) {
      this.period = period;
      this.maxPeriod = maxPeriod;
      this.maxAttempts = maxAttempts;
      this.attempt = 1;
    }

    public void continueOrPropagate(RetryableError e) {
      if (attempt++ >= maxAttempts)
        throw e;

      long interval;
      if (e.retryAfter() != null) {
        interval = e.retryAfter().getTime() - currentTimeMillis();
        if (interval > maxPeriod)
          interval = maxPeriod;
        if (interval < 0)
          return;
      } else {
        interval = nextMaxInterval();
      }
      try {
        Thread.sleep(interval);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
      sleptForMillis += interval;
    }

    /**
     * Calculates the time interval to a retry attempt.
     * <br>
     * The interval increases exponentially with each attempt, at a rate of
     * nextInterval *= 1.5 (where 1.5 is the backoff factor), to the maximum
     * interval.
     *
     * @return time in nanoseconds from now until the next attempt.
     */
    long nextMaxInterval() {
      long interval = (long) (period * Math.pow(1.5, attempt - 1));
      return interval > maxPeriod ? maxPeriod : interval;
    }
  }
}
