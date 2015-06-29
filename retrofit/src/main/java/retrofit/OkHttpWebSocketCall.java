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

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.ws.WebSocket.PayloadType;
import com.squareup.okhttp.ws.WebSocketListener;
import java.io.IOException;
import java.util.concurrent.Executor;
import okio.Buffer;
import okio.BufferedSource;

final class OkHttpWebSocketCall<I, O> implements WebSocketCall<I, O> {
  private final OkHttpClient client;
  private final RequestFactory requestFactory;
  private final Converter<I> incomingConverter;
  private final Converter<O> outgoingConverter;
  private final Executor callbackExecutor;
  private final Object[] args;

  private volatile com.squareup.okhttp.ws.WebSocketCall rawCall;
  private boolean executed; // Guarded by this.

  OkHttpWebSocketCall(OkHttpClient client, RequestFactory requestFactory,
      Converter<I> incomingConverter, Converter<O> outgoingConverter, Executor callbackExecutor,
      Object[] args) {
    this.client = client;
    this.requestFactory = requestFactory;
    this.incomingConverter = incomingConverter;
    this.outgoingConverter = outgoingConverter;
    this.callbackExecutor = callbackExecutor;
    this.args = args;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone") // We are a final type & this saves clearing state.
  @Override public OkHttpWebSocketCall<I, O> clone() {
    return new OkHttpWebSocketCall<>(client, requestFactory, incomingConverter, outgoingConverter,
        callbackExecutor, args);
  }

  @Override public void enqueue(final WebSocketCallback<I, O> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already executed");
      executed = true;
    }

    com.squareup.okhttp.ws.WebSocketCall rawCall;
    try {
      rawCall = com.squareup.okhttp.ws.WebSocketCall.create(client, requestFactory.create(args));
    } catch (Throwable t) {
      callback.failure(t);
      return;
    }
    this.rawCall = rawCall;

    rawCall.enqueue(new WebSocketListener() {
      @Override public void onOpen(com.squareup.okhttp.ws.WebSocket rawWebSocket,
          com.squareup.okhttp.Response rawResponse) {
        WebSocket<O> webSocket = new OkHttpWebSocket<>(rawWebSocket, outgoingConverter);
        final Response<WebSocket<O>> response = Response.success(webSocket, rawResponse);
        callbackExecutor.execute(new Runnable() {
          @Override public void run() {
            callback.connect(response);
          }
        });
      }

      @Override
      public void onFailure(final IOException e, com.squareup.okhttp.Response rawResponse) {
        callbackExecutor.execute(new Runnable() {
          @Override public void run() {
            try {
              callback.failure(e);
            } catch (Throwable t) {
              t.printStackTrace();
            }
          }
        });
      }

      @Override public void onMessage(final BufferedSource source, PayloadType type)
          throws IOException {
        ResponseBody body = new ResponseBody() {
          @Override public MediaType contentType() {
            return null;
          }
          @Override public long contentLength() throws IOException {
            return -1;
          }
          @Override public BufferedSource source() throws IOException {
            return source;
          }
        };
        final I message = incomingConverter.fromBody(body);
        callbackExecutor.execute(new Runnable() {
          @Override public void run() {
            try {
              callback.message(message);
            } catch (Throwable t) {
              t.printStackTrace();
            }
          }
        });
      }

      @Override public void onPong(Buffer payload) {
      }

      @Override public void onClose(int code, String reason) {
      }
    });
  }

  public void cancel() {
    com.squareup.okhttp.ws.WebSocketCall rawCall = this.rawCall;
    if (rawCall == null) {
      throw new IllegalStateException("enqueue or execute must be called first");
    }
    rawCall.cancel();
  }
}
