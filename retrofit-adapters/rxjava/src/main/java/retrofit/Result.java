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
package retrofit;

import java.io.IOException;

import static retrofit.Utils.checkNotNull;

/** The result of executing an HTTP request. */
public final class Result<T> {
  public static <T> Result<T> error(Throwable error) {
    return new Result<>(null, null, checkNotNull(error, "error == null"));
  }

  public static <T> Result<T> response(Response<T> response, Retrofit retrofit) {
    return new Result<>(checkNotNull(response, "response == null"),
      checkNotNull(retrofit, "retrofit == null"), null);
  }

  private final Response<T> response;
  private final Retrofit retrofit;
  private final Throwable error;

  Result(Response<T> response, Retrofit retrofit, Throwable error) {
    this.response = response;
    this.retrofit = retrofit;
    this.error = error;
  }

  /**
   * The response received from executing an HTTP request. Only present when {@link #isError()} is
   * false, null otherwise.
   */
  public Response<T> response() {
    return response;
  }

  /**
   * The Retrofit used to make this request. Only present when {@link #isError()} is
   * false, null otherwise.
   */
  public Retrofit retrofit() {
    return retrofit;
  }

  /**
   * The error experienced while attempting to execute an HTTP request. Only present when {@link
   * #isError()} is true, null otherwise.
   * <p>
   * If the error is an {@link IOException} then there was a problem with the transport to the
   * remote server. Any other exception type indicates an unexpected failure and should be
   * considered fatal (configuration error, programming error, etc.).
   */
  public Throwable error() {
    return error;
  }

  /** {@code true} if the request resulted in an error. See {@link #error()} for the cause. */
  public boolean isError() {
    return error != null;
  }
}
