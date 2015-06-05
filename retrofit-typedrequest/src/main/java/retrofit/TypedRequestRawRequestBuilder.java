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

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URLEncoder;
import java.util.Map;

import okio.BufferedSink;

final class TypedRequestRawRequestBuilder {

  private static final Headers NO_HEADERS = Headers.of();

  private final String requestMethod;
  private final HttpUrl.Builder urlBuilder;
  private final TypedRequest request;
  private final boolean hasBody;
  private final Retrofit retrofit;

  private MultipartBuilder multipartBuilder;
  private FormEncodingBuilder formEncodingBuilder;
  private RequestBody body;

  private String relativeUrl;
  private StringBuilder queryParams;
  private Headers.Builder headers;
  private String contentTypeHeader;

  TypedRequestRawRequestBuilder(Retrofit retrofit, TypedRequest request) {
    this.retrofit = retrofit;
    this.urlBuilder = retrofit.baseUrl().url().newBuilder();
    this.request = request;
    this.requestMethod = request.method().name();
    this.hasBody = "PATCH".equals(requestMethod) || "POST".equals(requestMethod)
        || "PUT".equals(requestMethod);

    if (request.bodyEncoding() == TypedRequest.BodyEncoding.FORM_URL_ENCODED) {
      // Will be set to 'body' in 'build'.
      formEncodingBuilder = new FormEncodingBuilder();
    } else if (request.bodyEncoding() == TypedRequest.BodyEncoding.MULTIPART) {
      // Will be set to 'body' in 'build'.
      multipartBuilder = new MultipartBuilder().type(MultipartBuilder.FORM);
    }

    addPath();
    addBody();
    addHeaders();
    addQueryParams();
    addFields();
    addParts();
  }

  private void addFields() {
    if (!(request.bodyEncoding() == TypedRequest.BodyEncoding.FORM_URL_ENCODED)) {
      return;
    }

    for (Field field : request.fields()) {
      String entryKey = field.name();
      if (entryKey == null) {
        throw new IllegalArgumentException("Parameter field map contained null key.");
      }
      Object entryValue = field.value();
      if (entryValue != null) { // Skip null values.
        if (!field.encoded()) {
          formEncodingBuilder.add(entryKey, entryValue.toString());
        } else {
          formEncodingBuilder.addEncoded(entryKey, entryValue.toString());
        }
      }
    }
  }

  private void addParts() {
    if (!(request.bodyEncoding() == TypedRequest.BodyEncoding.MULTIPART)) {
      return;
    }
    for (Part part : request.parts()) {
      String entryKey = part.name();
      if (entryKey == null) {
        throw new IllegalArgumentException("Part map contained null key.");
      }
      Object entryValue = part.value();
      StringBuilder contentDisposition = new StringBuilder();
      contentDisposition.append("form-data; name=\"");
      contentDisposition.append(entryKey);
      contentDisposition.append("\"");
      if (part.filename() != null) {
        contentDisposition.append("; filename=\"");
        contentDisposition.append(part.filename());
        contentDisposition.append("\"");
      }
      Headers headers = Headers.of(
          "Content-Disposition", contentDisposition.toString(),
          "Content-Transfer-Encoding", part.encoding());
      if (entryValue != null) { // Skip null values.
        if (entryValue instanceof RequestBody) {
          multipartBuilder.addPart(headers, (RequestBody) entryValue);
        } else if (entryValue instanceof String) {
          multipartBuilder.addPart(headers, RequestBody.create(
              MediaType.parse("text/plain"), (String) entryValue));
        } else {
          //noinspection unchecked
          Converter<Object, RequestBody> converter =
              retrofit.requestConverter(entryValue.getClass(), new Annotation[0]);
          RequestBody body;
          try {
            //noinspection unchecked
            body = converter.convert(entryValue);
          } catch (IOException e) {
            throw new RuntimeException("Unable to convert " + entryValue + " to RequestBody");
          }
          multipartBuilder.addPart(headers, body);
        }
      }
    }
  }

  private void addPath() {
    String url = request.path();
    String query = null;
    int question = request.path().indexOf('?');
    if (question != -1 && question < request.path().length() - 1) {
      url = request.path().substring(0, question);
      query = request.path().substring(question + 1);
    }

    relativeUrl = url;

    if (query != null) {
      queryParams = new StringBuilder().append('?').append(query);
    }
  }

  private void addBody() {
    Object body = request.body();
    if (body instanceof RequestBody) {
      this.body = (RequestBody) body;
    } else if (body != null) {
      //noinspection unchecked
      Converter<Object, RequestBody> converter =
          retrofit.requestConverter(body.getClass(), new Annotation[0]);
      RequestBody requestBody;
      try {
        //noinspection unchecked
        requestBody = converter.convert(body);
      } catch (IOException e) {
        throw new RuntimeException("Unable to convert " + body + " to RequestBody");
      }
      this.body = requestBody;
    }
  }

  private void addHeaders() {
    Map<String, String> headers = request.headers();
    if (headers != null) {
      this.headers = parseHeaders(headers).newBuilder();
    }
  }

  com.squareup.okhttp.Headers parseHeaders(Map<String, String> headers) {
    com.squareup.okhttp.Headers.Builder builder = new com.squareup.okhttp.Headers.Builder();
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      String entryKey = entry.getKey();
      if (entryKey == null) {
        throw new IllegalArgumentException("Header map contained null key.");
      }
      String entryValue = entry.getValue();
      if (entryValue != null) {
        if ("Content-Type".equalsIgnoreCase(entryKey)) {
          contentTypeHeader = entryValue;
        } else {
          builder.add(entryKey, entryValue);
        }
      }
    }
    return builder.build();
  }

  private void addQueryParam(String name, String value, boolean encoded) {
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

      if (!encoded) {
        name = URLEncoder.encode(name, "UTF-8");
        value = URLEncoder.encode(value, "UTF-8");
      }
      queryParams.append(name).append('=').append(value);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(
          "Unable to convert query parameter \"" + name + "\" value to UTF-8: " + value, e);
    }
  }

  private void addQueryParams() {
    for (Query query : request.queryParams()) {
      String entryKey = query.name();
      if (entryKey == null) {
        throw new IllegalArgumentException("Parameter query map contained null key.");
      }
      String entryValue = query.value();
      if (entryValue != null) { // Skip null values.
        addQueryParam(entryKey, entryValue, query.encoded());
      }
    }
  }

  com.squareup.okhttp.Request build() {
    String apiUrl = urlBuilder.build().toString();
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
      } else if (hasBody) {
        // Body is absent, make an empty body.
        body = RequestBody.create(null, new byte[0]);
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

    return new com.squareup.okhttp.Request.Builder()
        .url(url.toString())
        .method(requestMethod, body)
        .headers(headers)
        .tag(request.tag())
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
