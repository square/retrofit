// Copyright 2014 Square, Inc.
package retrofit.client;

import com.google.common.io.ByteStreams;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import okio.Buffer;
import okio.BufferedSource;
import org.junit.Test;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedString;

import static org.assertj.core.api.Assertions.assertThat;
import static retrofit.TestingUtils.assertBytes;

public final class OkClientTest {
  private static final String HOST = "http://example.com";

  @Test public void get() {
    Request request = new Request("GET", HOST + "/foo/bar/?kit=kat", Headers.of(), null);
    com.squareup.okhttp.Request okRequest = OkClient.createRequest(request);

    assertThat(okRequest.method()).isEqualTo("GET");
    assertThat(okRequest.urlString()).isEqualTo(HOST + "/foo/bar/?kit=kat");
    assertThat(okRequest.headers().size()).isEqualTo(0);
    assertThat(okRequest.body()).isNull();
  }

  @Test public void post() throws IOException {
    TypedString body = new TypedString("hi");
    Request request = new Request("POST", HOST + "/foo/bar/", Headers.of(), body);
    com.squareup.okhttp.Request okRequest = OkClient.createRequest(request);

    assertThat(okRequest.method()).isEqualTo("POST");
    assertThat(okRequest.urlString()).isEqualTo(HOST + "/foo/bar/");
    assertThat(okRequest.headers().size()).isEqualTo(0);

    RequestBody okBody = okRequest.body();
    assertThat(okBody).isNotNull();

    Buffer buffer = new Buffer();
    okBody.writeTo(buffer);
    assertThat(buffer.readUtf8()).isEqualTo("hi");
  }

  @Test public void headers() {
    Headers headers = new Headers.Builder()
        .add("kit", "kat")
        .add("foo", "bar")
        .add("ping", "")
        .build();
    Request request = new Request("GET", HOST + "/this/", headers, null);
    com.squareup.okhttp.Request okRequest = OkClient.createRequest(request);

    Headers okHeaders = okRequest.headers();
    assertThat(okHeaders.size()).isEqualTo(3);
    assertThat(okHeaders.get("kit")).isEqualTo("kat");
    assertThat(okHeaders.get("foo")).isEqualTo("bar");
    assertThat(okHeaders.get("ping")).isEqualTo("");
  }

  @Test public void response() throws IOException {
    com.squareup.okhttp.Response okResponse = new com.squareup.okhttp.Response.Builder()
        .code(200).message("OK")
        .body(new TestResponseBody("hello", "text/plain"))
        .addHeader("foo", "bar")
        .addHeader("kit", "kat")
        .protocol(Protocol.HTTP_1_1)
        .request(new com.squareup.okhttp.Request.Builder()
            .url(HOST + "/foo/bar/")
            .get()
            .build())
        .build();
    Response response = OkClient.parseResponse(okResponse);

    assertThat(response.getUrl()).isEqualTo(HOST + "/foo/bar/");
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getReason()).isEqualTo("OK");
    Headers headers = response.getHeaders();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.get("foo")).isEqualTo("bar");
    assertThat(headers.get("kit")).isEqualTo("kat");
    TypedInput responseBody = response.getBody();
    assertThat(responseBody.mimeType()).isEqualTo("text/plain");
    assertBytes(ByteStreams.toByteArray(responseBody.in()), "hello");
  }

  @Test public void responseNoContentType() throws IOException {
    com.squareup.okhttp.Response okResponse = new com.squareup.okhttp.Response.Builder()
        .code(200).message("OK")
        .body(new TestResponseBody("hello", null))
        .addHeader("foo", "bar")
        .addHeader("kit", "kat")
        .protocol(Protocol.HTTP_1_1)
        .request(new com.squareup.okhttp.Request.Builder()
            .url(HOST + "/foo/bar/")
            .get()
            .build())
        .build();
    Response response = OkClient.parseResponse(okResponse);

    assertThat(response.getUrl()).isEqualTo(HOST + "/foo/bar/");
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getReason()).isEqualTo("OK");
    Headers headers = response.getHeaders();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.get("foo")).isEqualTo("bar");
    assertThat(headers.get("kit")).isEqualTo("kat");
    TypedInput responseBody = response.getBody();
    assertThat(responseBody.mimeType()).isNull();
    assertBytes(ByteStreams.toByteArray(responseBody.in()), "hello");
  }

  @Test public void emptyResponse() throws IOException {
    com.squareup.okhttp.Response okResponse = new com.squareup.okhttp.Response.Builder()
        .code(200)
        .message("OK")
        .body(new TestResponseBody("", null))
        .addHeader("foo", "bar")
        .addHeader("kit", "kat")
        .protocol(Protocol.HTTP_1_1)
        .request(new com.squareup.okhttp.Request.Builder()
            .url(HOST + "/foo/bar/")
            .get()
            .build())
        .build();
    Response response = OkClient.parseResponse(okResponse);

    assertThat(response.getUrl()).isEqualTo(HOST + "/foo/bar/");
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getReason()).isEqualTo("OK");
    Headers headers = response.getHeaders();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.get("foo")).isEqualTo("bar");
    assertThat(headers.get("kit")).isEqualTo("kat");
    assertThat(response.getBody()).isNull();
  }

  private static final class TestResponseBody extends ResponseBody {
    private final Buffer buffer;
    private final String contentType;

    private TestResponseBody(String content, String contentType) {
      this.buffer = new Buffer().writeUtf8(content);
      this.contentType = contentType;
    }

    @Override public MediaType contentType() {
      return contentType == null ? null : MediaType.parse(contentType);
    }

    @Override public long contentLength() {
      return buffer.size();
    }

    @Override public BufferedSource source() {
      return buffer.clone();
    }
  }
}
