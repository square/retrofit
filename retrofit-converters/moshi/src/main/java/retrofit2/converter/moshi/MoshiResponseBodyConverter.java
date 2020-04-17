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
package retrofit2.converter.moshi;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import java.io.IOException;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.ByteString;
import retrofit2.Converter;

final class MoshiResponseBodyConverter<T> implements Converter<ResponseBody, T> {
  private static final ByteString UTF8_BOM = ByteString.decodeHex("EFBBBF");

  private final JsonAdapter<T> adapter;

  MoshiResponseBodyConverter(JsonAdapter<T> adapter) {
    this.adapter = adapter;
  }

  @Override
  public T convert(ResponseBody value) throws IOException {
    BufferedSource source = value.source();
    try {
      // Moshi has no document-level API so the responsibility of BOM skipping falls to whatever
      // is delegating to it. Since it's a UTF-8-only library as well we only honor the UTF-8 BOM.
      if (source.rangeEquals(0, UTF8_BOM)) {
        source.skip(UTF8_BOM.size());
      }
      JsonReader reader = JsonReader.of(source);
      T result = adapter.fromJson(reader);
      if (reader.peek() != JsonReader.Token.END_DOCUMENT) {
        throw new JsonDataException("JSON document was not fully consumed.");
      }
      return result;
    } finally {
      value.close();
    }
  }
}
