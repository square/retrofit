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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Applies service method argument's value to the {@linkplain RequestBuilder}.
 * Each {@code ParameterHandler} instance corresponds to a single parameter of the service method.
 * Instances are created by {@linkplain Factory a factory} which is
 * {@linkplain Retrofit.Builder#addParameterHandlerFactory(Factory) installed}
 * into the {@link Retrofit} instance.
 *
 * <p>This is an advanced extension point. For most users custom {@linkplain Converter}
 * or {@linkplain CallAdapter} should be sufficient.</p>
 *
 * @param <T> method parameter type
 */
public interface ParameterHandler<T> {
  /**
   * Apply parameter {@code value} to the {builder}.
   */
  void apply(RequestBuilder builder, T value) throws IOException;

  /**
   * Creates {@link ParameterHandler} instances based on annotations and parameter type.
   */
  interface Factory {
    /**
     * Returns a {@link ParameterHandler} instance for {@code annotation} and parameter
     * {@code type}, or null if {@code annotation} cannot be handled by this factory.
     * If this factory is designed to handle passed {@code annotation} but other parameters
     * (e.g. {@code type}) have incompatible values {@linkplain IllegalArgumentException}
     * should be thrown.
     */
    ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
        Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit);
  }
}
