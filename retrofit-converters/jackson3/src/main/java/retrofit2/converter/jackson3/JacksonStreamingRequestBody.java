/*
 * Copyright (C) 2025 Square, Inc.
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
package retrofit2.converter.jackson3;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectWriter;

import java.io.IOException;

final class JacksonStreamingRequestBody extends RequestBody {
  private final ObjectWriter adapter;
  private final Object value;
  private final MediaType mediaType;

  public JacksonStreamingRequestBody(ObjectWriter adapter, Object value, MediaType mediaType) {
    this.adapter = adapter;
    this.value = value;
    this.mediaType = mediaType;
  }

  @Override
  public MediaType contentType() {
    return mediaType;
  }

  @Override
  public void writeTo(BufferedSink sink) throws IOException {
    try {
      adapter.writeValue(sink.outputStream(), value);
    } catch (JacksonException e) {
      throw new IOException(e);
    }
  }
}
