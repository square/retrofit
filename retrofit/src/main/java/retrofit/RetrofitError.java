/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;

import retrofit.client.Response;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;

public class RetrofitError extends RuntimeException {
  public static RetrofitError networkError(String url, IOException exception) {
    return new RetrofitError(exception.getMessage(), url, null, null, null, true, exception);
  }

  public static RetrofitError conversionError(String url, Response response, Converter converter,
      Type successType, ConversionException exception) {
    return new RetrofitError(exception.getMessage(), url, response, converter, successType, false,
        exception);
  }

  public static RetrofitError httpError(String url, Response response, Converter converter,
      Type successType) {
    String message = response.getStatus() + " " + response.getReason();
    return new RetrofitError(message, url, response, converter, successType, false, null);
  }

  public static RetrofitError unexpectedError(String url, Throwable exception) {
    return new RetrofitError(exception.getMessage(), url, null, null, null, false, exception);
  }

  private final String url;
  private final Response response;
  private final Converter converter;
  private final Type successType;
  private final boolean networkError;

  RetrofitError(String message, String url, Response response, Converter converter,
      Type successType, boolean networkError, Throwable exception) {
    super(message, exception);
    this.url = url;
    this.response = response;
    this.converter = converter;
    this.successType = successType;
    this.networkError = networkError;
  }

  /** The request URL which produced the error. */
  @NotNull public String getUrl() {
    return url;
  }

  /** Response object containing status code, headers, body, etc. */
  @Nullable public Response getResponse() {
    return response;
  }

  /** Whether or not this error was the result of a network error. */
  public boolean isNetworkError() {
    return networkError;
  }

  /**
   * HTTP response body converted to the type declared by either the interface method return type or
   * the generic type of the supplied {@link Callback} parameter. {@code null} if there is no
   * response.
   */
  @Nullable public Object getBody() {
    return getBodyAs(successType);
  }

  /**
   * The type declared by either the interface method return type or the generic type of the
   * supplied {@link Callback} parameter.
   */
  public Type getSuccessType() {
    return successType;
  }

  /**
   * HTTP response body converted to specified {@code type}. {@code null} if there is no response.
   */
  @Nullable public Object getBodyAs(Type type) {
    if (response == null) {
      return null;
    }
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
}
