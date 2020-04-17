/*
 * Copyright (C) 2017 Square, Inc.
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
package retrofit.converter.java8;

import java.io.IOException;
import java.util.Optional;
import okhttp3.ResponseBody;
import retrofit2.Converter;

final class OptionalConverter<T> implements Converter<ResponseBody, Optional<T>> {
  private final Converter<ResponseBody, T> delegate;

  OptionalConverter(Converter<ResponseBody, T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Optional<T> convert(ResponseBody value) throws IOException {
    return Optional.ofNullable(delegate.convert(value));
  }
}
