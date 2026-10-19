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
package retrofit2;

import static retrofit2.Utils.throwIfFatal;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Timeout;

final class OkHttpCall<T> implements Call<T> {
  private final RequestFactory requestFactory;
  private final Object instance;
  private final Object[] args;
  private final okhttp3.Call.Factory callFactory;
  private final Converter<ResponseBody, T> responseConverter;

  private volatile boolean canceled;

  private final AtomicBoolean executed = new AtomicBoolean();

  /**
   * One of: null (not yet created), an {@link okhttp3.Call} (success), or a {@link Throwable}
   * (creation failure). Once set to a non-null value, this reference never changes.
   */
  private final AtomicReference<Object> rawCallState = new AtomicReference<>();

  OkHttpCall(
      RequestFactory requestFactory,
      Object instance,
      Object[] args,
      okhttp3.Call.Factory callFactory,
      Converter<ResponseBody, T> responseConverter) {
    this.requestFactory = requestFactory;
    this.instance = instance;
    this.args = args;
    this.callFactory = callFactory;
    this.responseConverter = responseConverter;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone") // We are a final type & this saves clearing state.
  @Override
  public OkHttpCall<T> clone() {
    return new OkHttpCall<>(requestFactory, instance, args, callFactory, responseConverter);
  }

  @Override
  public Request request() {
    try {
      return getRawCall().request();
    } catch (IOException e) {
      throw new RuntimeException("Unable to create request.", e);
    }
  }

  @Override
  public Timeout timeout() {
    try {
      return getRawCall().timeout();
    } catch (IOException e) {
      throw new RuntimeException("Unable to create call.", e);
    }
  }

  /**
   * Returns the raw call, initializing it if necessary. Throws if initializing the raw call throws,
   * or has thrown in previous attempts to create it.
   */
  private okhttp3.Call getRawCall() throws IOException {
    Object state = rawCallState.get();
    if (state instanceof okhttp3.Call) return (okhttp3.Call) state;
    if (state != null) throwStateFailure(state);

    try {
      okhttp3.Call call = createRawCall();
      if (rawCallState.compareAndSet(null, call)) return call;
      // Another thread resolved state concurrently — use their result.
      state = rawCallState.get();
      if (state instanceof okhttp3.Call) return (okhttp3.Call) state;
      throwStateFailure(state);
      throw new AssertionError(); // unreachable
    } catch (RuntimeException | Error | IOException e) {
      throwIfFatal(e); // Do not assign a fatal error to rawCallState.
      rawCallState.compareAndSet(null, e);
      throw e;
    }
  }

  private static void throwStateFailure(Object state) throws IOException {
    if (state instanceof IOException) throw (IOException) state;
    if (state instanceof RuntimeException) throw (RuntimeException) state;
    throw (Error) state;
  }

  @Override
  public void enqueue(final Callback<T> callback) {
    Objects.requireNonNull(callback, "callback == null");

    if (!executed.compareAndSet(false, true)) {
      throw new IllegalStateException("Already executed.");
    }

    okhttp3.Call call;
    Throwable failure;
    try {
      call = getRawCall();
      failure = null;
    } catch (Throwable t) {
      throwIfFatal(t);
      call = null;
      failure = t;
    }

    if (failure != null) {
      callback.onFailure(this, failure);
      return;
    }

    if (canceled) {
      call.cancel();
    }

    call.enqueue(
        new okhttp3.Callback() {
          @Override
          public void onResponse(okhttp3.Call call, okhttp3.Response rawResponse) {
            Response<T> response;
            try {
              response = parseResponse(rawResponse);
            } catch (Throwable e) {
              throwIfFatal(e);
              callFailure(e);
              return;
            }

            try {
              callback.onResponse(OkHttpCall.this, response);
            } catch (Throwable t) {
              throwIfFatal(t);
              t.printStackTrace(); // TODO this is not great
            }
          }

          @Override
          public void onFailure(okhttp3.Call call, IOException e) {
            callFailure(e);
          }

          private void callFailure(Throwable e) {
            try {
              callback.onFailure(OkHttpCall.this, e);
            } catch (Throwable t) {
              throwIfFatal(t);
              t.printStackTrace(); // TODO this is not great
            }
          }
        });
  }

  @Override
  public boolean isExecuted() {
    return executed.get();
  }

  @Override
  public Response<T> execute() throws IOException {
    if (!executed.compareAndSet(false, true)) {
      throw new IllegalStateException("Already executed.");
    }

    okhttp3.Call call = getRawCall();

    if (canceled) {
      call.cancel();
    }

    return parseResponse(call.execute());
  }

  private okhttp3.Call createRawCall() throws IOException {
    okhttp3.Call call = callFactory.newCall(requestFactory.create(instance, args));
    if (call == null) {
      throw new NullPointerException("Call.Factory returned null.");
    }
    return call;
  }

  Response<T> parseResponse(okhttp3.Response rawResponse) throws IOException {
    ResponseBody rawBody = rawResponse.body();

    // Remove the body's source (the only stateful object) so we can pass the response along.
    rawResponse =
        rawResponse
            .newBuilder()
            .body(new NoContentResponseBody(rawBody.contentType(), rawBody.contentLength()))
            .build();

    int code = rawResponse.code();
    if (code < 200 || code >= 300) {
      try {
        // Buffer the entire body to avoid future I/O.
        ResponseBody bufferedBody = Utils.buffer(rawBody);
        return Response.error(bufferedBody, rawResponse);
      } finally {
        rawBody.close();
      }
    }

    if (code == 204 || code == 205) {
      rawBody.close();
      return Response.success(null, rawResponse);
    }

    ExceptionCatchingResponseBody catchingBody = new ExceptionCatchingResponseBody(rawBody);
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

  @Override
  public void cancel() {
    canceled = true;

    Object state = rawCallState.get();
    if (state instanceof okhttp3.Call) {
      ((okhttp3.Call) state).cancel();
    }
  }

  @Override
  public boolean isCanceled() {
    if (canceled) {
      return true;
    }
    Object state = rawCallState.get();
    return state instanceof okhttp3.Call && ((okhttp3.Call) state).isCanceled();
  }

  static final class NoContentResponseBody extends ResponseBody {
    private final @Nullable MediaType contentType;
    private final long contentLength;

    NoContentResponseBody(@Nullable MediaType contentType, long contentLength) {
      this.contentType = contentType;
      this.contentLength = contentLength;
    }

    @Override
    public MediaType contentType() {
      return contentType;
    }

    @Override
    public long contentLength() {
      return contentLength;
    }

    @Override
    public BufferedSource source() {
      throw new IllegalStateException("Cannot read raw response body of a converted body.");
    }
  }

  static final class ExceptionCatchingResponseBody extends ResponseBody {
    private final ResponseBody delegate;
    private final BufferedSource delegateSource;
    @Nullable IOException thrownException;

    ExceptionCatchingResponseBody(ResponseBody delegate) {
      this.delegate = delegate;
      this.delegateSource =
          Okio.buffer(
              new ForwardingSource(delegate.source()) {
                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                  try {
                    return super.read(sink, byteCount);
                  } catch (IOException e) {
                    thrownException = e;
                    throw e;
                  }
                }
              });
    }

    @Override
    public MediaType contentType() {
      return delegate.contentType();
    }

    @Override
    public long contentLength() {
      return delegate.contentLength();
    }

    @Override
    public BufferedSource source() {
      return delegateSource;
    }

    @Override
    public void close() {
      delegate.close();
    }

    void throwIfCaught() throws IOException {
      if (thrownException != null) {
        throw thrownException;
      }
    }
  }
}
