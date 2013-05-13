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

import java.util.Collections;
import java.util.List;
import retrofit.client.Header;

/** Manages headers for each request. */
public interface RequestHeaders {
  /**
   * Get a list of headers for a request. This method will be called once for each request allowing
   * you to change the list as the state of your application changes.
   */
  List<Header> get();

  /** Empty header list. */
  RequestHeaders NONE = new RequestHeaders() {
    @Override public List<Header> get() {
      return Collections.emptyList();
    }
  };
}
