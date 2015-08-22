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

import java.util.Random;
import org.junit.Test;
import retrofit.mock.NetworkBehavior;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class NetworkBehaviorTest {
  private final NetworkBehavior behavior = NetworkBehavior.create(new Random(2847));

  @Test public void delayMustBePositive() {
    try {
      behavior.setDelay(-1, SECONDS);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Amount must be positive value.");
    }
  }

  @Test public void varianceRestrictsRange() {
    try {
      behavior.setVariancePercent(-13);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Variance percentage must be between 0 and 100.");
    }
    try {
      behavior.setVariancePercent(174);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Variance percentage must be between 0 and 100.");
    }
  }

  @Test public void failureRestrictsRange() {
    try {
      behavior.setFailurePercent(-13);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Failure percentage must be between 0 and 100.");
    }
    try {
      behavior.setFailurePercent(174);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Failure percentage must be between 0 and 100.");
    }
  }

  @Test public void failurePercentageIsAccurate() {
    behavior.setFailurePercent(0);
    for (int i = 0; i < 10000; i++) {
      assertThat(behavior.calculateIsFailure()).isFalse();
    }

    behavior.setFailurePercent(3);
    int failures = 0;
    for (int i = 0; i < 100000; i++) {
      if (behavior.calculateIsFailure()) {
        failures += 1;
      }
    }
    assertThat(failures).isEqualTo(2964); // ~3% of 100k
  }

  @Test public void delayVarianceIsAccurate() {
    behavior.setDelay(2, SECONDS);

    behavior.setVariancePercent(0);
    for (int i = 0; i < 100000; i++) {
      assertThat(behavior.calculateDelay(MILLISECONDS)).isEqualTo(2000);
    }

    behavior.setVariancePercent(40);
    long lowerBound = Integer.MAX_VALUE;
    long upperBound = Integer.MIN_VALUE;
    for (int i = 0; i < 100000; i++) {
      long delay = behavior.calculateDelay(MILLISECONDS);
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
}
