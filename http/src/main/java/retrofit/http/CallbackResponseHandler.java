// Copyright 2012 Square, Inc.
package retrofit.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Support for response handlers that invoke {@link Callback}.
 *
 * @author Bob Lee (bob@squareup.com)
 * @author Jake Wharton (jw@squareup.com)
 */
public class CallbackResponseHandler<R, CE, SE> implements ResponseHandler<Void> {

  private static final Logger LOGGER = Logger.getLogger(CallbackResponseHandler.class.getName());

  private final Callback<R, CE, SE> callback;
  private final Type[] callbackTypes;
  private final Converter converter;
  private final String requestUrl; // Can be null.
  private final Date start;
  private final ThreadLocal<DateFormat> dateFormat;

  protected CallbackResponseHandler(Callback<R, CE, SE> callback, Type[] callbackTypes, Converter converter,
      String requestUrl, Date start, ThreadLocal<DateFormat> dateFormat) {
    this.callback = callback;
    this.callbackTypes = callbackTypes;
    this.converter = converter;
    this.requestUrl = requestUrl;
    this.start = start;
    this.dateFormat = dateFormat;
  }

  /**
   * Parses the HTTP entity and creates an object that will be passed to
   * {@link Callback#call(R)}. Invoked in background thread.
   *
   * @param entity HTTP entity to read and parse, not null
   * @param type destination object type which is guaranteed to match <T>
   * @return parsed response
   * @throws ConversionException if the server returns an unexpected response
   * @throws RuntimeException if an unexpected error occurs
   */
  protected <T> T parse(HttpEntity entity, Type type) throws ConversionException {
    if (LOGGER.isLoggable(Level.FINE)) {
      try {
        entity = HttpClients.copyAndLog(entity, requestUrl, start, dateFormat.get());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return converter.to(entity, type);
  }

  public Void handleResponse(HttpResponse response) throws IOException {
    // Note: An IOException thrown from here (while downloading the HTTP
    // entity, for example) will propagate to the caller and be reported as a
    // network error.
    //
    // Callback methods actually execute in the main thread, so we don't
    // have to worry about unhandled exceptions thrown by them.

    StatusLine statusLine = response.getStatusLine();
    int statusCode = statusLine.getStatusCode();

    if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
      LOGGER.fine("Session expired.  Request url " + requestUrl);
      callback.sessionExpired();
      return null;
    }

    HttpEntity entity = response.getEntity();

    // 2XX == successful request
    if (statusCode >= 200 && statusCode < 300) {
      if (entity == null) {
        LOGGER.fine("Missing entity for " + statusCode + " response.  Url " + requestUrl);
        callback.serverError(null, statusCode);
        return null;
      }

      try {
        R result = parse(entity, callbackTypes[0]);
        callback.call(result);
      } catch (ConversionException e) {
        LOGGER.log(Level.WARNING, e.getMessage(), e);
        callback.serverError(null, statusCode);
      }
      return null;
    }

    // 5XX == server error
    if (statusCode >= 500) {
      SE error = null;
      try {
        error = parse(entity, callbackTypes[2]);
        LOGGER.fine("Server returned " + statusCode + ", " + statusLine.getReasonPhrase() + ". Body: " + error
            + ". Request url " + requestUrl);
      } catch (ConversionException e) {
        LOGGER.log(Level.WARNING, e.getMessage(), e);
      }
      callback.serverError(error, statusCode);
      return null;
    }

    // 4XX error
    if (entity != null) {
      CE error = null;
      try {
        error = parse(entity, callbackTypes[1]);
        LOGGER.fine("Server returned " + statusCode + ", " + statusLine.getReasonPhrase() + ". Body: " + error
            + ". Request url " + requestUrl);
      } catch (ConversionException e) {
        LOGGER.log(Level.WARNING, e.getMessage(), e);
      }
      callback.clientError(error, statusCode);
      return null;
    }

    LOGGER.fine("Server returned " + statusCode + ", " + statusLine.getReasonPhrase() + ". Request url " + requestUrl);
    callback.clientError(null, statusCode);
    return null;
  }
}
