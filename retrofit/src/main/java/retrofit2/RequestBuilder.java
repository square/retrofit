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
package retrofit2;

import java.io.IOException;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;

final class RequestBuilder {
  private static final char[] HEX_DIGITS =
      { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
  private static final String PATH_SEGMENT_ALWAYS_ENCODE_SET = " \"<>^`{}|\\?#";

  private final String method;

  private final HttpUrl baseUrl;
  private String relativeUrl;
  private HttpUrl.Builder urlBuilder;

  private final Request.Builder requestBuilder;
  private MediaType contentType;

  private final boolean hasBody;
  private MultipartBody.Builder multipartBuilder;
  private FormBody.Builder formBuilder;
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
      formBuilder = new FormBody.Builder();
    } else if (isMultipart) {
      // Will be set to 'body' in 'build'.
      multipartBuilder = new MultipartBody.Builder();
      multipartBuilder.setType(MultipartBody.FORM);
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
    relativeUrl = relativeUrl.replace("{" + name + "}", canonicalizeForPath(value, encoded));
  }

  private static String canonicalizeForPath(String input, boolean alreadyEncoded) {
    int codePoint;
    for (int i = 0, limit = input.length(); i < limit; i += Character.charCount(codePoint)) {
      codePoint = input.codePointAt(i);
      if (codePoint < 0x20 || codePoint >= 0x7f
          || PATH_SEGMENT_ALWAYS_ENCODE_SET.indexOf(codePoint) != -1
          || (!alreadyEncoded && (codePoint == '/' || codePoint == '%'))) {
        // Slow path: the character at i requires encoding!
        Buffer out = new Buffer();
        out.writeUtf8(input, 0, i);
        canonicalizeForPath(out, input, i, limit, alreadyEncoded);
        return out.readUtf8();
      }
    }

    // Fast path: no characters required encoding.
    return input;
  }

  private static void canonicalizeForPath(Buffer out, String input, int pos, int limit,
      boolean alreadyEncoded) {
    Buffer utf8Buffer = null; // Lazily allocated.
    int codePoint;
    for (int i = pos; i < limit; i += Character.charCount(codePoint)) {
      codePoint = input.codePointAt(i);
      if (alreadyEncoded
          && (codePoint == '\t' || codePoint == '\n' || codePoint == '\f' || codePoint == '\r')) {
        // Skip this character.
      } else if (codePoint < 0x20 || codePoint >= 0x7f
          || PATH_SEGMENT_ALWAYS_ENCODE_SET.indexOf(codePoint) != -1
          || (!alreadyEncoded && (codePoint == '/' || codePoint == '%'))) {
        // Percent encode this character.
        if (utf8Buffer == null) {
          utf8Buffer = new Buffer();
        }
        utf8Buffer.writeUtf8CodePoint(codePoint);
        while (!utf8Buffer.exhausted()) {
          int b = utf8Buffer.readByte() & 0xff;
          out.writeByte('%');
          out.writeByte(HEX_DIGITS[(b >> 4) & 0xf]);
          out.writeByte(HEX_DIGITS[b & 0xf]);
        }
      } else {
        // This character doesn't need encoding. Just copy it over.
        out.writeUtf8CodePoint(codePoint);
      }
    }
  }

  void addQueryParam(String name, String value, boolean encoded) {
    if (relativeUrl != null) {
      // Do a one-time combination of the built relative URL and the base URL.
      urlBuilder = baseUrl.newBuilder(relativeUrl);
      if (urlBuilder == null) {
        throw new IllegalArgumentException(
            "Malformed URL. Base: " + baseUrl + ", Relative: " + relativeUrl);
      }
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
      formBuilder.addEncoded(name, value);
    } else {
      formBuilder.add(name, value);
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
      if (url == null) {
        throw new IllegalArgumentException(
            "Malformed URL. Base: " + baseUrl + ", Relative: " + relativeUrl);
      }
    }

    RequestBody body = this.body;
    if (body == null) {
      // Try to pull from one of the builders.
      if (formBuilder != null) {
        body = formBuilder.build();
      } else if (multipartBuilder != null) {
        body = multipartBuilder.build();
      } else if (hasBody) {
        body = Utils.EMPTY_BODY;
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
