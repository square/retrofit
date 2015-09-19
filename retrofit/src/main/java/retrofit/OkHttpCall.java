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
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;

import static retrofit.Utils.closeQuietly;

final class OkHttpCall<T> implements Call<T> {
  private final OkHttpClient client;
  private final RequestFactory requestFactory;
  private final Converter<ResponseBody, T> responseConverter;
  private final Object[] args;
  private final Logger logger;

  private volatile com.squareup.okhttp.Call rawCall;
  private boolean executed; // Guarded by this.
  private volatile boolean canceled;

  OkHttpCall(OkHttpClient client, RequestFactory requestFactory,
      Converter<ResponseBody, T> responseConverter, Logger logger, Object[] args) {
    this.client = client;
    this.requestFactory = requestFactory;
    this.responseConverter = responseConverter;
    this.logger = logger;
    this.args = args;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone") // We are a final type & this saves clearing state.
  @Override public OkHttpCall<T> clone() {
    return new OkHttpCall<>(client, requestFactory, responseConverter, logger, args);
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
      final long start = System.nanoTime();

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
        long elapsedTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        Response<T> response;
        try {
          response = parseResponse(rawResponse, elapsedTime);
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

    final long start = System.nanoTime();
    com.squareup.okhttp.Response response = rawCall.execute();
    long elapsedTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

    return parseResponse(response, elapsedTime);
  }

  private com.squareup.okhttp.Call createRawCall() {
    Request request = requestFactory.create(args);
    if (logger.level().log()) {
      request = logAndReplaceRequest(request, args);
    }
    return client.newCall(request);
  }

  private Request logAndReplaceRequest(Request request, Object[] args)  {
    logger.log(String.format("---> HTTP %s %s", request.method(), request.urlString()));

    if (logger.level().ordinal() >= LogLevel.HEADERS.ordinal()) {
      if (request.headers().size() != 0) {
        logger.log(request.headers().toString());
      }

      String bodySize = "no";
      RequestBody body = request.body();
      if (body != null) {
        final MediaType contentType = body.contentType();
        if (contentType != null) {
          logger.log("Content-Type: " + contentType);
        }

        long contentLength = -1;
        try {
          contentLength = body.contentLength();
        } catch (IOException e) {
          e.printStackTrace();
        }
        bodySize = contentLength + "-byte";
        if (contentLength != -1) {
          logger.log("Content-Length: " + contentLength);
        }

        if (logger.level().ordinal() >= LogLevel.FULL.ordinal()) {
          if (request.headers().size() != 0) {
            logger.log("");
          }
          try {
            final Buffer buffer = new Buffer();
            body.writeTo(buffer);
            logger.log(buffer.snapshot().utf8());
            request = request.newBuilder()
                .method(request.method(), new RequestBody() {
                  @Override public MediaType contentType() {
                    return contentType;
                  }

                  @Override public void writeTo(BufferedSink sink) throws IOException {
                    sink.write(buffer, buffer.size());
                  }
                })
                .build();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }

      if (logger.level().ordinal() >= LogLevel.HEADERS_AND_ARGS.ordinal()) {
        if (request.headers().size() != 0) {
          logger.log("---> REQUEST:");
        }
        for (int i = 0; i < args.length; i++) {
          logger.log("#" + i + ": " + args[i]);
        }
      }

      logger.log(String.format("---> END HTTP (%s request body)", bodySize));
    }

    return request;
  }

  private Response<T> parseResponse(com.squareup.okhttp.Response rawResponse, long elapsedTime)
      throws IOException {
    if (logger.level().log()) {
      rawResponse = logAndReplaceResponse(rawResponse, elapsedTime);
    }
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
      T body = responseConverter.convert(catchingBody);
      return Response.success(body, rawResponse);
    } catch (RuntimeException e) {
      // If the underlying source threw an exception, propagate that rather than indicating it was
      // a runtime exception.
      catchingBody.throwIfCaught();
      throw e;
    }
  }

  private com.squareup.okhttp.Response logAndReplaceResponse(com.squareup.okhttp.Response response,
      long elapsedTimed)
      throws IOException {
    logger.log(String.format("<--- HTTP %s (%sms)", response.code(), elapsedTimed));

    if (logger.level().ordinal() >= LogLevel.HEADERS.ordinal()) {
      if (response.headers().size() != 0) {
        logger.log(response.headers().toString());
      }

      long bodyLength = 0;
      ResponseBody body = response.body();
      if (body != null) {
        bodyLength = body.contentLength();

        if (logger.level().ordinal() >= LogLevel.FULL.ordinal()) {
          if (response.headers().size() != 0) {
            logger.log("");
          }

          final MediaType contentType = body.contentType();

          BufferedSource source = body.source();
          final Buffer buffer = new Buffer();
          buffer.writeAll(source);
          source.close();
          bodyLength = buffer.size();

          response = response.newBuilder()
              .body(new ResponseBody() {
                @Override public MediaType contentType() {
                  return contentType;
                }

                @Override public long contentLength() throws IOException {
                  return buffer.size();
                }

                @Override public BufferedSource source() throws IOException {
                  return buffer;
                }
              })
              .build();
          logger.log(buffer.snapshot().utf8());
        }
      }

      logger.log(String.format("<--- END HTTP (%s-byte response body)", bodyLength));
    }

    return response;
  }

  public void cancel() {
    canceled = true;
    com.squareup.okhttp.Call rawCall = this.rawCall;
    if (rawCall != null) {
      rawCall.cancel();
    }
  }
}
