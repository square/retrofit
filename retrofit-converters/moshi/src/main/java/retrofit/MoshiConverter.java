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

import com.squareup.moshi.JsonAdapter;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import okio.Buffer;
import okio.BufferedSource;

final class MoshiConverter<T> implements Converter<T> {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");

  private JsonAdapter<T> adapter;

  MoshiConverter(JsonAdapter<T> adapter) {
    this.adapter = adapter;
  }

  @Override public T fromBody(ResponseBody body) throws IOException {
    BufferedSource source = body.source();
    try {
      return adapter.fromJson(source);
    } finally {
      try {
        source.close();
      } catch (IOException ignored) {
      }
    }
  }

  @Override public RequestBody toBody(T value) {
    Buffer buffer = new Buffer();
    try {
      adapter.toJson(buffer, value);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    return RequestBody.create(MEDIA_TYPE, buffer.snapshot());
  }
}
