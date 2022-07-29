/*
 * Copyright (C) 2015 Square, Inc.
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
package retrofit2;

import okhttp3.Headers;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * An HTTP response.
 */
public final class Response<T> {
  private final okhttp3.Response rawResponse;
  private final @Nullable T body;
  private final @Nullable ResponseBody errorBody;
  private static final String RESPONSE_SUCCESS = "Response.success()";
  private static final String RESPONSE_ERROR = "Response.error()";
  private static final String HEADER_NULL = "headers == null";
  private static final String RAW_RESPONSE_NULL = "rawResponse == null";
  private static final String RAW_RESPONSE_SUCCESS = "rawResponse must be successful response";
  private static final String RAW_RESPONSE_NOT_SUCCESS = "rawResponse should not be successful response";
  private static final String BODY_NULL = "body == null";

  private Response(
    okhttp3.Response rawResponse, @Nullable T body, @Nullable ResponseBody errorBody) {
    this.rawResponse = rawResponse;
    this.body = body;
    this.errorBody = errorBody;
  }

  /**
   * Create a synthetic successful response with {@code body} as the deserialized body.
   */
  public static <T> Response<T> success(@Nullable T body) {
    return success(
      body,
      new okhttp3.Response.Builder() //
        .code(200)
        .message("OK")
        .protocol(Protocol.HTTP_1_1)
        .request(new Request.Builder().url("http://localhost/").build())
        .build());
  }

  /**
   * Create a synthetic successful response with an HTTP status code of {@code code} and {@code
   * body} as the deserialized body.
   */
  public static <T> Response<T> success(int code, @Nullable T body) {
    if (code < 200 || code >= 300) {
      throw new IllegalArgumentException(String.format("code < 200 or >= 300: %s", code));
    }

    return success(
      body,
      new okhttp3.Response.Builder() //
        .code(code)
        .message(RESPONSE_SUCCESS)
        .protocol(Protocol.HTTP_1_1)
        .request(new Request.Builder().url("http://localhost/").build())
        .build());
  }

  /**
   * Create a synthetic successful response using {@code headers} with {@code body} as the
   * deserialized body.
   */
  public static <T> Response<T> success(@Nullable T body, Headers headers) {
    Objects.requireNonNull(headers, HEADER_NULL);
    return success(
      body,
      new okhttp3.Response.Builder() //
        .code(200)
        .message("OK")
        .protocol(Protocol.HTTP_1_1)
        .headers(headers)
        .request(new Request.Builder().url("http://localhost/").build())
        .build());
  }

  /**
   * Create a successful response from {@code rawResponse} with {@code body} as the deserialized
   * body.
   */
  public static <T> Response<T> success(@Nullable T body, okhttp3.Response rawResponse) {
    Objects.requireNonNull(rawResponse, RAW_RESPONSE_NULL);
    if (!rawResponse.isSuccessful()) {
      throw new IllegalArgumentException(RAW_RESPONSE_SUCCESS);
    }
    return new Response<>(rawResponse, body, null);
  }

  /**
   * Create a synthetic error response with an HTTP status code of {@code code} and {@code body} as
   * the error body.
   */
  public static <T> Response<T> error(int code, ResponseBody body) {
    Objects.requireNonNull(body, BODY_NULL);
    if (code < 400) throw new IllegalArgumentException(String.format("code < 400: %s", code));
    return error(
      body,
      new okhttp3.Response.Builder() //
        .body(new OkHttpCall.NoContentResponseBody(body.contentType(), body.contentLength()))
        .code(code)
        .message(RESPONSE_ERROR)
        .protocol(Protocol.HTTP_1_1)
        .request(new Request.Builder().url("http://localhost/").build())
        .build());
  }

  /**
   * Create an error response from {@code rawResponse} with {@code body} as the error body.
   */
  public static <T> Response<T> error(ResponseBody body, okhttp3.Response rawResponse) {
    Objects.requireNonNull(body, BODY_NULL);
    Objects.requireNonNull(rawResponse, RAW_RESPONSE_NULL);
    if (rawResponse.isSuccessful()) {
      throw new IllegalArgumentException(RAW_RESPONSE_NOT_SUCCESS);
    }
    return new Response<>(rawResponse, null, body);
  }

  /**
   * The raw response from the HTTP client.
   */
  public okhttp3.Response raw() {
    return rawResponse;
  }

  /**
   * HTTP status code.
   */
  public int code() {
    return rawResponse.code();
  }

  /**
   * HTTP status message or null if unknown.
   */
  public String message() {
    return rawResponse.message();
  }

  /**
   * HTTP headers.
   */
  public Headers headers() {
    return rawResponse.headers();
  }

  /**
   * Returns true if {@link #code()} is in the range [200..300).
   */
  public boolean isSuccessful() {
    return rawResponse.isSuccessful();
  }

  /**
   * The deserialized response body of a {@linkplain #isSuccessful() successful} response.
   */
  public @Nullable T body() {
    return body;
  }

  /**
   * The raw response body of an {@linkplain #isSuccessful() unsuccessful} response.
   */
  public @Nullable ResponseBody errorBody() {
    return errorBody;
  }

  @Override
  public String toString() {
    return rawResponse.toString();
  }
}
