// Copyright 2013 Square, Inc.
package retrofit;

/**
 * An exception used to trigger the simulation of an HTTP error for mock services.
 *
 * @see MockRestAdapter
 */
public class MockHttpException extends RuntimeException {
  public final int code;
  public final String reason;
  public final Object responseBody;

  /**
   * Create a new HTTP exception.
   *
   * @param code Corresponding HTTP status code to trigger.
   * @param responseBody Object to use as the contents of the response body.
   */
  public MockHttpException(int code, Object responseBody) {
    this(code, "", responseBody);
  }

  /**
   * Create a new HTTP exception.
   *
   * @param code HTTP status code to trigger.
   * @param reason HTTP status reason message.
   * @param responseBody Object to use as the contents of the response body.
   */
  public MockHttpException(int code, String reason, Object responseBody) {
    super("HTTP " + code + " " + reason);
    this.code = code;
    this.reason = reason;
    this.responseBody = responseBody;
  }
}
