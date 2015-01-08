/*
 * Copyright (C) 2013 Square, Inc.
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
package retrofit.client;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okio.BufferedSink;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/** Retrofit client that uses OkHttp for communication. */
public class OkClient implements Client {
  private static OkHttpClient generateDefaultOkHttp() {
    OkHttpClient client = new OkHttpClient();
    client.setConnectTimeout(Defaults.CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    client.setReadTimeout(Defaults.READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    return client;
  }

  private final OkHttpClient client;

  public OkClient() {
    this(generateDefaultOkHttp());
  }

  public OkClient(OkHttpClient client) {
    if (client == null) throw new NullPointerException("client == null");
    this.client = client;
  }

  @Override public Response execute(Request request) throws IOException {
    return parseResponse(client.newCall(createRequest(request)).execute());
  }

  static com.squareup.okhttp.Request createRequest(Request request) {
    com.squareup.okhttp.Request.Builder builder = new com.squareup.okhttp.Request.Builder()
        .url(request.getUrl())
        .method(request.getMethod(), createRequestBody(request.getBody()));

    List<Header> headers = request.getHeaders();
    for (int i = 0, size = headers.size(); i < size; i++) {
      Header header = headers.get(i);
      String value = header.getValue();
      if (value == null) value = "";
      builder.addHeader(header.getName(), value);
    }

    return builder.build();
  }

  static Response parseResponse(com.squareup.okhttp.Response response) {
    return new Response(response.request().urlString(), response.code(), response.message(),
        createHeaders(response.headers()), createResponseBody(response.body()));
  }

  private static RequestBody createRequestBody(final TypedOutput body) {
    if (body == null) {
      return null;
    }
    final MediaType mediaType = MediaType.parse(body.mimeType());
    return new RequestBody() {
      @Override public MediaType contentType() {
        return mediaType;
      }

      @Override public void writeTo(BufferedSink sink) throws IOException {
        body.writeTo(sink.outputStream());
      }

      @Override public long contentLength() {
        return body.length();
      }
    };
  }

  private static TypedInput createResponseBody(final ResponseBody body) {
    if (body.contentLength() == 0) {
      return null;
    }
    return new TypedInput() {
      @Override public String mimeType() {
        MediaType mediaType = body.contentType();
        return mediaType == null ? null : mediaType.toString();
      }

      @Override public long length() {
        return body.contentLength();
      }

      @Override public InputStream in() throws IOException {
        return body.byteStream();
      }
    };
  }

  private static List<Header> createHeaders(Headers headers) {
    int size = headers.size();
    List<Header> headerList = new ArrayList<Header>(size);
    for (int i = 0; i < size; i++) {
      headerList.add(new Header(headers.name(i), headers.value(i)));
    }
    return headerList;
  }
}
