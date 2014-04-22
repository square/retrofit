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
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.converter.Converter;
import retrofit.mime.FormUrlEncodedTypedOutput;
import retrofit.mime.MultipartTypedOutput;
import retrofit.mime.TypedOutput;
import retrofit.mime.TypedString;
import retrofit.rpc.RpcRequest;

import static retrofit.RestMethodInfo.ParamUsage.QUERY;
import static retrofit.RestMethodInfo.ParamUsage.QUERY_MAP;
import static retrofit.RestMethodInfo.ParamUsage.RPC_PARAM;

final class RequestBuilder implements RequestInterceptor.RequestFacade {
  private final Converter converter;
  private final String[] paramNames;
  private final RestMethodInfo.ParamUsage[] paramUsages;
  private final String requestMethod;
  private final String methodName;
  private final RestMethodInfo.RequestType requestType;
  private final boolean isSynchronous;
  private final boolean isObservable;
  private final String apiUrl;
  private final String rpcRequestMethod;

  private final FormUrlEncodedTypedOutput formBody;
  private final MultipartTypedOutput multipartBody;
  private TypedOutput body;

  private String relativeUrl;
  private StringBuilder queryParams;
  private List<Header> headers;
  private boolean hasContentTypeHeader;

  RequestBuilder(String apiUrl, RestMethodInfo methodInfo, Converter converter) {
    this.apiUrl = apiUrl;
    this.converter = converter;

    paramNames = methodInfo.requestParamNames;
    paramUsages = methodInfo.requestParamUsage;
    requestMethod = methodInfo.requestMethod;
    isSynchronous = methodInfo.isSynchronous;
    isObservable = methodInfo.isObservable;
    rpcRequestMethod = methodInfo.rpcRequestMethod;

    if (methodInfo.headers != null) {
      headers = new ArrayList<Header>(methodInfo.headers);
    }
    hasContentTypeHeader = methodInfo.hasContentTypeHeader;

    relativeUrl = methodInfo.requestUrl;
    methodName = methodInfo.requestUrl;

    String requestQuery = methodInfo.requestQuery;
    if (requestQuery != null) {
      queryParams = new StringBuilder().append('?').append(requestQuery);
    }

    requestType = methodInfo.requestType;
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

      case JSON_RPC:
        formBody = null;
        multipartBody = null;
        break;
      default:
        throw new IllegalArgumentException("Unknown request type: " + methodInfo.requestType);
    }
  }

  @Override public void addHeader(String name, String value) {
    if (name == null) {
      throw new IllegalArgumentException("Header name must not be null.");
    }
    List<Header> headers = this.headers;
    if (headers == null) {
      this.headers = headers = new ArrayList<Header>(2);
    }
    headers.add(new Header(name, value));

    if ("Content-Type".equalsIgnoreCase(name)) {
      hasContentTypeHeader = true;
    }
  }

  @Override public void addPathParam(String name, String value) {
    addPathParam(name, value, true);
  }

  @Override public void addEncodedPathParam(String name, String value) {
    addPathParam(name, value, false);
  }

  private void addPathParam(String name, String value, boolean urlEncodeValue) {
    if (name == null) {
      throw new IllegalArgumentException("Path replacement name must not be null.");
    }
    if (value == null) {
      throw new IllegalArgumentException(
          "Path replacement \"" + name + "\" value must not be null.");
    }
    try {
      if (urlEncodeValue) {
        String encodedValue = URLEncoder.encode(String.valueOf(value), "UTF-8");
        // URLEncoder encodes for use as a query parameter. Path encoding uses %20 to
        // encode spaces rather than +. Query encoding difference specified in HTML spec.
        // Any remaining plus signs represent spaces as already URLEncoded.
        encodedValue = encodedValue.replace("+", "%20");
        relativeUrl = relativeUrl.replace("{" + name + "}", encodedValue);
      } else {
        relativeUrl = relativeUrl.replace("{" + name + "}", String.valueOf(value));
      }
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(
          "Unable to convert path parameter \"" + name + "\" value to UTF-8:" + value, e);
    }
  }

  @Override public void addQueryParam(String name, String value) {
    addQueryParam(name, value, true);
  }

  @Override public void addEncodedQueryParam(String name, String value) {
    addQueryParam(name, value, false);
  }

  private void addQueryParam(String name, String value, boolean urlEncodeValue) {
    if (name == null) {
      throw new IllegalArgumentException("Query param name must not be null.");
    }
    if (value == null) {
      throw new IllegalArgumentException("Query param \"" + name + "\" value must not be null.");
    }
    try {
      if (urlEncodeValue) {
        value = URLEncoder.encode(String.valueOf(value), "UTF-8");
      }
      StringBuilder queryParams = this.queryParams;
      if (queryParams == null) {
        this.queryParams = queryParams = new StringBuilder();
      }

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
    if (!isSynchronous && !isObservable) {
      count -= 1;
    }

    Map<String, Object> rpcParams = new HashMap<String, Object>();
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
        case ENCODED_PATH:
          if (value == null) {
            throw new IllegalArgumentException(
                "Path parameter \"" + name + "\" value must not be null.");
          }
          addEncodedPathParam(name, value.toString());
          break;
        case QUERY:
        case ENCODED_QUERY:
          if (value != null) { // Skip null values.
            boolean urlEncodeValue = paramUsage == QUERY;
            if (value instanceof Iterable) {
              for (Object iterableValue : (Iterable<?>) value) {
                if (iterableValue != null) { // Skip null values
                  addQueryParam(name, iterableValue.toString(), urlEncodeValue);
                }
              }
            } else if (value.getClass().isArray()) {
              for (int x = 0, arrayLength = Array.getLength(value); x < arrayLength; x++) {
                Object arrayValue = Array.get(value, x);
                if (arrayValue != null) { // Skip null values
                  addQueryParam(name, arrayValue.toString(), urlEncodeValue);
                }
              }
            } else {
              addQueryParam(name, value.toString(), urlEncodeValue);
            }
          }
          break;
        case QUERY_MAP:
        case ENCODED_QUERY_MAP:
          if (value != null) { // Skip null values.
            boolean urlEncodeValue = paramUsage == QUERY_MAP;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
              Object entryValue = entry.getValue();
              if (entryValue != null) { // Skip null values.
                addQueryParam(entry.getKey().toString(), entryValue.toString(), urlEncodeValue);
              }
            }
          }
          break;
        case HEADER:
          if (value != null) { // Skip null values.
            addHeader(name, value.toString());
          }
          break;
        case FIELD:
          if (value != null) { // Skip null values.
            if (value instanceof Iterable) {
              for (Object iterableValue : (Iterable<?>) value) {
                if (iterableValue != null) { // Skip null values.
                  formBody.addField(name, iterableValue.toString());
                }
              }
            } else if (value.getClass().isArray()) {
              for (int x = 0, arrayLength = Array.getLength(value); x < arrayLength; x++) {
                Object arrayValue = Array.get(value, x);
                if (arrayValue != null) { // Skip null values.
                  formBody.addField(name, arrayValue.toString());
                }
              }
            } else {
              formBody.addField(name, value.toString());
            }
          }
          break;
        case FIELD_MAP:
          if (value != null) { // Skip null values.
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
              Object entryValue = entry.getValue();
              if (entryValue != null) { // Skip null values.
                formBody.addField(entry.getKey().toString(), entryValue.toString());
              }
            }
          }
          break;
        case PART:
          if (value != null) { // Skip null values.
            if (value instanceof TypedOutput) {
              multipartBody.addPart(name, (TypedOutput) value);
            } else if (value instanceof String) {
              multipartBody.addPart(name, new TypedString((String) value));
            } else {
              multipartBody.addPart(name, converter.toBody(value));
            }
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

        case RPC_PARAM:
          rpcParams.put(name, value);
          break;

        default:
          throw new IllegalArgumentException("Unknown parameter usage: " + paramUsage);
      }
    }

    if (requestType == RestMethodInfo.RequestType.JSON_RPC) {
      body = converter.toBody(new RpcRequest(rpcRequestMethod, rpcParams));
    }
  }

  Request build() throws UnsupportedEncodingException {
    if (multipartBody != null && multipartBody.getPartCount() == 0) {
      throw new IllegalStateException("Multipart requests must contain at least one part.");
    }

    String apiUrl = this.apiUrl;
    StringBuilder url = new StringBuilder(apiUrl);
    if (apiUrl.endsWith("/")) {
      // We require relative paths to start with '/'. Prevent a double-slash.
      url.deleteCharAt(url.length() - 1);
    }

    if (relativeUrl != null) {
      url.append(relativeUrl);
    }

    StringBuilder queryParams = this.queryParams;
    if (queryParams != null) {
      url.append(queryParams);
    }

    TypedOutput body = this.body;
    if (body != null) {
      // Only add Content-Type header from the body if one is not already set.
      if (!hasContentTypeHeader) {
        addHeader("Content-Type", body.mimeType());
      }

      // Only add Content-Length header from the body if it is known.
      long length = body.length();
      if (length != -1) {
        addHeader("Content-Length", String.valueOf(length));
      }
    }

    return new Request(requestMethod, url.toString(), headers, body);
  }
}
