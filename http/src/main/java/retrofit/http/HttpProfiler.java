// Copyright $today.year Square, Inc.
package retrofit.http;

/**
 * A hook allowing clients to log HTTP method times and response status codes.
 *
 * @author Eric Burke (eric@squareup.com)
 */
public interface HttpProfiler {

  /** The HTTP method. */
  public enum Method {
    DELETE,
    GET,
    HEAD,
    POST,
    PUT
  }

  /**
   * Invoked after an HTTP method completes. This is called from the
   * RestAdapter's background thread.
   *
   * @param method       the HTTP method (POST, GET, etc).
   * @param baseUrl      the URL that was called.
   * @param relativePath the path part of the URL.
   * @param elapsedTime  time in milliseconds.
   * @param statusCode   response status code.
   */
  void called(Method method, String baseUrl, String relativePath,
              long elapsedTime, int statusCode);
}
