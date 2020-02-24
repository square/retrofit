/*
 * Copyright (C) 2020 Square, Inc.
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
package retrofit2.adapter.rxjava3;

import java.io.IOException;
import javax.annotation.Nullable;
import retrofit2.Response;

/** The result of executing an HTTP request. */
public final class Result<T> {
  @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
  public static <T> Result<T> error(Throwable error) {
    if (error == null) throw new NullPointerException("error == null");
    return new Result<>(null, error);
  }

  @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
  public static <T> Result<T> response(Response<T> response) {
    if (response == null) throw new NullPointerException("response == null");
    return new Result<>(response, null);
  }

  private final @Nullable Response<T> response;
  private final @Nullable Throwable error;

  private Result(@Nullable Response<T> response, @Nullable Throwable error) {
    this.response = response;
    this.error = error;
  }

  /**
   * The response received from executing an HTTP request. Only present when {@link #isError()} is
   * false, null otherwise.
   */
  public @Nullable Response<T> response() {
    return response;
  }

  /**
   * The error experienced while attempting to execute an HTTP request. Only present when {@link
   * #isError()} is true, null otherwise.
   *
   * <p>If the error is an {@link IOException} then there was a problem with the transport to the
   * remote server. Any other exception type indicates an unexpected failure and should be
   * considered fatal (configuration error, programming error, etc.).
   */
  public @Nullable Throwable error() {
    return error;
  }

  /** {@code true} if the request resulted in an error. See {@link #error()} for the cause. */
  public boolean isError() {
    return error != null;
  }
}
