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
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.ByteString;
import retrofit2.internal.WebSocket;
import retrofit2.internal.WebSocketCall;
import retrofit2.internal.WebSocketListener;

import static retrofit2.Converter.BYTESTRING_MESSAGE;
import static retrofit2.Converter.STRING_MESSAGE;

final class OkHttpWebSocketCall<InT, OutT> implements WebSocketCall<InT, OutT> {
  private final okhttp3.WebSocket.Factory rawWebSocketFactory;
  private final RequestFactory requestFactory;
  private final @Nullable Object[] args;
  private final Converter<ResponseBody, InT> inConverter;
  private final Converter<OutT, RequestBody> outConverter;

  OkHttpWebSocketCall(okhttp3.WebSocket.Factory rawWebSocketFactory, RequestFactory requestFactory,
      @Nullable Object[] args, Converter<ResponseBody, InT> inConverter,
      Converter<OutT, RequestBody> outConverter) {
    this.rawWebSocketFactory = rawWebSocketFactory;
    this.requestFactory = requestFactory;
    this.args = args;
    this.inConverter = inConverter;
    this.outConverter = outConverter;
  }

  @Override
  public WebSocket<OutT> connect(final WebSocketListener<InT, OutT> listener) {
    Request request;
    try {
      request = requestFactory.create(args);
    } catch (IOException e) {
      // TODO call on failure? but we don't have a WebSocket instance to use!
      return null;
    }
    final AtomicReference<WebSocket<OutT>> webSocketRef = new AtomicReference<>();
    okhttp3.WebSocket rawWebSocket =
        rawWebSocketFactory.newWebSocket(request, new okhttp3.WebSocketListener() {
          @Override public void onOpen(okhttp3.WebSocket webSocket, Response response) {
            listener.onOpen(webSocketRef.get(), response);
          }

          @Override public void onMessage(okhttp3.WebSocket webSocket, String text) {
            ResponseBody body = ResponseBody.create(STRING_MESSAGE, text);

            InT message;
            try {
              message = inConverter.convert(body);
            } catch (IOException e) {
              // TODO call on failure?
              return;
            }
            listener.onMessage(webSocketRef.get(), message);
          }

          @Override public void onMessage(okhttp3.WebSocket webSocket, ByteString bytes) {
            // TODO drop toByteArray with OkHttp 3.11: https://github.com/square/okhttp/pull/4115
            ResponseBody body = ResponseBody.create(BYTESTRING_MESSAGE, bytes.toByteArray());

            InT message;
            try {
              message = inConverter.convert(body);
            } catch (IOException e) {
              // TODO call on failure?
              return;
            }
            listener.onMessage(webSocketRef.get(), message);
          }

          @Override public void onClosing(okhttp3.WebSocket webSocket, int code, String reason) {
            listener.onClosing(webSocketRef.get(), code, reason);
          }

          @Override public void onClosed(okhttp3.WebSocket webSocket, int code, String reason) {
            listener.onClosed(webSocketRef.get(), code, reason);
          }

          @Override public void onFailure(okhttp3.WebSocket webSocket, Throwable t,
              @Nullable Response response) {
            listener.onFailure(webSocketRef.get(), t, response);
          }
        });
    WebSocket<OutT> webSocket = new OkHttpWebSocket<>(rawWebSocket, outConverter);
    webSocketRef.set(webSocket);
    return webSocket;
  }
}
