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
package retrofit2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Adapts a {@link Call} into the type of {@code T}. Instances are created by {@linkplain Factory a
 * factory} which is {@linkplain Retrofit.Builder#addCallAdapterFactory(Factory) installed} into
 * the {@link Retrofit} instance.
 */
public interface CallAdapter<T> {
  /**
   * Returns the value type that this adapter uses when converting the HTTP response body to a Java
   * object. For example, the response type for {@code Call<Repo>} is {@code Repo}. This type
   * is used to prepare the {@code call} passed to {@code #adapt}.
   * <p>
   * Note: This is typically not the same type as the {@code returnType} provided to this call
   * adapter's factory.
   */
  Type responseType();

  /**
   * Returns an instance of {@code T} which delegates to {@code call}.
   * <p>
   * For example, given an instance for a hypothetical utility, {@code Async}, this instance would
   * return a new {@code Async<R>} which invoked {@code call} when run.
   * <pre>{@code
   * &#64;Override
   * public <R> Async<R> adapt(final Call<R> call) {
   *   return Async.create(new Callable<Response<R>>() {
   *     &#64;Override
   *     public Response<R> call() throws Exception {
   *       return call.execute();
   *     }
   *   });
   * }
   * }</pre>
   */
  <R> T adapt(Call<R> call);

  /**
   * Creates {@link CallAdapter} instances based on the return type of {@linkplain
   * Retrofit#create(Class) the service interface} methods.
   */
  interface Factory {
    /**
     * Returns a call adapter for interface methods that return {@code returnType}, or null if it
     * cannot be handled by this factory.
     */
    CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit);
  }
}
