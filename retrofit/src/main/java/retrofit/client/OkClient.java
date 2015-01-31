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

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import okio.BufferedSink;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/** Retrofit client that uses OkHttp for communication. */
public class OkClient implements Client {
  private static OkHttpClient generateDefaultOkHttp() {
    OkHttpClient client = new OkHttpClient();
    client.setConnectTimeout(15, TimeUnit.SECONDS);
    client.setReadTimeout(15, TimeUnit.SECONDS);
    client.setWriteTimeout(15, TimeUnit.SECONDS);
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

  @Override public void execute(Request request, final AsyncCallback callback) {
    client.newCall(createRequest(request)).enqueue(new Callback() {
      @Override public void onFailure(com.squareup.okhttp.Request request, IOException e) {
        callback.onFailure(e);
      }

      @Override public void onResponse(com.squareup.okhttp.Response response) throws IOException {
        callback.onResponse(parseResponse(response));
      }
    });
  }

  static com.squareup.okhttp.Request createRequest(Request request) {
    return new com.squareup.okhttp.Request.Builder()
        .url(request.getUrl())
        .headers(request.getHeaders())
        .method(request.getMethod(), createRequestBody(request.getBody()))
        .build();
  }

  static Response parseResponse(com.squareup.okhttp.Response response) {
    return new Response(response.request().urlString(), response.code(), response.message(),
        response.headers(), createResponseBody(response.body()));
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
}
