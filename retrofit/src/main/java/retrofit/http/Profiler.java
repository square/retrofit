// Copyright 2012 Square, Inc.
package retrofit.http;

/**
 * A hook allowing clients to log HTTP method times and response status codes.
 *
 * @author Eric Burke (eric@squareup.com)
 */
public interface Profiler<T> {

  /**
   * Invoked before an HTTP method call. The object returned by this method will be
   * passed to {@link #afterCall} when the call returns.
   * <p>
   * This method gives implementors the opportunity to include information that may
   * change during the server call in {@code afterCall} logic.
   */
  T beforeCall();

  /**
   * Invoked after an HTTP method completes. This is called from the
   * RestAdapter's background thread.
   *
   * @param requestInfo information about the originating HTTP request.
   * @param elapsedTime time in milliseconds it took the HTTP request to complete.
   * @param statusCode response status code.
   * @param beforeCallData the data returned by the corresponding {@link #beforeCall()}.
   */
  void afterCall(RequestInformation requestInfo, long elapsedTime, int statusCode,
      T beforeCallData);

  /** Information about the HTTP request. */
  public static final class RequestInformation {
    private final String method;
    private final String baseUrl;
    private final String relativePath;
    private final long contentLength;
    private final String contentType;

    public RequestInformation(String method, String baseUrl, String relativePath,
        long contentLength, String contentType) {
      this.method = method;
      this.baseUrl = baseUrl;
      this.relativePath = relativePath;
      this.contentLength = contentLength;
      this.contentType = contentType;
    }

    /** Returns the HTTP method of the originating request. */
    public String getMethod() {
      return method;
    }

    /** Returns the URL to which the originating request was sent. */
    public String getBaseUrl() {
      return baseUrl;
    }

    /** Returns the path relative to the base URL to which the originating request was sent. */
    public String getRelativePath() {
      return relativePath;
    }

    /** Returns the number of bytes in the originating request. */
    public long getContentLength() {
      return contentLength;
    }

    /** Returns the content type header value of the originating request. */
    public String getContentType() {
      return contentType;
    }
  }
}
