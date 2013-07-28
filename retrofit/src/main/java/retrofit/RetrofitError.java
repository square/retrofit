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

import retrofit.client.Response;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;

import java.io.IOException;
import java.lang.reflect.Type;

public abstract class RetrofitError extends RuntimeException {
  public static RetrofitNetworkError networkError(String url, IOException exception) {
    return new RetrofitNetworkError(url, exception);
  }

  public static RetrofitConversionError conversionError(String url, Response response,
                                                        Converter converter, Type successType,
                                                        ConversionException exception) {
    return new RetrofitConversionError(url, response, converter, successType, exception);
  }

  public static RetrofitHttpError httpError(String url, Response response, Converter converter,
                                            Type successType) {
    return new RetrofitHttpError(url, response, converter, successType);
  }

  public static UnexpectedRetrofitError unexpectedError(String url, Throwable exception) {
    return new UnexpectedRetrofitError(url, exception);
  }

  private final String url;

  protected RetrofitError(String message, String url) {
    super(message);
    this.url = url;
  }

  protected RetrofitError(String url, Throwable exception) {
    super(exception);
    this.url = url;
  }

  protected RetrofitError(String message, String url, Throwable exception) {
    super(message, exception);
    this.url = url;
  }

  /**
   * The request URL which produced the error.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Response object containing status code, headers, body, etc. if present, null otherwise.
   *
   * @deprecated Will be moved to {@link RetrofitResponseError}.
   */
  @Deprecated
  public abstract Response getResponse();

  /**
   * Whether or not this error was the result of a network error.
   *
   * @deprecated Use the instanceof operator instead and test for a {@link RetrofitNetworkError}.
   */
  @Deprecated
  public abstract boolean isNetworkError();

  /**
   * HTTP response body converted to the type declared by either the interface method return type or
   * the generic type of the supplied {@link Callback} parameter. Null if no body is present.
   *
   * @deprecated Will be moved to {@link RetrofitResponseError}.
   */
  @Deprecated
  public abstract Object getBody();

  /**
   * HTTP response body converted to specified {@code type}. Null if no body is present.
   *
   * @deprecated Will be moved to {@link RetrofitResponseError}.
   */
  @Deprecated
  public abstract Object getBodyAs(Type type);
}
