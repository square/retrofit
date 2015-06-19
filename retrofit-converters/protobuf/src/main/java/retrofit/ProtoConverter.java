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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.io.InputStream;

final class ProtoConverter<T extends MessageLite> implements Converter<T> {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/x-protobuf");

  private Parser<T> parser;

  ProtoConverter(Parser<T> parser) {
    this.parser = parser;
  }

  @Override public T fromBody(ResponseBody body) throws IOException {
    InputStream is = body.byteStream();
    try {
      return parser.parseFrom(is);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        is.close();
      } catch (IOException ignored) {
      }
    }
  }

  @Override public RequestBody toBody(T value) {
    byte[] bytes = value.toByteArray();
    return RequestBody.create(MEDIA_TYPE, bytes);
  }
}
