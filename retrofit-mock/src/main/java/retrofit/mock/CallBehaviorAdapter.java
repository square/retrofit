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

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import retrofit.Call;
import retrofit.Retrofit;

public final class CallBehaviorAdapter implements NetworkBehavior.Adapter<Call<?>> {
  private final Executor callbackExecutor;
  private final ExecutorService backgroundExecutor;

  /**
   * Create an instance with a normal {@link Retrofit} instance and an executor service on which
   * the simulated delays will be created. Instances of this class should be re-used so that the
   * behavior of every mock service is consistent.
   */
  public CallBehaviorAdapter(Retrofit retrofit, ExecutorService backgroundExecutor) {
    this.callbackExecutor = retrofit.callbackExecutor();
    this.backgroundExecutor = backgroundExecutor;
  }

  @Override public Call<?> applyBehavior(NetworkBehavior behavior, Call<?> value) {
    return new BehaviorCall<>(behavior, backgroundExecutor, callbackExecutor, value);
  }
}
