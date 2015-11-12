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
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import java.lang.reflect.Method;

final class RequestFactory {
  private final Method method;
  private final String httpMethod;
  private final BaseUrl baseUrl;
  private final String relativeUrl;
  private final Headers headers;
  private final MediaType contentType;
  private final boolean hasBody;
  private final boolean isFormEncoded;
  private final boolean isMultipart;
  private final RequestBuilderAction[] requestBuilderActions;
  // Should never include sensitive data such as query params, headers and so on.
  private volatile String toString;

  RequestFactory(Method method, String httpMethod, BaseUrl baseUrl, String relativeUrl,
      Headers headers, MediaType contentType, boolean hasBody, boolean isFormEncoded,
      boolean isMultipart, RequestBuilderAction[] requestBuilderActions) {
    this.method = method;
    this.httpMethod = httpMethod;
    this.baseUrl = baseUrl;
    this.relativeUrl = relativeUrl;
    this.headers = headers;
    this.contentType = contentType;
    this.hasBody = hasBody;
    this.isFormEncoded = isFormEncoded;
    this.isMultipart = isMultipart;
    this.requestBuilderActions = requestBuilderActions;
  }

  Request create(Object... args) {
    RequestBuilder requestBuilder =
        new RequestBuilder(httpMethod, baseUrl.url(), relativeUrl, headers, contentType, hasBody,
            isFormEncoded, isMultipart);

    if (args != null) {
      RequestBuilderAction[] actions = requestBuilderActions;
      if (actions.length != args.length) {
        throw new IllegalArgumentException("Argument count ("
            + args.length
            + ") doesn't match action count ("
            + actions.length
            + ")");
      }
      for (int i = 0, count = args.length; i < count; i++) {
        actions[i].perform(requestBuilder, args[i]);
      }
    }

    return requestBuilder.build();
  }

  @Override public String toString() {
    if (toString != null) {
      return toString;
    }

    StringBuilder parameters = new StringBuilder();
    Class[] parameterTypes = method.getParameterTypes();

    for (int i = 0; i < parameterTypes.length; i++) {
      parameters.append(parameterTypes[i].getSimpleName());
      if (i != parameterTypes.length - 1) {
        parameters.append(',');
      }
    }

    toString = method.getDeclaringClass().getSimpleName()
        + "."
        + method.getName()
        + "("
        + parameters
        + ")"
        + ", HTTP method = "
        + httpMethod
        + ", relative path template = "
        + relativeUrl;

    return toString;
  }
}
