/*
 * Copyright (C) 2026 Retrofit Authors
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
package retrofit2;

/**
 * Optional marker interface that keeps a Retrofit service available after R8 shrinking.
 * <p>
 * Extend this interface to prevent R8 from removing or merging a service interface, even if it
 * declares no HTTP methods or all its methods are removed as unused. The interface remains
 * available for {@link Retrofit#create}, while unused methods can still be removed.
 *
 * <pre><code>
 * public interface TestService extends RetrofitService {}
 *
 * TestService testService = retrofit.create(TestService.class);
 * </code></pre>
 */
public interface RetrofitService {}
