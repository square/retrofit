/*
 * Copyright (C) 2016 Square, Inc.
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
package retrofit2.parameters;

import java.io.IOException;

import retrofit2.RequestBuilder;

/**
 * Applies a named value to the {@link RequestBuilder}.
 * The {@code name} is defined by parameter annotation or {@code Map} key when used with
 * {@link NamedParameterHandler} and {@link MapParameterHandler} respectively.
 * @param <T> type of the passed value
 */
public interface NamedValuesHandler<T> {
  void apply(RequestBuilder builder, String name, T value) throws IOException;
}
