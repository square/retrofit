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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.converter.Converter;
import retrofit.mime.FormUrlEncodedTypedOutput;
import retrofit.mime.MultipartTypedOutput;
import retrofit.mime.TypedOutput;

/** Builds HTTP requests from Java method invocations. */
final class RequestBuilder {
  private final Converter converter;

  private RestMethodInfo methodInfo;
  private Object[] args;
  private String apiUrl;
  private List<retrofit.client.Header> headers;

  RequestBuilder(Converter converter) {
    this.converter = converter;
  }

  /** Supply cached method metadata info. */
  RequestBuilder methodInfo(RestMethodInfo methodDetails) {
    this.methodInfo = methodDetails;
    return this;
  }

  /** Base API url. */
  RequestBuilder apiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
    return this;
  }

  /** Arguments from method invocation. */
  RequestBuilder args(Object[] args) {
    this.args = args;
    return this;
  }

  /** A list of custom headers. */
  RequestBuilder headers(List<retrofit.client.Header> headers) {
    this.headers = headers;
    return this;
  }

  /**
   * Construct a {@link Request} from the supplied information. You <strong>must</strong> call
   * {@link #methodInfo}, {@link #apiUrl}, {@link #args}, and {@link #headers} before invoking this
   * method.
   */
  Request build() throws UnsupportedEncodingException {
    String apiUrl = this.apiUrl;

    StringBuilder url = new StringBuilder(apiUrl);
    if (apiUrl.endsWith("/")) {
      // We require relative paths to start with '/'. Prevent a double-slash.
      url.deleteCharAt(url.length() - 1);
    }

    // Append the method relative URL.
    url.append(buildRelativeUrl());

    // Append query parameters, if needed.
    if (methodInfo.hasQueryParams) {
      boolean first = true;
      String requestQuery = methodInfo.requestQuery;
      if (requestQuery != null) {
        url.append('?').append(requestQuery);
        first = false;
      }
      String[] requestQueryName = methodInfo.requestQueryName;
      for (int i = 0; i < requestQueryName.length; i++) {
        String query = requestQueryName[i];
        if (query != null) {
          String value = URLEncoder.encode(String.valueOf(args[i]), "UTF-8");
          url.append(first ? '?' : '&').append(query).append('=').append(value);
          first = false;
        }
      }
    }

    List<retrofit.client.Header> headers = new ArrayList<retrofit.client.Header>();
    if (this.headers != null) {
      headers.addAll(this.headers);
    }
    List<Header> methodHeaders = methodInfo.headers;
    if (methodHeaders != null) {
      headers.addAll(methodHeaders);
    }
    // RFC 2616: Header names are case-insensitive.
    String[] requestParamHeader = methodInfo.requestParamHeader;
    if (requestParamHeader != null) {
      for (int i = 0; i < requestParamHeader.length; i++) {
        String name = requestParamHeader[i];
        if (name == null) continue;
        Object arg = args[i];
        if (arg != null) {
          headers.add(new retrofit.client.Header(name, String.valueOf(arg)));
        }
      }
    }

    return new Request(methodInfo.requestMethod, url.toString(), headers, buildBody());
  }

  /** Create the final relative URL by performing parameter replacement. */
  private String buildRelativeUrl() throws UnsupportedEncodingException {
    String replacedPath = methodInfo.requestUrl;
    String[] requestUrlParam = methodInfo.requestUrlParam;
    for (int i = 0; i < requestUrlParam.length; i++) {
      String param = requestUrlParam[i];
      if (param != null) {
        String value = URLEncoder.encode(String.valueOf(args[i]), "UTF-8");
        replacedPath = replacedPath.replace("{" + param + "}", value);
      }
    }
    return replacedPath;
  }

  /** Create the request body using the method info and invocation arguments. */
  private TypedOutput buildBody() {
    switch (methodInfo.requestType) {
      case SIMPLE: {
        int bodyIndex = methodInfo.bodyIndex;
        if (bodyIndex == RestMethodInfo.NO_BODY) {
          return null;
        }
        Object body = args[bodyIndex];
        if (body instanceof TypedOutput) {
          return (TypedOutput) body;
        } else {
          return converter.toBody(body);
        }
      }

      case FORM_URL_ENCODED: {
        FormUrlEncodedTypedOutput body = new FormUrlEncodedTypedOutput();
        String[] requestFormFields = methodInfo.requestFormFields;
        for (int i = 0; i < requestFormFields.length; i++) {
          String name = requestFormFields[i];
          if (name != null) {
            body.addField(name, String.valueOf(args[i]));
          }
        }
        return body;
      }

      case MULTIPART: {
        MultipartTypedOutput body = new MultipartTypedOutput();
        String[] requestMultipartPart = methodInfo.requestMultipartPart;
        for (int i = 0; i < requestMultipartPart.length; i++) {
          String name = requestMultipartPart[i];
          if (name != null) {
            Object value = args[i];
            if (value instanceof TypedOutput) {
              body.addPart(name, (TypedOutput) value);
            } else {
              body.addPart(name, converter.toBody(value));
            }
          }
        }
        return body;
      }

      default:
        throw new IllegalArgumentException("Unknown request type " + methodInfo.requestType);
    }
  }
}
