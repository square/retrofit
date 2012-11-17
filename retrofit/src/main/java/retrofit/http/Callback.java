// Copyright 2012 Square, Inc.
package retrofit.http;

/**
 * Communicates responses to server or offline requests. Contains a callback
 * method for each possible outcome. One and only one method will be invoked in
 * response to a given request.
 *
 * @param <R> expected response type
 * @param <CE> client error response type
 * @param <SE> server error response type
 * @author Bob Lee (bob@squareup.com)
 */
public interface Callback<R, CE, SE> {

  /** Handles a response. */
  void call(R response);

  /**
   * The session expired or the account has been disabled. Prompt the user to
   * log in again.
   *
   * @param error message to show user, or null if no message was returned
   */
  void sessionExpired(CE error);

  /** Couldn't reach the server. Check network settings and try again. */
  void networkError();

  /**
   * The server returned a client error. In most cases, this is a programming
   * error, but it can also signify a user input error.
   *
   * @param statusCode the HTTP response code, typically 4XX
   */
  void clientError(CE response, int statusCode);

  /**
   * We reached the server, but it encountered an error (5xx) or its response
   * was unparseable. Please try again later.
   *
   * @param error message to show user, or null if no message was returned
   * @param statusCode the HTTP response code
   */
  void serverError(SE error, int statusCode);

  /**
   * An unexpected error occurred. Called if the framework throws an unexpected
   * exception or if the server returns a 400 (Bad Request) error. In either
   * case, the client software likely contains a bug; otherwise, the error
   * would have been caught sooner. The user should try updating their client.
   */
  void unexpectedError(Throwable t);
}
