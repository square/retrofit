/*
 * Copyright (C) 2018 Square, Inc.
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

import java.io.IOException;
import javax.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.internal.WebSocket;

import static retrofit2.Converter.BYTESTRING_MESSAGE;
import static retrofit2.Converter.STRING_MESSAGE;

final class OkHttpWebSocket<OutT> implements WebSocket<OutT> {
  private final okhttp3.WebSocket rawWebSocket;
  private final Converter<OutT, RequestBody> outConverter;

  OkHttpWebSocket(okhttp3.WebSocket rawWebSocket, Converter<OutT, RequestBody> outConverter) {
    this.rawWebSocket = rawWebSocket;
    this.outConverter = outConverter;
  }

  @Override public Request request() {
    return rawWebSocket.request();
  }

  @Override public long queueSize() {
    return rawWebSocket.queueSize();
  }

  @Override public boolean send(OutT item) {
    Buffer buffer = new Buffer();
    MediaType contentType;
    try {
      RequestBody body = outConverter.convert(item);
      body.writeTo(buffer);
      contentType = body.contentType();
    } catch (IOException e) {
      rawWebSocket.cancel();
      throw new RuntimeException("Failed to convert: " + item, e);
    }

    if (STRING_MESSAGE.equals(contentType)) {
      return rawWebSocket.send(buffer.readUtf8());
    } else if (BYTESTRING_MESSAGE.equals(contentType)) {
      return rawWebSocket.send(buffer.readByteString());
    }
    throw new IllegalStateException("Outgoing message converter "
        + outConverter
        + " returned RequestBody with invalid content type "
        + contentType
        + ". Converter.STRING_MESSAGE or Converter.BYTESTRING_MESSAGE are the only valid values.");
  }

  @Override public boolean close(int code, @Nullable String reason) {
    return rawWebSocket.close(code, reason);
  }

  @Override public void cancel() {
    rawWebSocket.cancel();
  }
}
