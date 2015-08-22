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
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import okio.BufferedSink;

final class RequestBuilder {
  private final String method;

  private final HttpUrl baseUrl;
  private String relativeUrl;
  private HttpUrl.Builder urlBuilder;

  private final Request.Builder requestBuilder;
  private MediaType contentType;

  private final boolean hasBody;
  private MultipartBuilder multipartBuilder;
  private FormEncodingBuilder formEncodingBuilder;
  private RequestBody body;

  RequestBuilder(String method, HttpUrl baseUrl, String relativeUrl, Headers headers,
      MediaType contentType, boolean hasBody, boolean isFormEncoded, boolean isMultipart) {
    this.method = method;
    this.baseUrl = baseUrl;
    this.relativeUrl = relativeUrl;
    this.requestBuilder = new Request.Builder();
    this.contentType = contentType;
    this.hasBody = hasBody;

    if (headers != null) {
      requestBuilder.headers(headers);
    }

    if (isFormEncoded) {
      // Will be set to 'body' in 'build'.
      formEncodingBuilder = new FormEncodingBuilder();
    } else if (isMultipart) {
      // Will be set to 'body' in 'build'.
      multipartBuilder = new MultipartBuilder();
    }
  }

  void setRelativeUrl(String relativeUrl) {
    this.relativeUrl = relativeUrl;
  }

  void addHeader(String name, String value) {
    if ("Content-Type".equalsIgnoreCase(name)) {
      contentType = MediaType.parse(value);
    } else {
      requestBuilder.addHeader(name, value);
    }
  }

  void addPathParam(String name, String value, boolean encoded) {
    if (relativeUrl == null) {
      // The relative URL is cleared when the first query parameter is set.
      throw new AssertionError();
    }
    try {
      if (!encoded) {
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

  void addQueryParam(String name, String value, boolean encoded) {
    if (relativeUrl != null) {
      // Do a one-time combination of the built relative URL and the base URL.
      urlBuilder = baseUrl.resolve(relativeUrl).newBuilder();
      relativeUrl = null;
    }

    if (encoded) {
      urlBuilder.addEncodedQueryParameter(name, value);
    } else {
      urlBuilder.addQueryParameter(name, value);
    }
  }

  void addFormField(String name, String value, boolean encoded) {
    if (encoded) {
      formEncodingBuilder.addEncoded(name, value);
    } else {
      formEncodingBuilder.add(name, value);
    }
  }

  void addPart(Headers headers, RequestBody body) {
    multipartBuilder.addPart(headers, body);
  }

  void setBody(RequestBody body) {
    this.body = body;
  }

  Request build() {
    HttpUrl url;
    HttpUrl.Builder urlBuilder = this.urlBuilder;
    if (urlBuilder != null) {
      url = urlBuilder.build();
    } else {
      // No query parameters triggered builder creation, just combine the relative URL and base URL.
      url = baseUrl.resolve(relativeUrl);
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

    MediaType contentType = this.contentType;
    if (contentType != null) {
      if (body != null) {
        body = new ContentTypeOverridingRequestBody(body, contentType);
      } else {
        requestBuilder.addHeader("Content-Type", contentType.toString());
      }
    }

    return requestBuilder
        .url(url)
        .method(method, body)
        .build();
  }

  private static class ContentTypeOverridingRequestBody extends RequestBody {
    private final RequestBody delegate;
    private final MediaType contentType;

    ContentTypeOverridingRequestBody(RequestBody delegate, MediaType contentType) {
      this.delegate = delegate;
      this.contentType = contentType;
    }

    @Override public MediaType contentType() {
      return contentType;
    }

    @Override public long contentLength() throws IOException {
      return delegate.contentLength();
    }

    @Override public void writeTo(BufferedSink sink) throws IOException {
      delegate.writeTo(sink);
    }
  }
}
