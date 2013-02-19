// Copyright 2012 Square, Inc.
package retrofit.http;

import java.io.IOException;
import java.lang.reflect.Type;
import retrofit.http.client.Response;
import retrofit.io.TypedInput;

public class RetrofitError extends RuntimeException {
  static RetrofitError networkError(String url, IOException exception) {
    return new RetrofitError(url, null, null, null, true, exception);
  }

  static RetrofitError conversionError(String url, Response response, Converter converter,
      Type successType, ConversionException exception) {
    return new RetrofitError(url, response, converter, successType, false, exception);
  }

  static RetrofitError httpError(String url, Response response, Converter converter,
      Type successType) {
    return new RetrofitError(url, response, converter, successType, false, null);
  }

  static RetrofitError unexpectedError(String url, Throwable exception) {
    return new RetrofitError(url, null, null, null, false, exception);
  }

  private final String url;
  private final Response response;
  private final Converter converter;
  private final Type successType;
  private final boolean networkError;
  private final Throwable exception;

  private RetrofitError(String url, Response response, Converter converter, Type successType,
      boolean networkError, Throwable exception) {
    this.url = url;
    this.response = response;
    this.converter = converter;
    this.successType = successType;
    this.networkError = networkError;
    this.exception = exception;
  }

  /** The request URL which produced the error. */
  public String getUrl() {
    return url;
  }

  /** Response object containing status code, headers, body, etc. */
  public Response getResponse() {
    return response;
  }

  /** Whether or not this error was the result of a network error. */
  public boolean isNetworkError() {
    return networkError;
  }

  /**
   * HTTP response body converted to the type declared by either the interface method return type or
   * the generic type of the supplied {@link Callback} parameter.
   */
  public Object getBody() {
    TypedInput body = response.getBody();
    if (body == null) {
      return null;
    }
    try {
      return converter.fromBody(body, successType);
    } catch (ConversionException e) {
      throw new RuntimeException(e);
    }
  }

  /** HTTP response body converted to specified {@code type}. */
  public Object getBodyAs(Type type) {
    TypedInput body = response.getBody();
    if (body == null) {
      return null;
    }
    try {
      return converter.fromBody(body, type);
    } catch (ConversionException e) {
      throw new RuntimeException(e);
    }
  }

  /** The exception which caused this error, if any. */
  public Throwable getException() {
    return exception;
  }
}