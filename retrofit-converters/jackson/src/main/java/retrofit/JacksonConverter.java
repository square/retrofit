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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.io.InputStream;

final class JacksonConverter<T> implements Converter<T> {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");

  private final ObjectWriter writer;
  private final ObjectReader reader;

  JacksonConverter(ObjectWriter writer, ObjectReader reader) {
    this.writer = writer;
    this.reader = reader;
  }


  @Override public T fromBody(ResponseBody body) throws IOException {
    InputStream is = body.byteStream();
    try {
      return reader.readValue(is);
    } finally {
      try {
        is.close();
      } catch (IOException ignored) {
      }
    }
  }

  @Override public RequestBody toBody(T value) {
    try {
      byte[] bytes = writer.writeValueAsBytes(value);
      return RequestBody.create(MEDIA_TYPE, bytes);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
