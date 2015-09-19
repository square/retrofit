package retrofit;

/** Controls the level of logging. */
public enum LogLevel {
  /** No logging. */
  NONE,
  /** Log only the request method and URL and the response status code and execution time. */
  BASIC,
  /** Log the basic information along with request and response headers. */
  HEADERS,
  /** Log the basic information along with request and response objects via toString(). */
  HEADERS_AND_ARGS,
  /**
   * Log the headers, body, and metadata for both requests and responses.
   * <p>
   * Note: This requires that the entire request and response body be buffered in memory!
   */
  FULL;

  public boolean log() {
    return this != NONE;
  }
}
