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

import static retrofit.Utils.closeQuietly;

final class OkHttpCall<T> implements Call<T> {
  private final OkHttpClient client;
  private final RequestFactory requestFactory;
  private final Converter<T> responseConverter;
  private final Object[] args;

  private volatile com.squareup.okhttp.Call rawCall;
  private boolean executed; // Guarded by this.
  private volatile boolean canceled;

  OkHttpCall(OkHttpClient client, RequestFactory requestFactory, Converter<T> responseConverter,
      Object[] args) {
    this.client = client;
    this.requestFactory = requestFactory;
    this.responseConverter = responseConverter;
    this.args = args;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone") // We are a final type & this saves clearing state.
  @Override public OkHttpCall<T> clone() {
    return new OkHttpCall<>(client, requestFactory, responseConverter, args);
  }

  @Override public void enqueue(final Callback<T> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already executed");
      executed = true;
    }

    com.squareup.okhttp.Call rawCall;
    try {
      rawCall = createRawCall();
    } catch (Throwable t) {
      callback.onFailure(t);
      return;
    }
    if (canceled) {
      rawCall.cancel();
    }
    this.rawCall = rawCall;

    rawCall.enqueue(new com.squareup.okhttp.Callback() {
      private void callFailure(Throwable e) {
        try {
          callback.onFailure(e);
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }

      private void callSuccess(Response<T> response) {
        try {
          callback.onResponse(response);
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }

      @Override public void onFailure(Request request, IOException e) {
        callFailure(e);
      }

      @Override public void onResponse(com.squareup.okhttp.Response rawResponse) {
        Response<T> response;
        try {
          response = parseResponse(rawResponse);
        } catch (Throwable e) {
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

    com.squareup.okhttp.Call rawCall = createRawCall();
    if (canceled) {
      rawCall.cancel();
    }
    this.rawCall = rawCall;

    return parseResponse(rawCall.execute());
  }

  private com.squareup.okhttp.Call createRawCall() {
    return client.newCall(requestFactory.create(args));
  }

  private Response<T> parseResponse(com.squareup.okhttp.Response rawResponse) throws IOException {
    ResponseBody rawBody = rawResponse.body();

    // Remove the body's source (the only stateful object) so we can pass the response along.
    rawResponse = rawResponse.newBuilder()
        .body(new NoContentResponseBody(rawBody.contentType(), rawBody.contentLength()))
        .build();

    int code = rawResponse.code();
    if (code < 200 || code >= 300) {
      try {
        // Buffer the entire body to avoid future I/O.
        ResponseBody bufferedBody = Utils.readBodyToBytesIfNecessary(rawBody);
        return Response.error(bufferedBody, rawResponse);
      } finally {
        closeQuietly(rawBody);
      }
    }

    if (code == 204 || code == 205) {
      return Response.success(null, rawResponse);
    }

    ExceptionCatchingRequestBody catchingBody = new ExceptionCatchingRequestBody(rawBody);
    try {
      T body = responseConverter.fromBody(catchingBody);
      return Response.success(body, rawResponse);
    } catch (RuntimeException e) {
      // If the underlying source threw an exception, propagate that rather than indicating it was
      // a runtime exception.
      catchingBody.throwIfCaught();
      throw e;
    }
  }

  public void cancel() {
    canceled = true;
    com.squareup.okhttp.Call rawCall = this.rawCall;
    if (rawCall != null) {
      rawCall.cancel();
    }
  }
}
