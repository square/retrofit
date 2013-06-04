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

final class RequestBuilder implements RequestInterceptor.RequestFacade {
  private final Converter converter;
  private final List<Header> headers;
  private final StringBuilder queryParams;
  private final String[] paramNames;
  private final RestMethodInfo.ParamUsage[] paramUsages;
  private final String requestMethod;
  private final boolean isSynchronous;

  private final FormUrlEncodedTypedOutput formBody;
  private final MultipartTypedOutput multipartBody;
  private TypedOutput body;

  private String relativeUrl;
  private String apiUrl;

  RequestBuilder(Converter converter, RestMethodInfo methodInfo) {
    this.converter = converter;

    paramNames = methodInfo.requestParamNames;
    paramUsages = methodInfo.requestParamUsage;
    requestMethod = methodInfo.requestMethod;
    isSynchronous = methodInfo.isSynchronous;

    headers = new ArrayList<Header>();
    queryParams = new StringBuilder();

    relativeUrl = methodInfo.requestUrl;

    String requestQuery = methodInfo.requestQuery;
    if (requestQuery != null) {
      queryParams.append('?').append(requestQuery);
    }

    switch (methodInfo.requestType) {
      case FORM_URL_ENCODED:
        formBody = new FormUrlEncodedTypedOutput();
        multipartBody = null;
        body = formBody;
        break;
      case MULTIPART:
        formBody = null;
        multipartBody = new MultipartTypedOutput();
        body = multipartBody;
        break;
      case SIMPLE:
        formBody = null;
        multipartBody = null;
        // If present, 'body' will be set in 'setArguments' call.
        break;
      default:
        throw new IllegalArgumentException("Unknown request type: " + methodInfo.requestType);
    }
  }

  void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  @Override public void addHeader(String name, String value) {
    if (name == null) {
      throw new IllegalArgumentException("Header name must not be null.");
    }
    headers.add(new Header(name, value));
  }

  @Override public void addPathParam(String name, String value) {
    if (name == null) {
      throw new IllegalArgumentException("Path replacement name must not be null.");
    }
    if (value == null) {
      throw new IllegalArgumentException(
          "Path replacement \"" + name + "\" value must not be null.");
    }
    try {
      String encodedValue = URLEncoder.encode(String.valueOf(value), "UTF-8");
      relativeUrl = relativeUrl.replace("{" + name + "}", encodedValue);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(
          "Unable to convert path parameter \"" + name + "\" value to UTF-8:" + value, e);
    }
  }

  private void addQueryParam(String name, String value) {
    if (name == null) {
      throw new IllegalArgumentException("Query param name must not be null.");
    }
    if (value == null) {
      throw new IllegalArgumentException("Query param \"" + name + "\" value must not be null.");
    }
    try {
      value = URLEncoder.encode(String.valueOf(value), "UTF-8");
      StringBuilder queryParams = this.queryParams;
      queryParams.append(queryParams.length() > 0 ? '&' : '?');
      queryParams.append(name).append('=').append(value);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(
          "Unable to convert query parameter \"" + name + "\" value to UTF-8: " + value, e);
    }
  }

  void setArguments(Object[] args) {
    if (args == null) {
      return;
    }
    int count = args.length;
    if (!isSynchronous) {
      count -= 1;
    }
    for (int i = 0; i < count; i++) {
      String name = paramNames[i];
      Object value = args[i];
      RestMethodInfo.ParamUsage paramUsage = paramUsages[i];
      switch (paramUsage) {
        case PATH:
          if (value == null) {
            throw new IllegalArgumentException(
                "Path parameter \"" + name + "\" value must not be null.");
          }
          addPathParam(name, value.toString());
          break;
        case QUERY:
          if (value != null) { // Skip null values.
            addQueryParam(name, value.toString());
          }
          break;
        case HEADER:
          if (value != null) { // Skip null values.
            addHeader(name, value.toString());
          }
          break;
        case FIELD:
          if (value != null) { // Skip null values.
            formBody.addField(name, value.toString());
          }
          break;
        case PART:
          if (value == null) {
            throw new IllegalArgumentException(
                "Multipart part \"" + name + "\" value must not be null.");
          }
          if (value instanceof TypedOutput) {
            multipartBody.addPart(name, (TypedOutput) value);
          } else {
            multipartBody.addPart(name, converter.toBody(value));
          }
          break;
        case BODY:
          if (value == null) {
            throw new IllegalArgumentException("Body parameter value must not be null.");
          }
          if (value instanceof TypedOutput) {
            body = (TypedOutput) value;
          } else {
            body = converter.toBody(value);
          }
          break;
        default:
          throw new IllegalArgumentException("Unknown parameter usage: " + paramUsage);
      }
    }
  }

  Request build() throws UnsupportedEncodingException {
    String apiUrl = this.apiUrl;

    StringBuilder url = new StringBuilder(apiUrl);
    if (apiUrl.endsWith("/")) {
      // We require relative paths to start with '/'. Prevent a double-slash.
      url.deleteCharAt(url.length() - 1);
    }

    url.append(relativeUrl);

    StringBuilder queryParams = this.queryParams;
    if (queryParams.length() > 0) {
      url.append(queryParams);
    }

    return new Request(requestMethod, url.toString(), headers, body);
  }
}
