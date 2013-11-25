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

/**
 * Callback according to API 2.0 spec. Currently does not implement error handling methods
 */
public interface Callback2<T> {

  /** Successful HTTP response. */
  void success(T t);

  /**
   * Unsuccessful HTTP response due to network failure, non-2XX status code, or unexpected
   * exception. Under API 2.0 this is broken into several methods.
   */
  void failure(RetrofitError error);
}
