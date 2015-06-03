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

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.lang.reflect.Type;

final class OkHttpCall<T> implements Call<T> {
  private final Endpoint endpoint;
  private final Converter converter;
  private final OkHttpClient client;
  private final MethodInfo methodInfo;
  private final Object[] args;

  private volatile com.squareup.okhttp.Call rawCall;
  private boolean executed; // Guarded by this.

  OkHttpCall(Endpoint endpoint, Converter converter, OkHttpClient client, MethodInfo methodInfo,
      Object[] args) {
    this.endpoint = endpoint;
    this.converter = converter;
    this.client = client;
    this.methodInfo = methodInfo;
    this.args = args;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone") // We are a final type & this saves clearing state.
  @Override public OkHttpCall<T> clone() {
    return new OkHttpCall<>(endpoint, converter, client, methodInfo, args);
  }

  public void enqueue(final Callback<T> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already executed");
      executed = true;
    }

    com.squareup.okhttp.Call rawCall;
    try {
      rawCall = createRawCall();
    } catch (Throwable t) {
      callback.failure(t);
      return;
    }
    this.rawCall = rawCall;

    rawCall.enqueue(new com.squareup.okhttp.Callback() {
      private void callFailure(Throwable e) {
        try {
          callback.failure(e);
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }

      private void callSuccess(Response<T> response) {
        try {
          callback.success(response);
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }

      @Override public void onFailure(Request request, IOException e) {
        callFailure(e);
      }

      @Override public void onResponse(com.squareup.okhttp.Response rawResponse) {
        final Response<T> response;
        try {
          response = parseResponse(rawResponse);
        } catch (final Throwable e) {
          callFailure(e);
          return;
        }
        callSuccess(response);
      }
    });
  }

  public Response<T> execute() throws IOException {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already executed");
      executed = true;
    }

    final com.squareup.okhttp.Call rawCall = createRawCall();
    this.rawCall = rawCall;

    return parseResponse(rawCall.execute());
  }

  private com.squareup.okhttp.Call createRawCall() {
    String serverUrl = endpoint.url();
    RequestBuilder requestBuilder = new RequestBuilder(serverUrl, methodInfo, converter);
    requestBuilder.setArguments(args);
    Request request = requestBuilder.build();

    return client.newCall(request);
  }

  private Response<T> parseResponse(com.squareup.okhttp.Response rawResponse) throws IOException {
    ResponseBody rawBody = rawResponse.body();
    // Remove the body (the only stateful object) from the raw response so we can pass it along.
    rawResponse = rawResponse.newBuilder().body(null).build();

    T converted = null;
    ResponseBody body = null;

    try {
      int code = rawResponse.code();
      if (code < 200 || code >= 300) {
        // Buffer the entire body in the event of a non-2xx status to avoid future I/O.
        body = Utils.readBodyToBytesIfNecessary(rawBody);
      } else if (code != 204 && code != 205) {
        Type responseType = methodInfo.adapter.responseType();
        if (responseType == ResponseBody.class) {
          //noinspection unchecked
          converted = (T) Utils.readBodyToBytesIfNecessary(rawBody);
        } else {
          ExceptionCatchingRequestBody wrapped = new ExceptionCatchingRequestBody(rawBody);
          try {
            //noinspection unchecked
            converted = (T) converter.fromBody(wrapped, responseType);
          } catch (RuntimeException e) {
            // If the underlying input stream threw an exception, propagate that rather than
            // indicating that it was a conversion exception.
            if (wrapped.threwException()) {
              throw wrapped.getThrownException();
            }

            throw e;
          }
        }
      }
    } finally {
      rawBody.close();
    }

    return new Response<>(rawResponse, converted, body);
  }

  public void cancel() {
    com.squareup.okhttp.Call rawCall = this.rawCall;
    if (rawCall == null) {
      throw new IllegalStateException("enqueue or execute must be called first");
    }
    rawCall.cancel();
  }
}
