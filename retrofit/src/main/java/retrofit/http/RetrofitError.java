// Copyright 2012 Square, Inc.
package retrofit.http;

import java.io.IOException;
import java.lang.reflect.Type;

public class RetrofitError extends RuntimeException {
  static RetrofitError networkError(String url, IOException exception) {
    return new RetrofitError(url, 0, null, null, null, null, true, exception);
  }

  static RetrofitError conversionError(String url, Converter converter, int statusCode,
      Header[] headers, byte[] body, Type successType, ConversionException exception) {
    return new RetrofitError(url, statusCode, headers, body, converter, successType, false,
        exception);
  }

  static RetrofitError httpError(String url, Converter converter, int statuCode, Header[] headers,
      byte[] body, Type successType) {
    return new RetrofitError(url, statuCode, headers, body, converter, successType, false, null);
  }

  static RetrofitError unexpectedError(String url, Throwable exception) {
    return new RetrofitError(url, 0, null, null, null, null, false, exception);
  }

  private final String url;
  private final Converter converter;
  private final int statusCode;
  private final Header[] headers;
  private final byte[] body;
  private final Type successType;
  private final boolean networkError;
  private final Throwable exception;

  private RetrofitError(String url, int statusCode, Header[] headers, byte[] body,
      Converter converter, Type successType, boolean networkError, Throwable exception) {
    this.url = url;
    this.converter = converter;
    this.statusCode = statusCode;
    this.headers = headers;
    this.body = body;
    this.successType = successType;
    this.networkError = networkError;
    this.exception = exception;
  }

  /** The request URL which produced the error. */
  public String getUrl() {
    return url;
  }

  /** HTTP status code of the response or 0 if no response received. */
  public int getStatusCode() {
    return statusCode;
  }

  /** Whether or not this error was the result of a network error. */
  public boolean isNetworkError() {
    return networkError;
  }

  /** List of headers returning in the HTTP response, if any. */
  public Header[] getHeaders() {
    return headers;
  }

  /** Raw {@code byte[]} of the HTTP response body, if any. */
  public byte[] getRawBody() {
    return body;
  }

  /**
   * HTTP response body converted to the type declared by either the interface method return type or
   * the generic type of the supplied {@link Callback} parameter.
   */
  public Object getBody() {
    if (body == null) {
      return null;
    }
    try {
      return converter.to(body, successType);
    } catch (ConversionException e) {
      throw new RuntimeException(e);
    }
  }

  /** HTTP response body converted to specified {@code type}. */
  public Object getBodyAs(Type type) {
    if (body == null) {
      return null;
    }
    try {
      return converter.to(body, type);
    } catch (ConversionException e) {
      throw new RuntimeException(e);
    }
  }

  /** The exception which caused this error, if any. */
  public Throwable getException() {
    return exception;
  }
}