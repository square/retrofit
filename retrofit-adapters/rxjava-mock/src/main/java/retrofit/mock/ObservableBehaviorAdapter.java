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

import rx.Observable;
import rx.functions.Func1;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class ObservableBehaviorAdapter implements BehaviorAdapter<Observable<?>> {
  public static ObservableBehaviorAdapter create() {
    return new ObservableBehaviorAdapter();
  }

  private ObservableBehaviorAdapter() {
  }

  @Override public Observable<?> applyBehavior(final Behavior behavior, final Observable<?> value) {
    return Observable.timer(behavior.calculateDelay(MILLISECONDS), MILLISECONDS)
        .flatMap(new Func1<Long, Observable<?>>() {
          @Override public Observable<?> call(Long ignored) {
            if (behavior.calculateIsFailure()) {
              return Observable.error(behavior.failureException());
            }
            return value;
          }
        });
  }
}
