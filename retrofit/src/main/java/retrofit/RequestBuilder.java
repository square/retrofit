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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.converter.Converter;
import retrofit.http.Body;
import retrofit.http.EncodedPath;
import retrofit.http.EncodedQuery;
import retrofit.http.EncodedQueryMap;
import retrofit.http.Field;
import retrofit.http.FieldMap;
import retrofit.http.Part;
import retrofit.http.PartMap;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.QueryMap;
import retrofit.mime.FormUrlEncodedTypedOutput;
import retrofit.mime.MultipartTypedOutput;
import retrofit.mime.TypedOutput;
import retrofit.mime.TypedString;

final class RequestBuilder implements RequestInterceptor.RequestFacade {
  private final Converter converter;
  private final Annotation[] paramAnnotations;
  private final String requestMethod;
  private final boolean isSynchronous;
  private final boolean isObservable;
  private final String apiUrl;

  private final FormUrlEncodedTypedOutput formBody;
  private final MultipartTypedOutput multipartBody;
  private TypedOutput body;

  private String relativeUrl;
  private StringBuilder queryParams;
  private List<Header> headers;
  private String contentTypeHeader;

  RequestBuilder(String apiUrl, RestMethodInfo methodInfo, Converter converter) {
    this.apiUrl = apiUrl;
    this.converter = converter;

    paramAnnotations = methodInfo.requestParamAnnotations;
    requestMethod = methodInfo.requestMethod;
    isSynchronous = methodInfo.isSynchronous;
    isObservable = methodInfo.isObservable;

    if (methodInfo.headers != null) {
      headers = new ArrayList<Header>(methodInfo.headers);
    }
    contentTypeHeader = methodInfo.contentTypeHeader;

    relativeUrl = methodInfo.requestUrl;

    String requestQuery = methodInfo.requestQuery;
    if (requestQuery != null) {
      queryParams = new StringBuilder().append('?').append(requestQuery);
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

  @Override public void addHeader(String name, String value) {
    if (name == null) {
      throw new IllegalArgumentException("Header name must not be null.");
    }
    if ("Content-Type".equalsIgnoreCase(name)) {
      contentTypeHeader = value;
      return;
    }

    List<Header> headers = this.headers;
    if (headers == null) {
      this.headers = headers = new ArrayList<Header>(2);
    }
    headers.add(new Header(name, value));
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
    addQueryParam(name, value, false, true);
  }

  @Override public void addEncodedQueryParam(String name, String value) {
    addQueryParam(name, value, false, false);
  }

  private void addQueryParam(String name, Object value, boolean encodeName, boolean encodeValue) {
    if (value instanceof Iterable) {
      for (Object iterableValue : (Iterable<?>) value) {
        if (iterableValue != null) { // Skip null values
          addQueryParam(name, iterableValue.toString(), encodeName, encodeValue);
        }
      }
    } else if (value.getClass().isArray()) {
      for (int x = 0, arrayLength = Array.getLength(value); x < arrayLength; x++) {
        Object arrayValue = Array.get(value, x);
        if (arrayValue != null) { // Skip null values
          addQueryParam(name, arrayValue.toString(), encodeName, encodeValue);
        }
      }
    } else {
      addQueryParam(name, value.toString(), encodeName, encodeValue);
    }
  }

  private void addQueryParam(String name, String value, boolean encodeName, boolean encodeValue) {
    if (name == null) {
      throw new IllegalArgumentException("Query param name must not be null.");
    }
    if (value == null) {
      throw new IllegalArgumentException("Query param \"" + name + "\" value must not be null.");
    }
    try {
      StringBuilder queryParams = this.queryParams;
      if (queryParams == null) {
        this.queryParams = queryParams = new StringBuilder();
      }

      queryParams.append(queryParams.length() > 0 ? '&' : '?');

      if (encodeName) {
        name = URLEncoder.encode(name, "UTF-8");
      }
      if (encodeValue) {
        value = URLEncoder.encode(value, "UTF-8");
      }
      queryParams.append(name).append('=').append(value);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(
          "Unable to convert query parameter \"" + name + "\" value to UTF-8: " + value, e);
    }
  }

  private void addQueryParamMap(int parameterNumber, Map<?, ?> map, boolean encodeNames,
      boolean encodeValues) {
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      Object entryKey = entry.getKey();
      if (entryKey == null) {
        throw new IllegalArgumentException(
            "Parameter #" + (parameterNumber + 1) + " query map contained null key.");
      }
      Object entryValue = entry.getValue();
      if (entryValue != null) { // Skip null values.
        addQueryParam(entryKey.toString(), entryValue, encodeNames, encodeValues);
      }
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
    for (int i = 0; i < count; i++) {
      Object value = args[i];

      Annotation annotation = paramAnnotations[i];
      Class<? extends Annotation> annotationType = annotation.annotationType();
      if (annotationType == Path.class) {
        Path path = (Path) annotation;
        String name = path.value();
        if (value == null) {
          throw new IllegalArgumentException(
              "Path parameter \"" + name + "\" value must not be null.");
        }
        addPathParam(name, value.toString(), path.encode());
      } else if (annotationType == EncodedPath.class) {
        String name = ((EncodedPath) annotation).value();
        if (value == null) {
          throw new IllegalArgumentException(
              "Path parameter \"" + name + "\" value must not be null.");
        }
        addPathParam(name, value.toString(), false);
      } else if (annotationType == Query.class) {
        if (value != null) { // Skip null values.
          Query query = (Query) annotation;
          addQueryParam(query.value(), value, query.encodeName(), query.encodeValue());
        }
      } else if (annotationType == EncodedQuery.class) {
        if (value != null) { // Skip null values.
          EncodedQuery query = (EncodedQuery) annotation;
          addQueryParam(query.value(), value, false, false);
        }
      } else if (annotationType == QueryMap.class) {
        if (value != null) { // Skip null values.
          QueryMap queryMap = (QueryMap) annotation;
          addQueryParamMap(i, (Map<?, ?>) value, queryMap.encodeNames(), queryMap.encodeValues());
        }
      } else if (annotationType == EncodedQueryMap.class) {
        if (value != null) { // Skip null values.
          addQueryParamMap(i, (Map<?, ?>) value, false, false);
        }
      } else if (annotationType == retrofit.http.Header.class) {
        if (value != null) { // Skip null values.
          String name = ((retrofit.http.Header) annotation).value();
          if (value instanceof Iterable) {
            for (Object iterableValue : (Iterable<?>) value) {
              if (iterableValue != null) { // Skip null values.
                addHeader(name, iterableValue.toString());
              }
            }
          } else if (value.getClass().isArray()) {
            for (int x = 0, arrayLength = Array.getLength(value); x < arrayLength; x++) {
              Object arrayValue = Array.get(value, x);
              if (arrayValue != null) { // Skip null values.
                addHeader(name, arrayValue.toString());
              }
            }
          } else {
            addHeader(name, value.toString());
          }
        }
      } else if (annotationType == Field.class) {
        if (value != null) { // Skip null values.
          Field field = (Field) annotation;
          String name = field.value();
          boolean encodeName = field.encodeName();
          boolean encodeValue = field.encodeValue();
          if (value instanceof Iterable) {
            for (Object iterableValue : (Iterable<?>) value) {
              if (iterableValue != null) { // Skip null values.
                formBody.addField(name, encodeName, iterableValue.toString(), encodeValue);
              }
            }
          } else if (value.getClass().isArray()) {
            for (int x = 0, arrayLength = Array.getLength(value); x < arrayLength; x++) {
              Object arrayValue = Array.get(value, x);
              if (arrayValue != null) { // Skip null values.
                formBody.addField(name, encodeName, arrayValue.toString(), encodeValue);
              }
            }
          } else {
            formBody.addField(name, encodeName, value.toString(), encodeValue);
          }
        }
      } else if (annotationType == FieldMap.class) {
        if (value != null) { // Skip null values.
          FieldMap fieldMap = (FieldMap) annotation;
          boolean encodeNames = fieldMap.encodeNames();
          boolean encodeValues = fieldMap.encodeValues();
          for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            Object entryKey = entry.getKey();
            if (entryKey == null) {
              throw new IllegalArgumentException(
                  "Parameter #" + (i + 1) + " field map contained null key.");
            }
            Object entryValue = entry.getValue();
            if (entryValue != null) { // Skip null values.
              formBody.addField(entryKey.toString(), encodeNames, entryValue.toString(),
                  encodeValues);
            }
          }
        }
      } else if (annotationType == Part.class) {
        if (value != null) { // Skip null values.
          String name = ((Part) annotation).value();
          String transferEncoding = ((Part) annotation).encoding();
          if (value instanceof TypedOutput) {
            multipartBody.addPart(name, transferEncoding, (TypedOutput) value);
          } else if (value instanceof String) {
            multipartBody.addPart(name, transferEncoding, new TypedString((String) value));
          } else {
            multipartBody.addPart(name, transferEncoding, converter.toBody(value));
          }
        }
      } else if (annotationType == PartMap.class) {
        if (value != null) { // Skip null values.
          String transferEncoding = ((PartMap) annotation).encoding();
          for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            Object entryKey = entry.getKey();
            if (entryKey == null) {
              throw new IllegalArgumentException(
                  "Parameter #" + (i + 1) + " part map contained null key.");
            }
            String entryName = entryKey.toString();
            Object entryValue = entry.getValue();
            if (entryValue != null) { // Skip null values.
              if (entryValue instanceof TypedOutput) {
                multipartBody.addPart(entryName, transferEncoding, (TypedOutput) entryValue);
              } else if (entryValue instanceof String) {
                multipartBody.addPart(entryName, transferEncoding,
                    new TypedString((String) entryValue));
              } else {
                multipartBody.addPart(entryName, transferEncoding, converter.toBody(entryValue));
              }
            }
          }
        }
      } else if (annotationType == Body.class) {
        if (value == null) {
          throw new IllegalArgumentException("Body parameter value must not be null.");
        }
        if (value instanceof TypedOutput) {
          body = (TypedOutput) value;
        } else {
          body = converter.toBody(value);
        }
      } else {
        throw new IllegalArgumentException(
            "Unknown annotation: " + annotationType.getCanonicalName());
      }
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

    url.append(relativeUrl);

    StringBuilder queryParams = this.queryParams;
    if (queryParams != null) {
      url.append(queryParams);
    }

    TypedOutput body = this.body;
    List<Header> headers = this.headers;
    if (contentTypeHeader != null) {
      if (body != null) {
        body = new MimeOverridingTypedOutput(body, contentTypeHeader);
      } else {
        Header header = new Header("Content-Type", contentTypeHeader);
        if (headers == null) {
          headers = Collections.singletonList(header);
        } else {
          headers.add(header);
        }
      }
    }

    return new Request(requestMethod, url.toString(), headers, body);
  }

  private static class MimeOverridingTypedOutput implements TypedOutput {
    private final TypedOutput delegate;
    private final String mimeType;

    MimeOverridingTypedOutput(TypedOutput delegate, String mimeType) {
      this.delegate = delegate;
      this.mimeType = mimeType;
    }

    @Override public String fileName() {
      return delegate.fileName();
    }

    @Override public String mimeType() {
      return mimeType;
    }

    @Override public long length() {
      return delegate.length();
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      delegate.writeTo(out);
    }
  }
}
