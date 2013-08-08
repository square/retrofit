/*
 * Copyright (C) 2013 Square, Inc.
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

/**
 * A hook allowing clients to customize error exceptions for synchronous requests.
 *
 * @author Sam Beran sberan@gmail.com
 */
public interface ErrorHandler {
  /**
   * Return a custom exception to be thrown for a {@link RetrofitError}. It is recommended that you
   * pass the supplied error as the cause to any new exceptions.
   * <p>
   * If the return exception is checked it must be declared to be thrown on the interface method.
   * <p>
   * Example usage:
   * <pre>
   * class MyErrorHandler implements ErrorHandler {
   *   &#64;Override public Throwable handleError(RetrofitError cause) {
   *     Response r = cause.getResponse();
   *     if (r != null && r.getStatus() == 401) {
   *       return new UnauthorizedException(cause);
   *     } else if (r.getStatus() == 500) {
   *       return new RetryableError(cause);
   *     }
   *     return cause;
   *   }
   * }
   * </pre>
   *
   * @param cause the original {@link RetrofitError} exception
   * @return Throwable an exception which will be thrown from the client interface method. Must not
   *         be {@code null}.  If the exception is retryable, throw an instance of
   *         {@link RetryableError}.
   */
  Throwable handleError(RetrofitError cause);

  /** An {@link ErrorHandler} which returns the original error. */
  ErrorHandler DEFAULT = new ErrorHandler() {
    @Override public Throwable handleError(RetrofitError cause) {
      return cause;
    }
  };
}
