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
import java.util.regex.Pattern;
import javax.annotation.Nullable;
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

  /**
   * Matches strings that contain {@code .} or {@code ..} as a complete path segment. This also
   * matches dots in their percent-encoded form, {@code %2E}.
   *
   * <p>It is okay to have these strings within a larger path segment (like {@code a..z} or {@code
   * index.html}) but when alone they have a special meaning. A single dot resolves to no path
   * segment so {@code /one/./three/} becomes {@code /one/three/}. A double-dot pops the preceding
   * directory, so {@code /one/../three/} becomes {@code /three/}.
   *
   * <p>We forbid these in Retrofit paths because they're likely to have the unintended effect.
   * For example, passing {@code ..} to {@code DELETE /account/book/{isbn}/} yields {@code DELETE
   * /account/}.
   */
  private static final Pattern PATH_TRAVERSAL = Pattern.compile("(.*/)?(\\.|%2e|%2E){1,2}(/.*)?");

  private final String method;

  private final HttpUrl baseUrl;
  private @Nullable String relativeUrl;
  private @Nullable HttpUrl.Builder urlBuilder;

  private final Request.Builder requestBuilder;
  private @Nullable MediaType contentType;

  private final boolean hasBody;
  private @Nullable MultipartBody.Builder multipartBuilder;
  private @Nullable FormBody.Builder formBuilder;
  private @Nullable RequestBody body;

  RequestBuilder(String method, HttpUrl baseUrl,
      @Nullable String relativeUrl, @Nullable Headers headers, @Nullable MediaType contentType,
      boolean hasBody, boolean isFormEncoded, boolean isMultipart) {
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

  void setRelativeUrl(Object relativeUrl) {
    this.relativeUrl = relativeUrl.toString();
  }

  void addHeader(String name, String value) {
    if ("Content-Type".equalsIgnoreCase(name)) {
      try {
        contentType = MediaType.get(value);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Malformed content type: " + value, e);
      }
    } else {
      requestBuilder.addHeader(name, value);
    }
  }

  void addPathParam(String name, String value, boolean encoded) {
    if (relativeUrl == null) {
      // The relative URL is cleared when the first query parameter is set.
      throw new AssertionError();
    }
    String replacement = canonicalizeForPath(value, encoded);
    String newRelativeUrl = relativeUrl.replace("{" + name + "}", replacement);
    if (PATH_TRAVERSAL.matcher(newRelativeUrl).matches()) {
      throw new IllegalArgumentException(
          "@Path parameters shouldn't perform path traversal ('.' or '..'): " + value);
    }
    relativeUrl = newRelativeUrl;
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

  void addQueryParam(String name, @Nullable String value, boolean encoded) {
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
      //noinspection ConstantConditions Checked to be non-null by above 'if' block.
      urlBuilder.addEncodedQueryParameter(name, value);
    } else {
      //noinspection ConstantConditions Checked to be non-null by above 'if' block.
      urlBuilder.addQueryParameter(name, value);
    }
  }

  @SuppressWarnings("ConstantConditions") // Only called when isFormEncoded was true.
  void addFormField(String name, String value, boolean encoded) {
    if (encoded) {
      formBuilder.addEncoded(name, value);
    } else {
      formBuilder.add(name, value);
    }
  }

  @SuppressWarnings("ConstantConditions") // Only called when isMultipart was true.
  void addPart(Headers headers, RequestBody body) {
    multipartBuilder.addPart(headers, body);
  }

  @SuppressWarnings("ConstantConditions") // Only called when isMultipart was true.
  void addPart(MultipartBody.Part part) {
    multipartBuilder.addPart(part);
  }

  void setBody(RequestBody body) {
    this.body = body;
  }

  Request.Builder get() {
    HttpUrl url;
    HttpUrl.Builder urlBuilder = this.urlBuilder;
    if (urlBuilder != null) {
      url = urlBuilder.build();
    } else {
      // No query parameters triggered builder creation, just combine the relative URL and base URL.
      //noinspection ConstantConditions Non-null if urlBuilder is null.
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
        .method(method, body);
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
