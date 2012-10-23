// Copyright 2012 Square, Inc.
package retrofit.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Sends server call times and response status codes to {@link retrofit.http.HttpProfiler}. */
class ProfilingResponseHandler<T> implements ResponseHandler<Void> {
  private static final Logger LOGGER = Logger.getLogger(ProfilingResponseHandler.class.getSimpleName());

  private final ResponseHandler<Void> delegate;
  private final HttpProfiler<T> profiler;
  private final HttpProfiler.RequestInformation requestInfo;
  private final long startTime;
  private final AtomicReference<T> beforeCallData = new AtomicReference<T>();

  /** Wraps the delegate response handler. */
  ProfilingResponseHandler(ResponseHandler<Void> delegate, HttpProfiler<T> profiler,
      HttpProfiler.RequestInformation requestInfo, long startTime) {
    this.delegate = delegate;
    this.profiler = profiler;
    this.requestInfo = requestInfo;
    this.startTime = startTime;
  }

  public void beforeCall() {
    try {
      beforeCallData.set(profiler.beforeCall());
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error occurred in HTTP profiler beforeCall().", e);
    }
  }

  @Override public Void handleResponse(HttpResponse httpResponse) throws IOException {
    // Intercept the response and send data to profiler.
    long elapsedTime = System.currentTimeMillis() - startTime;
    int statusCode = httpResponse.getStatusLine().getStatusCode();

    try {
      profiler.afterCall(requestInfo, elapsedTime, statusCode, beforeCallData.get());
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error occurred in HTTP profiler afterCall().", e);
    }

    // Pass along the response to the normal handler.
    return delegate.handleResponse(httpResponse);
  }
}
