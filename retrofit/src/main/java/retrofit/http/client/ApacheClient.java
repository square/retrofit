// Copyright 2013 Square, Inc.
package retrofit.http.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import retrofit.http.Header;
import retrofit.io.TypedByteArray;
import retrofit.io.TypedOutput;

import static org.apache.http.entity.mime.HttpMultipartMode.BROWSER_COMPATIBLE;

/** A {@link Client} which uses an implementation of Apache's {@link HttpClient}. */
public class ApacheClient implements Client {
  private static final String HEADER_CONTENT_TYPE = "Content-Type";

  private final HttpClient client;

  /** Creates an instance backed by {@link DefaultHttpClient}. */
  public ApacheClient() {
    this(new DefaultHttpClient());
  }

  public ApacheClient(HttpClient client) {
    this.client = client;
  }

  @Override public Response execute(Request request) throws IOException {
    // Create and prepare the Apache request object.
    HttpUriRequest apacheRequest = createRequest(request);
    prepareRequest(apacheRequest);

    // Obtain and prepare the Apache response object.
    HttpResponse apacheResponse = client.execute(apacheRequest);
    prepareResponse(apacheResponse);

    return parseResponse(apacheResponse);
  }

  /** Callback for additional preparation of the request before execution. */
  protected void prepareRequest(HttpUriRequest request) {
  }

  /** Callback for additional preparation of the response before parsing. */
  protected void prepareResponse(HttpResponse response) {
  }

  static HttpUriRequest createRequest(Request request) {
    return new GenericHttpRequest(request);
  }

  static Response parseResponse(HttpResponse response) throws IOException {
    StatusLine statusLine = response.getStatusLine();
    int status = statusLine.getStatusCode();
    String reason = statusLine.getReasonPhrase();

    List<Header> headers = new ArrayList<Header>();
    String contentType = "application/octet-stream";
    for (org.apache.http.Header header : response.getAllHeaders()) {
      String name = header.getName();
      String value = header.getValue();
      if (name.equalsIgnoreCase(HEADER_CONTENT_TYPE)) {
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

    return new Response(status, reason, headers, body);
  }

  private static class GenericHttpRequest extends HttpEntityEnclosingRequestBase {
    private final String method;

    GenericHttpRequest(Request request) {
      super();
      method = request.getMethod();
      setURI(URI.create(request.getUrl()));

      // Add all headers.
      for (Header header : request.getHeaders()) {
        addHeader(new BasicHeader(header.getName(), header.getValue()));
      }

      // Add the content body, if any.
      if (!request.isMultipart()) {
        TypedOutput body = request.getBody();
        if (body != null) {
          setEntity(new TypedOutputEntity(body));
        }
      } else {
        Map<String, TypedOutput> bodyParameters = request.getBodyParameters();
        if (bodyParameters != null && !bodyParameters.isEmpty()) {
          MultipartEntity entity = new MultipartEntity(BROWSER_COMPATIBLE);
          for (Map.Entry<String, TypedOutput> entry : bodyParameters.entrySet()) {
            entity.addPart(entry.getKey(), new TypedOutputBody(entry.getValue()));
          }
          setEntity(entity);
        }
      }
    }

    @Override public String getMethod() {
      return method;
    }
  }

  /** Adapts {@link org.apache.http.entity.mime.content.ContentBody} to {@link TypedOutput}. */
  private static class TypedOutputBody extends AbstractContentBody {
    private final TypedOutput typedBytes;

    TypedOutputBody(TypedOutput typedBytes) {
      super(typedBytes.mimeType());
      this.typedBytes = typedBytes;
    }

    @Override public long getContentLength() {
      return typedBytes.length();
    }

    @Override public String getFilename() {
      return null;
    }

    @Override public String getCharset() {
      return null;
    }

    @Override public String getTransferEncoding() {
      return MIME.ENC_BINARY;
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      // Note: We probably want to differentiate I/O errors that occur while reading a file from
      // network errors. Network operations can be retried. File operations will probably continue
      // to fail.
      //
      // In the case of photo uploads, we at least check that the file exists before we even try to
      // upload it.
      typedBytes.writeTo(out);
    }
  }

  /** Container class for passing an entire {@link TypedOutput} as an {@link HttpEntity}. */
  static class TypedOutputEntity extends AbstractHttpEntity {
    private final TypedOutput typedOutput;

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
