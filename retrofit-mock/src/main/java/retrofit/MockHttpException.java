// Copyright 2013 Square, Inc.
package retrofit;

import java.util.ArrayList;
import java.util.List;
import retrofit.client.Header;
import retrofit.client.Response;
import retrofit.converter.Converter;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

/**
 * An exception used to trigger the simulation of an HTTP error for mock services.
 *
 * @see MockRestAdapter
 */
public class MockHttpException extends RuntimeException {
  /** Create a new {@link MockHttpException} for HTTP 301 Moved Permanently. */
  public static MockHttpException newMovedPermanentely(String location, Object responseBody) {
    if (location == null || "".equals(location.trim())) {
      throw new IllegalArgumentException("Location must not be blank.");
    }
    return new MockHttpException(HTTP_MOVED_PERM, "Moved Permanently", responseBody)
        .withHeader("Location", location);
  }

  /** Create a new {@link MockHttpException} for HTTP 302 Moved Temporarily. */
  public static MockHttpException newMovedTemporarily(String location, Object responseBody) {
    if (location == null || "".equals(location.trim())) {
      throw new IllegalArgumentException("Location must not be blank.");
    }
    return new MockHttpException(HTTP_MOVED_TEMP, "Moved Temporarily", responseBody)
        .withHeader("Location", location);
  }

  /** Create a new {@link MockHttpException} for HTTP 400 Bad Request. */
  public static MockHttpException newBadRequest(Object responseBody) {
    return new MockHttpException(HTTP_BAD_REQUEST, "Bad Request", responseBody);
  }

  /** Create a new {@link MockHttpException} for HTTP 401 Unauthorized. */
  public static MockHttpException newUnauthorized(Object responseBody) {
    return new MockHttpException(HTTP_UNAUTHORIZED, "Unauthorized", responseBody);
  }

  /** Create a new {@link MockHttpException} for HTTP 403 Forbidden. */
  public static MockHttpException newForbidden(Object responseBody) {
    return new MockHttpException(HTTP_FORBIDDEN, "Forbidded", responseBody);
  }

  /** Create a new {@link MockHttpException} for HTTP 404 Not Found. */
  public static MockHttpException newNotFound(Object responseBody) {
    return new MockHttpException(HTTP_NOT_FOUND, "Not Found", responseBody);
  }

  /** Create a new {@link MockHttpException} for HTTP 500 Internal Server Error. */
  public static MockHttpException newInternalError(Object responseBody) {
    return new MockHttpException(HTTP_INTERNAL_ERROR, "Internal Server Error", responseBody);
  }

  final int code;
  final String reason;
  final Object responseBody;
  final List<Header> headers = new ArrayList<Header>(2);

  /**
   * Create a new HTTP exception.
   *
   * @param code HTTP status code to trigger. Must be 300 or higher.
   * @param reason HTTP status reason message.
   * @param responseBody Object to use as the contents of the response body.
   */
  public MockHttpException(int code, String reason, Object responseBody) {
    super("HTTP " + code + " " + reason);
    if (code < 300 || code > 599) {
      throw new IllegalArgumentException("Unsupported HTTP error code: " + code);
    }
    if (reason == null || "".equals(reason.trim())) {
      throw new IllegalArgumentException("Reason must not be blank.");
    }
    this.code = code;
    this.reason = reason;
    this.responseBody = responseBody;
  }

  /** Add a header to the response. */
  public MockHttpException withHeader(String name, String value) {
    if (name == null || "".equals(name.trim())) {
      throw new IllegalArgumentException("Header name must not be blank.");
    }
    if (value == null || "".equals(value.trim())) {
      throw new IllegalArgumentException("Header value must not be blank.");
    }
    headers.add(new Header(name, value));
    return this;
  }

  Response toResponse(Converter converter) {
    return new Response("", code, reason, headers, new MockTypedInput(converter, responseBody));
  }
}
