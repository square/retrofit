// Copyright 2010 Square, Inc.
package retrofit.http;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import retrofit.core.Callback;

/**
 * Support for response handlers that invoke {@link Callback}.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public abstract class CallbackResponseHandler<T>
    implements ResponseHandler<Void> {

  private static final Logger logger =
      Logger.getLogger(CallbackResponseHandler.class.getName());

  private static final int UNAUTHORIZED = 401;
  private static final int FORBIDDEN = 403;
  private static final int BAD_GATEWAY = 502;
  private static final int GATEWAY_TIMEOUT = 504;

  private final UiCallback<T> callback;

  public CallbackResponseHandler(UiCallback<T> callback) {
    this.callback = callback;
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

    if (statusCode == UNAUTHORIZED) {
      logger.fine("Session expired.");
      callback.sessionExpired();
      return null;
    }

    if (statusCode == FORBIDDEN) {
      logger.fine("Account disabled.");
      callback.sessionExpired();
      return null;
    }

    HttpEntity entity = response.getEntity();

    // 2XX == successful request
    if (statusCode >= 200 && statusCode < 300) {
      if (entity == null) {
        logger.fine("Missing entity for " + statusCode + " response.");
        callback.serverError(null);
        return null;
      }

      try {
        callback.call(parse(entity));
      } catch (ServerException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
        callback.serverError(null);
      }
      return null;
    }

    // 5XX == server error
    if (statusCode >= 500) {
      if (entity != null) {
        // TODO: Use specified encoding.
        String body = new String(HttpClients.entityToBytes(entity), "UTF-8");
        logger.fine("Server returned " + statusCode + ", "
            + statusLine.getReasonPhrase() + ". Body: " + body);
        callback.serverError(parseServerMessage(statusCode, body));
      } else {
        logger.fine("Server returned " + statusCode + ", "
            + statusLine.getReasonPhrase() + ".");
        callback.serverError(null);
      }
      return null;
    }

    // 4XX error
    if (entity != null) {
      // TODO: Use specified encoding.
      String body = new String(HttpClients.entityToBytes(entity), "UTF-8");
      logger.fine("Server returned " + statusCode + ", "
          + statusLine.getReasonPhrase() + ". Body: " + body);
      try {
        callback.clientError(parse(entity));
      } catch (ServerException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
        callback.serverError(null);
      }
    } else {
      logger.fine("Server returned " + statusCode + ", "
          + statusLine.getReasonPhrase() + ".");
      callback.clientError(null);
    }
    return null;
  }

  /**
   * Parses a server error message.
   */
  private static String parseServerMessage(int statusCode, String body) {
    if (statusCode == BAD_GATEWAY || statusCode == GATEWAY_TIMEOUT
        || statusCode < 500) {
      try {
        ServerError serverError = new Gson().fromJson(body, ServerError.class);
        if (serverError != null) return serverError.message;
      } catch (Throwable t) {
        // The server error takes precedence.
        logger.log(Level.WARNING, t.getMessage(), t);
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
