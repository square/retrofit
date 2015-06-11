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

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.lang.reflect.Type;

import static retrofit.Utils.closeQueitly;

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
    HttpUrl url = endpoint.url();
    RequestBuilder requestBuilder = new RequestBuilder(url, methodInfo, converter);
    requestBuilder.setArguments(args);
    Request request = requestBuilder.build();

    return client.newCall(request);
  }

  private Response<T> parseResponse(com.squareup.okhttp.Response rawResponse) throws IOException {
    ResponseBody rawBody = rawResponse.body();
    // Remove the body (the only stateful object) so we can pass the response along.
    rawResponse = rawResponse.newBuilder().body(null).build();

    int code = rawResponse.code();
    if (code < 200 || code >= 300) {
      try {
        // Buffer the entire body to avoid future I/O.
        ResponseBody bufferedBody = Utils.readBodyToBytesIfNecessary(rawBody);
        return Response.error(bufferedBody, rawResponse);
      } finally {
        closeQueitly(rawBody);
      }
    }

    if (code == 204 || code == 205) {
      return Response.success(null, rawResponse);
    }

    Type responseType = methodInfo.adapter.responseType();
    if (responseType == ResponseBody.class) {
      if (methodInfo.isStreaming) {
        // Use the raw body from the request. The caller is responsible for closing.
        //noinspection unchecked
        return Response.success((T) rawBody, rawResponse);
      }

      try {
        // Buffer the entire body to avoid future I/O.
        ResponseBody bufferedBody = Utils.readBodyToBytesIfNecessary(rawBody);
        //noinspection unchecked
        return Response.success((T) bufferedBody, rawResponse);
      } finally {
        closeQueitly(rawBody);
      }
    }

    ExceptionCatchingRequestBody catchingBody = new ExceptionCatchingRequestBody(rawBody);
    try {
      //noinspection unchecked
      T body = (T) converter.fromBody(catchingBody, responseType);
      return Response.success(body, rawResponse);
    } catch (RuntimeException e) {
      // If the underlying source threw an exception, propagate that rather than indicating it was
      // a runtime exception.
      catchingBody.throwIfCaught();
      throw e;
    } finally {
      closeQueitly(rawBody);
    }
  }

  public void cancel() {
    com.squareup.okhttp.Call rawCall = this.rawCall;
    if (rawCall == null) {
      throw new IllegalStateException("enqueue or execute must be called first");
    }
    rawCall.cancel();
  }
}
