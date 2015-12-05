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

import retrofit2.Retrofit;

public final class MockRetrofit {
  public static MockRetrofit create(Retrofit retrofit) {
    return create(retrofit, NetworkBehavior.create());
  }

  public static MockRetrofit create(Retrofit retrofit, NetworkBehavior behavior) {
    return new MockRetrofit(retrofit, behavior);
  }

  private final Retrofit retrofit;
  private final NetworkBehavior behavior;

  public MockRetrofit(Retrofit retrofit, NetworkBehavior behavior) {
    this.retrofit = retrofit;
    this.behavior = behavior;
  }

  public Retrofit retrofit() {
    return retrofit;
  }

  public NetworkBehavior networkBehavior() {
    return behavior;
  }

  @SuppressWarnings("unchecked") // Single-interface proxy creation guarded by parameter safety.
  public <T> BehaviorDelegate<T> create(Class<T> service) {
    return new BehaviorDelegate<>(retrofit, behavior, service);
  }
}
