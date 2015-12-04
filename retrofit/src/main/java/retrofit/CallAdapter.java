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
package retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/** Adapts a {@link Call} into the type of {@code T}. */
public interface CallAdapter<T> {
  /**
   * Returns the value type that this adapter uses when converting the HTTP response body to a Java
   * object. For example, the response type for {@code Call<Repo>} is {@code Repo}. This type
   * is used to prepare the {@code call} passed to {@code #adapt}.
   *
   * <p>Note that this is typically not the same type as the {@code returnType} provided to
   * this call adapter's factory.
   */
  Type responseType();

  /** Returns an instance of the {@code T} which adapts the execution of {@code call}. */
  <R> T adapt(Call<R> call);

  /** Creates {@link CallAdapter} instances based on a desired type. */
  interface Factory {
    /**
     * Returns a call adapter for interface methods that return {@code returnType}, or null if it
     * cannot be handled by this factory.
     */
    CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit);
  }
}
