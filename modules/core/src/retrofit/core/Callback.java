// Copyright 2010 Square, Inc.
package retrofit.core;

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
   */
  void sessionExpired();

  /**
   * Couldn't reach the server. Check network settings and try again.
   */
  void networkError();

  /**
   * The server returned a client error. In most cases, this is a programming
   * error, but it can also signify a user input error.
   *
   * @return response object or null if server returned an empty response.
   */
  void clientError(T response);

  /**
   * We reached the server, but it encountered an error. Please try again
   * later.
   *
   * @param message to show user, or null if no message was returned
   */
  void serverError(String message);

  /**
   * An unexpected error occurred. Called if the framework throws an unexpected
   * exception or if the server returns a 400 (Bad Request) error. In either
   * case, the client software likely contains a bug; otherwise, the error
   * would have been caught sooner. The user should try updating their client.
   */
  void unexpectedError(Throwable t);
}
