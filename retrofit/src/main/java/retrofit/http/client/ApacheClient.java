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
import retrofit.io.TypedBytes;

import static org.apache.http.entity.mime.HttpMultipartMode.BROWSER_COMPATIBLE;

/** A {@link Client} which uses an implementation of Apache's {@link HttpClient}. */
public class ApacheClient implements Client {
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
    for (org.apache.http.Header header : response.getAllHeaders()) {
      headers.add(new Header(header.getName(), header.getValue()));
    }

    byte[] body = null;
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      body = EntityUtils.toByteArray(entity);
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
        TypedBytes body = request.getBody();
        if (body != null) {
          setEntity(new TypedBytesEntity(body));
        }
      } else {
        Map<String, TypedBytes> bodyParameters = request.getBodyParameters();
        if (bodyParameters != null && !bodyParameters.isEmpty()) {
          MultipartEntity entity = new MultipartEntity(BROWSER_COMPATIBLE);
          for (Map.Entry<String, TypedBytes> entry : bodyParameters.entrySet()) {
            String key = entry.getKey();
            entity.addPart(key, new TypedBytesBody(entry.getValue(), key));
          }
          setEntity(entity);
        }
      }
    }

    @Override public String getMethod() {
      return method;
    }
  }

  /** Adapts ContentBody to TypedBytes. */
  private static class TypedBytesBody extends AbstractContentBody {
    private final TypedBytes typedBytes;

    TypedBytesBody(TypedBytes typedBytes, String baseName) {
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

  /** Container class for passing an entire {@link TypedBytes} as an HTTP request. */
  static class TypedBytesEntity extends AbstractHttpEntity {
    private final TypedBytes typedBytes;

    TypedBytesEntity(TypedBytes typedBytes) {
      this.typedBytes = typedBytes;
      setContentType(typedBytes.mimeType());
    }

    @Override public boolean isRepeatable() {
      return true;
    }

    @Override public long getContentLength() {
      return typedBytes.length();
    }

    @Override public InputStream getContent() throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      typedBytes.writeTo(out);
      return new ByteArrayInputStream(out.toByteArray());
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      typedBytes.writeTo(out);
    }

    @Override public boolean isStreaming() {
      return false;
    }
  }
}
