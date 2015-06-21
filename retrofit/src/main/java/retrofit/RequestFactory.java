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

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;

final class RequestFactory {
  private final String method;
  private final Endpoint endpoint;
  private final String pathUrl;
  private final String queryParams;
  private final Headers headers;
  private final MediaType mediaType;
  private final boolean hasBody;
  private final boolean isFormEncoded;
  private final boolean isMultipart;
  private final RequestBuilderAction[] requestBuilderActions;

  RequestFactory(String method, Endpoint endpoint, String pathUrl, String queryParams,
      Headers headers, MediaType mediaType, boolean hasBody, boolean isFormEncoded,
      boolean isMultipart, RequestBuilderAction[] requestBuilderActions) {
    this.method = method;
    this.endpoint = endpoint;
    this.pathUrl = pathUrl;
    this.queryParams = queryParams;
    this.headers = headers;
    this.mediaType = mediaType;
    this.hasBody = hasBody;
    this.isFormEncoded = isFormEncoded;
    this.isMultipart = isMultipart;
    this.requestBuilderActions = requestBuilderActions;
  }

  Request create(Object... args) {
    HttpUrl url = endpoint.url();
    RequestBuilder requestBuilder =
        new RequestBuilder(method, url, pathUrl, queryParams, headers, mediaType, hasBody,
            isFormEncoded, isMultipart);

    if (args != null) {
      RequestBuilderAction[] actions = requestBuilderActions;
      for (int i = 0, count = args.length; i < count; i++) {
        actions[i].perform(requestBuilder, args[i]);
      }
    }

    return requestBuilder.build();
  }
}
