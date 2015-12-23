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

import com.squareup.moshi.JsonAdapter;
import java.io.IOException;
import okhttp3.ResponseBody;
import okio.BufferedSource;

final class MoshiResponseBodyConverter<T> implements Converter<ResponseBody, T> {
  private final JsonAdapter<T> adapter;

  MoshiResponseBodyConverter(JsonAdapter<T> adapter) {
    this.adapter = adapter;
  }

  @Override public T convert(ResponseBody value) throws IOException {
    BufferedSource source = value.source();
    try {
      return adapter.fromJson(source);
    } finally {
      if (source != null) {
        try {
          source.close();
        } catch (IOException ignored) {
        }
      }
    }
  }
}
