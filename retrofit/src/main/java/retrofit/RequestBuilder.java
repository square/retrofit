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

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.Map;
import okio.BufferedSink;
import retrofit.converter.Converter;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FieldMap;
import retrofit.http.Header;
import retrofit.http.Part;
import retrofit.http.PartMap;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.QueryMap;

final class RequestBuilder {
  private static final Headers NO_HEADERS = Headers.of();

  private final Converter converter;
  private final Annotation[] paramAnnotations;
  private final String requestMethod;
  private final boolean async;
  private final String apiUrl;

  private MultipartBuilder multipartBuilder;
  private FormEncodingBuilder formEncodingBuilder;
  private RequestBody body;

  private String relativeUrl;
  private StringBuilder queryParams;
  private Headers.Builder headers;
  private String contentTypeHeader;

  RequestBuilder(String apiUrl, MethodInfo methodInfo, Converter converter) {
    this.apiUrl = apiUrl;
    this.converter = converter;

    paramAnnotations = methodInfo.requestParamAnnotations;
    requestMethod = methodInfo.requestMethod;
    async = methodInfo.executionType == MethodInfo.ExecutionType.ASYNC;

    if (methodInfo.headers != null) {
      headers = methodInfo.headers.newBuilder();
    }
    contentTypeHeader = methodInfo.contentTypeHeader;

    relativeUrl = methodInfo.requestUrl;

    String requestQuery = methodInfo.requestQuery;
    if (requestQuery != null) {
      queryParams = new StringBuilder().append('?').append(requestQuery);
    }

    switch (methodInfo.requestType) {
      case FORM_URL_ENCODED:
        // Will be set to 'body' in 'build'.
        formEncodingBuilder = new FormEncodingBuilder();
        break;
      case MULTIPART:
        // Will be set to 'body' in 'build'.
        multipartBuilder = new MultipartBuilder();
        break;
      case SIMPLE:
        // If present, 'body' will be set in 'setArguments' call.
        break;
      default:
        throw new IllegalArgumentException("Unknown request type: " + methodInfo.requestType);
    }
  }

  public void addHeader(String name, String value) {
    if (name == null) {
      throw new IllegalArgumentException("Header name must not be null.");
    }
    if ("Content-Type".equalsIgnoreCase(name)) {
      contentTypeHeader = value;
      return;
    }

    Headers.Builder headers = this.headers;
    if (headers == null) {
      this.headers = headers = new Headers.Builder();
    }
    headers.add(name, value);
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
        addQueryParam(entryKey.toString(), entryValue.toString(), encodeNames, encodeValues);
      }
    }
  }

  private void addFormField(String name, String value, boolean encode) {
    if (encode) {
      formEncodingBuilder.add(name, value);
    } else {
      formEncodingBuilder.addEncoded(name, value);
    }
  }

  void setArguments(Object[] args) {
    if (args == null) {
      return;
    }
    int count = args.length;
    if (async) {
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
      } else if (annotationType == Query.class) {
        if (value != null) { // Skip null values.
          Query query = (Query) annotation;
          addQueryParam(query.value(), value, query.encodeName(), query.encodeValue());
        }
      } else if (annotationType == QueryMap.class) {
        if (value != null) { // Skip null values.
          QueryMap queryMap = (QueryMap) annotation;
          addQueryParamMap(i, (Map<?, ?>) value, queryMap.encodeNames(), queryMap.encodeValues());
        }
      } else if (annotationType == Header.class) {
        if (value != null) { // Skip null values.
          String name = ((Header) annotation).value();
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
          boolean encode = field.encode();
          if (value instanceof Iterable) {
            for (Object iterableValue : (Iterable<?>) value) {
              if (iterableValue != null) { // Skip null values.
                addFormField(name, iterableValue.toString(), encode);
              }
            }
          } else if (value.getClass().isArray()) {
            for (int x = 0, arrayLength = Array.getLength(value); x < arrayLength; x++) {
              Object arrayValue = Array.get(value, x);
              if (arrayValue != null) { // Skip null values.
                addFormField(name, arrayValue.toString(), encode);
              }
            }
          } else {
            addFormField(name, value.toString(), encode);
          }
        }
      } else if (annotationType == FieldMap.class) {
        if (value != null) { // Skip null values.
          FieldMap fieldMap = (FieldMap) annotation;
          boolean encode = fieldMap.encode();
          for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            Object entryKey = entry.getKey();
            if (entryKey == null) {
              throw new IllegalArgumentException(
                  "Parameter #" + (i + 1) + " field map contained null key.");
            }
            Object entryValue = entry.getValue();
            if (entryValue != null) { // Skip null values.
              addFormField(entryKey.toString(), entryValue.toString(), encode);
            }
          }
        }
      } else if (annotationType == Part.class) {
        if (value != null) { // Skip null values.
          String name = ((Part) annotation).value();
          String transferEncoding = ((Part) annotation).encoding();
          Headers headers = Headers.of(
              "Content-Disposition", "name=\"" + name + "\"",
              "Content-Transfer-Encoding", transferEncoding);
          if (value instanceof RequestBody) {
            multipartBuilder.addPart(headers, (RequestBody) value);
          } else if (value instanceof String) {
            multipartBuilder.addPart(headers,
                RequestBody.create(MediaType.parse("text/plain"), (String) value));
          } else {
            multipartBuilder.addPart(headers, converter.toBody(value, value.getClass()));
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
            Headers headers = Headers.of(
                "Content-Disposition", "name=\"" + entryName + "\"",
                "Content-Transfer-Encoding", transferEncoding);
            if (entryValue != null) { // Skip null values.
              if (entryValue instanceof RequestBody) {
                multipartBuilder.addPart(headers, (RequestBody) entryValue);
              } else if (entryValue instanceof String) {
                multipartBuilder.addPart(headers,
                    RequestBody.create(MediaType.parse("text/plain"), (String) entryValue));
              } else {
                multipartBuilder.addPart(headers,
                    converter.toBody(entryValue, entryValue.getClass()));
              }
            }
          }
        }
      } else if (annotationType == Body.class) {
        if (value == null) {
          throw new IllegalArgumentException("Body parameter value must not be null.");
        }
        if (value instanceof RequestBody) {
          body = (RequestBody) value;
        } else {
          body = converter.toBody(value, value.getClass());
        }
      } else {
        throw new IllegalArgumentException(
            "Unknown annotation: " + annotationType.getCanonicalName());
      }
    }
  }

  Request build() {
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

    RequestBody body = this.body;
    if (body == null) {
      // Try to pull from one of the builders.
      if (formEncodingBuilder != null) {
        body = formEncodingBuilder.build();
      } else if (multipartBuilder != null) {
        body = multipartBuilder.build();
      }
    }

    Headers.Builder headerBuilder = this.headers;
    if (contentTypeHeader != null) {
      if (body != null) {
        body = new MediaTypeOverridingRequestBody(body, contentTypeHeader);
      } else {
        if (headerBuilder == null) {
          headerBuilder = new Headers.Builder();
        }
        headerBuilder.add("Content-Type", contentTypeHeader);
      }
    }
    Headers headers = headerBuilder != null ? headerBuilder.build() : NO_HEADERS;

    return new Request.Builder()
        .url(url.toString())
        .method(requestMethod, body)
        .headers(headers)
        .build();
  }

  private static class MediaTypeOverridingRequestBody extends RequestBody {
    private final RequestBody delegate;
    private final MediaType mediaType;

    MediaTypeOverridingRequestBody(RequestBody delegate, String mediaType) {
      this.delegate = delegate;
      this.mediaType = MediaType.parse(mediaType);
    }

    @Override public MediaType contentType() {
      return mediaType;
    }

    @Override public long contentLength() throws IOException {
      return delegate.contentLength();
    }

    @Override public void writeTo(BufferedSink sink) throws IOException {
      delegate.writeTo(sink);
    }
  }
}
