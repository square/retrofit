/*
 * Copyright (C) 2013 Square, Inc.
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

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import com.squareup.wire.Message;
import com.squareup.wire.Wire;
import java.io.IOException;
import java.io.InputStream;

final class WireConverter<T extends Message> implements Converter<T> {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/x-protobuf");

  private final Wire wire;
  private final Class<T> cls;

  public WireConverter(Wire wire, Class<T> cls) {
    this.wire = wire;
    this.cls = cls;
  }

  @Override public T fromBody(ResponseBody body) throws IOException {
    InputStream in = body.byteStream();
    try {
      return wire.parseFrom(in, cls);
    } finally {
      try {
        in.close();
      } catch (IOException ignored) {
      }
    }
  }

  @Override public RequestBody toBody(T value) {
    byte[] bytes = value.toByteArray();
    return RequestBody.create(MEDIA_TYPE, bytes);
  }
}
