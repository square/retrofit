// Copyright 2013 Square, Inc.
package retrofit.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit.http.Header;
import retrofit.http.mime.TypedInput;
import retrofit.http.mime.TypedOutput;

public class UrlConnectionClient implements Client {
  @Override public Response execute(Request request) throws IOException {
    HttpURLConnection connection = openConnection(request);
    prepareRequest(connection, request);
    return readResponse(connection);
  }

  protected HttpURLConnection openConnection(Request request) throws IOException {
    return (HttpURLConnection) new URL(request.getUrl()).openConnection();
  }

  void prepareRequest(HttpURLConnection connection, Request request) throws IOException {
    connection.setRequestMethod(request.getMethod());
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
