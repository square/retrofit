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

import com.fasterxml.jackson.databind.ObjectReader;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.io.Reader;

final class JacksonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
  private final ObjectReader adapter;

  JacksonResponseBodyConverter(ObjectReader adapter) {
    this.adapter = adapter;
  }

  @Override public T convert(ResponseBody value) throws IOException {
    Reader reader = value.charStream();
    try {
      return adapter.readValue(reader);
    } finally {
      Utils.closeQuietly(reader);
    }
  }
}
