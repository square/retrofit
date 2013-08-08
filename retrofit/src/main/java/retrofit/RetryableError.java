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

import java.util.Date;

/**
 * This exception is raised when the {@link retrofit.client.Response} is deemed to be retryable,
 * typically via an {@link retrofit.ErrorHandler} when the
 * {@link retrofit.client.Response#getStatus() status} is 503.
 */
public class RetryableError extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final Date retryAfter;

  public RetryableError(RetrofitError cause) {
    this(cause, null);
  }

  /**
   * @param retryAfter usually corresponds to the {@code Retry-After}
   *                   header. Can be null.
   */
  public RetryableError(RetrofitError cause, Date retryAfter) {
    super(cause);
    this.retryAfter = retryAfter;
  }

  /**
   * Sometimes corresponds to the {@code Retry-After} header
   * present in {@code 503} status. Other times parsed from an
   * application-specific response.  Null if unknown.
   */
  public Date retryAfter() {
    return retryAfter;
  }
}
