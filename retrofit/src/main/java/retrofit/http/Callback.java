// Copyright 2012 Square, Inc.
package retrofit.http;

/**
 * Communicates responses to server or offline requests. Contains a callback method for each
 * possible outcome. One and only one method will be invoked in response to a given request.
 *
 * @param <T> expected response type
 * @author Bob Lee (bob@squareup.com)
 */
public interface Callback<T> {

  /** Successful HTTP response. */
  void success(T t);

  /**
   * Unsuccessful HTTP response due to network failure, non-2XX status code, or unexpected
   * exception.
   */
  void failure(RetrofitError error);
}
