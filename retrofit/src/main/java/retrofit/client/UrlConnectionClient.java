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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit.RetrofitError;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/** Retrofit client that uses {@link HttpURLConnection} for communication. */
public class UrlConnectionClient implements Client {
  private final Field methodField;

  public UrlConnectionClient() {
    try {
      this.methodField = HttpURLConnection.class.getDeclaredField("method");
      this.methodField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      throw RetrofitError.unexpectedError(null, e);
    }
  }

  @Override public Response execute(Request request) throws IOException {
    HttpURLConnection connection = openConnection(request);
    prepareRequest(connection, request);
    return readResponse(connection);
  }

  protected HttpURLConnection openConnection(Request request) throws IOException {
    HttpURLConnection connection =
        (HttpURLConnection) new URL(request.getUrl()).openConnection();
    connection.setConnectTimeout(Defaults.CONNECT_TIMEOUT);
    connection.setReadTimeout(Defaults.READ_TIMEOUT);
    return connection;
  }

  void prepareRequest(HttpURLConnection connection, Request request) throws IOException {
    // HttpURLConnection artificially restricts request method
    try {
      connection.setRequestMethod(request.getMethod());
    } catch (ProtocolException e) {
      try {
        methodField.set(connection, request.getMethod());
      } catch (IllegalAccessException e1) {
        throw RetrofitError.unexpectedError(request.getUrl(), e1);
      }
    }

    connection.setDoInput(true);

    for (Header header : request.getHeaders()) {
      connection.addRequestProperty(header.getName(), header.getValue());
    }

    TypedOutput body = request.getBody();
    if (body != null) {
      connection.setDoOutput(true);
      connection.addRequestProperty("Content-Type", body.mimeType());
      long length = body.length();
      if (length != -1) {
        connection.addRequestProperty("Content-Length", String.valueOf(length));
      }
      body.writeTo(connection.getOutputStream());
    }
  }

  Response readResponse(HttpURLConnection connection) throws IOException {
    int status = connection.getResponseCode();
    String reason = connection.getResponseMessage();

    List<Header> headers = new ArrayList<Header>();
    for (Map.Entry<String, List<String>> field : connection.getHeaderFields().entrySet()) {
      String name = field.getKey();
      for (String value : field.getValue()) {
        headers.add(new Header(name, value));
      }
    }

    String mimeType = connection.getContentType();
    int length = connection.getContentLength();
    InputStream stream;
    if (status >= 400) {
      stream = connection.getErrorStream();
    } else {
      stream = connection.getInputStream();
    }
    TypedInput responseBody = new TypedInputStream(mimeType, length, stream);
    return new Response(status, reason, headers, responseBody);
  }

  private static class TypedInputStream implements TypedInput {
    private final String mimeType;
    private final long length;
    private final InputStream stream;

    private TypedInputStream(String mimeType, long length, InputStream stream) {
      this.mimeType = mimeType;
      this.length = length;
      this.stream = stream;
    }

    @Override public String mimeType() {
      return mimeType;
    }

    @Override public long length() {
      return length;
    }

    @Override public InputStream in() throws IOException {
      return stream;
    }
  }
}
