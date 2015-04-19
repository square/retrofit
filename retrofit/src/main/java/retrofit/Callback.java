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

import com.squareup.okhttp.Response;

/**
 * Communicates responses from a server or offline requests. One and only one method will be
 * invoked in response to a given request.
 * <p>
 * Callback methods are executed using the {@link RestAdapter} callback executor. When none is
 * specified, the following defaults are used:
 * <ul>
 * <li>Android: Callbacks are executed on the application's main (UI) thread.</li>
 * <li>JVM: Callbacks are executed on the background thread which performed the request.</li>
 * </ul>
 *
 * @param <T> expected response type
 * @see RestAdapter.Builder#callbackExecutor
 */
public interface Callback<T> {

  /** Successful HTTP response. */
  void success(T t, Response response);

  /**
   * Unsuccessful HTTP response due to network failure, non-2XX status code, or unexpected
   * exception.
   */
  void failure(RetrofitError error);
}
