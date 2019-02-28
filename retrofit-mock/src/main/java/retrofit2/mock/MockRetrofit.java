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
package retrofit2.mock;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import retrofit2.Retrofit;

public final class MockRetrofit {
  private final Retrofit retrofit;
  private final NetworkBehavior behavior;
  private final ExecutorService executor;

  MockRetrofit(Retrofit retrofit, NetworkBehavior behavior, ExecutorService executor) {
    this.retrofit = retrofit;
    this.behavior = behavior;
    this.executor = executor;
  }

  public Retrofit retrofit() {
    return retrofit;
  }

  public NetworkBehavior networkBehavior() {
    return behavior;
  }

  public Executor backgroundExecutor() {
    return executor;
  }

  @SuppressWarnings("unchecked") // Single-interface proxy creation guarded by parameter safety.
  public <T> BehaviorDelegate<T> create(Class<T> service) {
    return new BehaviorDelegate<>(retrofit, behavior, executor, service);
  }

  public static final class Builder {
    private final Retrofit retrofit;
    private @Nullable NetworkBehavior behavior;
    private @Nullable ExecutorService executor;

    @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
    public Builder(Retrofit retrofit) {
      if (retrofit == null) throw new NullPointerException("retrofit == null");
      this.retrofit = retrofit;
    }

    @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
    public Builder networkBehavior(NetworkBehavior behavior) {
      if (behavior == null) throw new NullPointerException("behavior == null");
      this.behavior = behavior;
      return this;
    }

    @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
    public Builder backgroundExecutor(ExecutorService executor) {
      if (executor == null) throw new NullPointerException("executor == null");
      this.executor = executor;
      return this;
    }

    public MockRetrofit build() {
      if (behavior == null) behavior = NetworkBehavior.create();
      if (executor == null) executor = Executors.newCachedThreadPool();
      return new MockRetrofit(retrofit, behavior, executor);
    }
  }
}
