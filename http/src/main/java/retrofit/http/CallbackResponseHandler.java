// Copyright 2010 Square, Inc.
package retrofit.http;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.BufferedHttpEntity;
import retrofit.core.Callback;

/**
 * Support for response handlers that invoke {@link Callback}.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public abstract class CallbackResponseHandler<T>
    implements ResponseHandler<Void> {

  private static final Logger LOGGER =
      Logger.getLogger(CallbackResponseHandler.class.getName());

  private final Callback<T> callback;
  private final Gson gson;
  private String requestUrl; // Can be null.

  protected CallbackResponseHandler(Gson gson, Callback<T> callback) {
    this(gson, callback, null);
  }

  protected CallbackResponseHandler(Gson gson, Callback<T> callback, String requestUrl) {
    this.gson = gson;
    this.callback = callback;
    this.requestUrl = requestUrl;
  }

  /**
   * Parses the HTTP entity and creates an object that will be passed to
   * {@link Callback#call(T)}. Invoked in background thread.
   *
   * @param entity HTTP entity to read and parse, not null
   * @throws IOException if a network error occurs
   * @throws ServerException if the server returns an unexpected response
   * @throws RuntimeException if an unexpected error occurs
   * @return parsed response
   */
  protected abstract T parse(HttpEntity entity) throws IOException,
      ServerException;

  @SuppressWarnings("unchecked")
  public Void handleResponse(HttpResponse response) throws IOException {
    /*
     * Note: An IOException thrown from here (while downloading the HTTP
     * entity, for example) will propagate to the caller and be reported as a
     * network error.
     *
     * Callback methods actually execute in the main thread, so we don't
     * have to worry about unhandled exceptions thrown by them.
     */

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
        callback.call(parse(entity));
      } catch (ServerException e) {
        LOGGER.log(Level.WARNING, e.getMessage(), e);
        callback.serverError(null, statusCode);
      }
      return null;
    }

    // 5XX == server error
    if (statusCode >= 500) {
      if (entity != null) {
        // TODO: Use specified encoding.
        String body = new String(HttpClients.entityToBytes(entity), "UTF-8");
        LOGGER.fine("Server returned " + statusCode + ", "
            + statusLine.getReasonPhrase() + ". Body: " + body + ". Request url " + requestUrl);
        callback.serverError(parseServerMessage(statusCode, body), statusCode);
      } else {
        LOGGER.fine("Server returned " + statusCode + ", "
            + statusLine.getReasonPhrase() + ". Request url " + requestUrl);
        callback.serverError(null, statusCode);
      }
      return null;
    }

    // 4XX error
    if (entity != null) {
      /** Construct BufferedHttpEntity so that we can read it multiple times. */
      HttpEntity bufferedEntity = new BufferedHttpEntity(entity);
      // TODO: Use specified encoding.
      String body = new String(HttpClients.entityToBytes(bufferedEntity),
          "UTF-8");
      LOGGER.fine("Server returned " + statusCode + ", "
          + statusLine.getReasonPhrase() + ". Body: " + body + ". Request url " + requestUrl);
      try {
        callback.clientError(parse(bufferedEntity), statusCode);
      } catch (ServerException e) {
        LOGGER.log(Level.WARNING, e.getMessage(), e);
        callback.serverError(null, statusCode);
      }
    } else {
      LOGGER.fine("Server returned " + statusCode + ", "
          + statusLine.getReasonPhrase() + ". Request url " + requestUrl);
      callback.clientError(null, statusCode);
    }
    return null;
  }

  /**
   * Parses a server error message.
   */
  private String parseServerMessage(int statusCode, String body) {
    if (statusCode == HttpStatus.SC_BAD_GATEWAY || statusCode == HttpStatus.SC_GATEWAY_TIMEOUT
        || statusCode < 500) {
      try {
        ServerError serverError = gson.fromJson(body, ServerError.class);
        if (serverError != null) return serverError.message;
      } catch (Throwable t) {
        // The server error takes precedence.
        LOGGER.log(Level.WARNING, t.getMessage(), t);
      }
    }
    return null;
  }

  /**
   * Gson POJO for parsing server error messages.
   */
  static class ServerError {
    String message;
  }
}
