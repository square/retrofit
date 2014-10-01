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
package retrofit.client;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit.mime.TypedOutput;

/** Encapsulates all of the information necessary to make an HTTP request. */
public final class Request {
  private final String method;
  private final String url;
  private final List<Header> headers;
  private final TypedOutput body;

  public Request(@NotNull String method, @NotNull String url, @Nullable List<Header> headers,
      @Nullable TypedOutput body) {
    if (method == null) {
      throw new NullPointerException("Method must not be null.");
    }
    if (url == null) {
      throw new NullPointerException("URL must not be null.");
    }
    this.method = method;
    this.url = url;

    if (headers == null) {
      this.headers = Collections.emptyList();
    } else {
      this.headers = Collections.unmodifiableList(new ArrayList<Header>(headers));
    }

    this.body = body;
  }

  /** HTTP method verb. */
  @NotNull public String getMethod() {
    return method;
  }

  /** Target URL. */
  @NotNull public String getUrl() {
    return url;
  }

  /** Returns an unmodifiable list of headers, never {@code null}. */
  @NotNull public List<Header> getHeaders() {
    return headers;
  }

  /** Returns the request body or {@code null}. */
  @Nullable public TypedOutput getBody() {
    return body;
  }
}
