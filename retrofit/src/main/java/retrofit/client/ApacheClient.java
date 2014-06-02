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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedOutput;

/** A {@link Client} which uses an implementation of Apache's {@link HttpClient}. */
public class ApacheClient implements Client {
  private static HttpClient createDefaultClient() {
    HttpParams params = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(params, Defaults.CONNECT_TIMEOUT_MILLIS);
    HttpConnectionParams.setSoTimeout(params, Defaults.READ_TIMEOUT_MILLIS);
    return new DefaultHttpClient(params);
  }

  private final HttpClient client;

  /** Creates an instance backed by {@link DefaultHttpClient}. */
  public ApacheClient() {
    this(createDefaultClient());
  }

  public ApacheClient(HttpClient client) {
    this.client = client;
  }

  @Override public Response execute(Request request) throws IOException {
    HttpUriRequest apacheRequest = createRequest(request);
    HttpResponse apacheResponse = execute(client, apacheRequest);
    return parseResponse(request.getUrl(), apacheResponse);
  }

  /** Execute the specified {@code request} using the provided {@code client}. */
  protected HttpResponse execute(HttpClient client, HttpUriRequest request) throws IOException {
    return client.execute(request);
  }

  static HttpUriRequest createRequest(Request request) {
    if (request.getBody() != null) {
      return new GenericEntityHttpRequest(request);
    }
    return new GenericHttpRequest(request);
  }

  static Response parseResponse(String url, HttpResponse response) throws IOException {
    StatusLine statusLine = response.getStatusLine();
    int status = statusLine.getStatusCode();
    String reason = statusLine.getReasonPhrase();

    List<Header> headers = new ArrayList<Header>();
    String contentType = "application/octet-stream";
    for (org.apache.http.Header header : response.getAllHeaders()) {
      String name = header.getName();
      String value = header.getValue();
      if ("Content-Type".equalsIgnoreCase(name)) {
        contentType = value;
      }
      headers.add(new Header(name, value));
    }

    TypedByteArray body = null;
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      byte[] bytes = EntityUtils.toByteArray(entity);
      body = new TypedByteArray(contentType, bytes);
    }

    return new Response(url, status, reason, headers, body);
  }

  private static class GenericHttpRequest extends HttpRequestBase {
    private final String method;

    public GenericHttpRequest(Request request) {
      method = request.getMethod();
      setURI(URI.create(request.getUrl()));

      // Add all headers.
      for (Header header : request.getHeaders()) {
        addHeader(new BasicHeader(header.getName(), header.getValue()));
      }
    }

    @Override public String getMethod() {
      return method;
    }
  }

  private static class GenericEntityHttpRequest extends HttpEntityEnclosingRequestBase {
    private final String method;

    GenericEntityHttpRequest(Request request) {
      super();
      method = request.getMethod();
      setURI(URI.create(request.getUrl()));

      // Add all headers.
      for (Header header : request.getHeaders()) {
        addHeader(new BasicHeader(header.getName(), header.getValue()));
      }

      // Add the content body.
      setEntity(new TypedOutputEntity(request.getBody()));
    }

    @Override public String getMethod() {
      return method;
    }
  }

  /** Container class for passing an entire {@link TypedOutput} as an {@link HttpEntity}. */
  static class TypedOutputEntity extends AbstractHttpEntity {
    final TypedOutput typedOutput;

    TypedOutputEntity(TypedOutput typedOutput) {
      this.typedOutput = typedOutput;
      setContentType(typedOutput.mimeType());
    }

    @Override public boolean isRepeatable() {
      return true;
    }

    @Override public long getContentLength() {
      return typedOutput.length();
    }

    @Override public InputStream getContent() throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      typedOutput.writeTo(out);
      return new ByteArrayInputStream(out.toByteArray());
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      typedOutput.writeTo(out);
    }

    @Override public boolean isStreaming() {
      return false;
    }
  }
}
