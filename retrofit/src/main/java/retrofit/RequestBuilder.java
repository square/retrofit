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
  private final String requestMethod;
  private final boolean requestHasBody;
  private final HttpUrl.Builder urlBuilder;
  private final Request.Builder requestBuilder;

  private MultipartBuilder multipartBuilder;
  private FormEncodingBuilder formEncodingBuilder;
  private RequestBody body;

  private String relativeUrl;
  private MediaType mediaType;

  RequestBuilder(HttpUrl url, MethodInfo methodInfo) {
    requestMethod = methodInfo.requestMethod;
    requestHasBody = methodInfo.requestHasBody;
    mediaType = methodInfo.mediaType;
    relativeUrl = methodInfo.requestUrl;

    urlBuilder = url.newBuilder();
    requestBuilder = new Request.Builder();

    Headers headers = methodInfo.headers;
    if (headers != null) {
      requestBuilder.headers(headers);
    }

    String requestQuery = methodInfo.requestQuery;
    if (requestQuery != null) {
      urlBuilder.query(requestQuery);
    }

    switch (methodInfo.bodyEncoding) {
      case FORM_URL_ENCODED:
        // Will be set to 'body' in 'build'.
        formEncodingBuilder = new FormEncodingBuilder();
        break;
      case MULTIPART:
        // Will be set to 'body' in 'build'.
        multipartBuilder = new MultipartBuilder();
        break;
      case NONE:
        // If present, 'body' will be set in 'setArguments' call.
        break;
      default:
        throw new IllegalArgumentException("Unknown request type: " + methodInfo.bodyEncoding);
    }
  }

  void addHeader(String name, String value) {
    if ("Content-Type".equalsIgnoreCase(name)) {
      mediaType = MediaType.parse(value);
    } else {
      requestBuilder.addHeader(name, value);
    }
  }

  void addPathParam(String name, String value, boolean encoded) {
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
    urlBuilder.encodedPath(relativeUrl);

    RequestBody body = this.body;
    if (body == null) {
      // Try to pull from one of the builders.
      if (formEncodingBuilder != null) {
        body = formEncodingBuilder.build();
      } else if (multipartBuilder != null) {
        body = multipartBuilder.build();
      } else if (requestHasBody) {
        // Body is absent, make an empty body.
        body = RequestBody.create(null, new byte[0]);
      }
    }

    MediaType mediaType = this.mediaType;
    if (mediaType != null) {
      if (body != null) {
        body = new MediaTypeOverridingRequestBody(body, mediaType);
      } else {
        requestBuilder.addHeader("Content-Type", mediaType.toString());
      }
    }

    return requestBuilder
        .url(urlBuilder.build())
        .method(requestMethod, body)
        .build();
  }

  private static class MediaTypeOverridingRequestBody extends RequestBody {
    private final RequestBody delegate;
    private final MediaType mediaType;

    MediaTypeOverridingRequestBody(RequestBody delegate, MediaType mediaType) {
      this.delegate = delegate;
      this.mediaType = mediaType;
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
