// Copyright 2010 Square, Inc.
package retrofit.http;

/**
 * Communicates responses to server or offline requests. Contains a callback
 * method for each possible outcome. One and only one method will be invoked in
 * response to a given request.
 *
 * @param <T> expected response type
 * @author Bob Lee (bob@squareup.com)
 */
public interface Callback<T> {

  /**
   * Handles a response.
   *
   * @param t response
   */
  void call(T t);

  /**
   * The session expired or the account has been disabled. Prompt the user to
   * log in again.
   *
   * @param error message to show user, or null if no message was returned
   */
  void sessionExpired(ServerError error);

  /**
   * Couldn't reach the server. Check network settings and try again.
   */
  void networkError();

  /**
   * The server returned a client error. In most cases, this is a programming
   * error, but it can also signify a user input error.
   *
   * @param statusCode the HTTP response code, typically 4XX
   */
  void clientError(T response, int statusCode);

  /**
   * We reached the server, but it encountered an error (5xx) or its response
   * was unparseable. Please try again later.
   *
   * @param error message to show user, or null if no message was returned
   * @param statusCode the HTTP response code
   */
  void serverError(ServerError error, int statusCode);

  /**
   * An unexpected error occurred. Called if the framework throws an unexpected
   * exception or if the server returns a 400 (Bad Request) error. In either
   * case, the client software likely contains a bug; otherwise, the error
   * would have been caught sooner. The user should try updating their client.
   */
  void unexpectedError(Throwable t);


  /** JSON object for parsing server error responses. */
  static final class ServerError {
    public final String message;

    public ServerError(String message) {
      this.message = message;
    }

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ServerError that = (ServerError) o;
      return message == null ? that.message == null : message.equals(that.message);
    }

    @Override public int hashCode() {
      return message != null ? message.hashCode() : 0;
    }
  }
}
