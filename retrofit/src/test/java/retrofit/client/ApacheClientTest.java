// Copyright 2013 Square, Inc.
package retrofit.client;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Test;
import retrofit.TestingUtils;
import retrofit.mime.MultipartTypedOutput;
import retrofit.mime.TypedOutput;
import retrofit.mime.TypedString;

import static org.assertj.core.api.Assertions.assertThat;
import static retrofit.TestingUtils.assertBytes;
import static retrofit.client.ApacheClient.TypedOutputEntity;

public class ApacheClientTest {
  private static final String HOST = "http://example.com";

  @Test public void get() {
    Request request = new Request("GET", HOST + "/foo/bar/?kit=kat", null, null);
    HttpUriRequest apacheRequest = ApacheClient.createRequest(request);

    assertThat(apacheRequest.getMethod()).isEqualTo("GET");
    assertThat(apacheRequest.getURI().toString()).isEqualTo(HOST + "/foo/bar/?kit=kat");
    assertThat(apacheRequest.getAllHeaders()).isEmpty();

    if (apacheRequest instanceof HttpEntityEnclosingRequest) {
      HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) apacheRequest;
      assertThat(entityRequest.getEntity()).isNull();
    }
  }

  @Test public void post() throws IOException {
    TypedString body = new TypedString("hi");
    Request request = new Request("POST", HOST + "/foo/bar/", null, body);
    HttpUriRequest apacheRequest = ApacheClient.createRequest(request);

    assertThat(apacheRequest.getMethod()).isEqualTo("POST");
    assertThat(apacheRequest.getURI().toString()).isEqualTo(HOST + "/foo/bar/");
    assertThat(apacheRequest.getAllHeaders()).isEmpty();

    assertThat(apacheRequest).isInstanceOf(HttpEntityEnclosingRequest.class);
    HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) apacheRequest;
    HttpEntity entity = entityRequest.getEntity();
    assertThat(entity).isNotNull();
    assertBytes(ByteStreams.toByteArray(entity.getContent()), "hi");
  }

  @Test public void multipart() {
    Map<String, TypedOutput> bodyParams = new LinkedHashMap<String, TypedOutput>();
    bodyParams.put("foo", new TypedString("bar"));
    bodyParams.put("ping", new TypedString("pong"));
    TypedOutput body = TestingUtils.createMultipart(bodyParams);
    Request request = new Request("POST", HOST + "/that/", null, body);
    HttpUriRequest apacheRequest = ApacheClient.createRequest(request);

    assertThat(apacheRequest.getMethod()).isEqualTo("POST");
    assertThat(apacheRequest.getURI().toString()).isEqualTo(HOST + "/that/");
    assertThat(apacheRequest.getAllHeaders()).isEmpty();

    assertThat(apacheRequest).isInstanceOf(HttpEntityEnclosingRequest.class);
    HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) apacheRequest;
    TypedOutputEntity entity = (TypedOutputEntity) entityRequest.getEntity();
    assertThat(entity.typedOutput).isInstanceOf(MultipartTypedOutput.class);
    // TODO test more?
  }

  @Test public void headers() {
    List<Header> headers = new ArrayList<Header>();
    headers.add(new Header("kit", "kat"));
    headers.add(new Header("foo", "bar"));
    Request request = new Request("GET", HOST + "/this/", headers, null);
    HttpUriRequest apacheRequest = ApacheClient.createRequest(request);

    assertThat(apacheRequest.getAllHeaders()).hasSize(2);
    org.apache.http.Header kit = apacheRequest.getFirstHeader("kit");
    assertThat(kit).isNotNull();
    assertThat(kit.getValue()).isEqualTo("kat");
    org.apache.http.Header foo = apacheRequest.getFirstHeader("foo");
    assertThat(foo).isNotNull();
    assertThat(foo.getValue()).isEqualTo("bar");
  }

  @Test public void response() throws IOException {
    StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK");
    HttpResponse apacheResponse = new BasicHttpResponse(statusLine);
    apacheResponse.setEntity(new TypedOutputEntity(new TypedString("hello")));
    apacheResponse.addHeader("Content-Type", "text/plain");
    apacheResponse.addHeader("foo", "bar");
    apacheResponse.addHeader("kit", "kat");
    Response response = ApacheClient.parseResponse(HOST + "/foo/bar/", apacheResponse);

    assertThat(response.getUrl()).isEqualTo(HOST + "/foo/bar/");
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getReason()).isEqualTo("OK");
    assertThat(response.getHeaders()).hasSize(3) //
        .containsOnly(new Header("foo", "bar"), new Header("kit", "kat"),
            new Header("Content-Type", "text/plain"));
    assertBytes(ByteStreams.toByteArray(response.getBody().in()), "hello");
  }

  @Test public void emptyResponse() throws IOException {
    StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK");
    HttpResponse apacheResponse = new BasicHttpResponse(statusLine);
    apacheResponse.addHeader("foo", "bar");
    apacheResponse.addHeader("kit", "kat");
    Response response = ApacheClient.parseResponse(HOST + "/foo/bar/", apacheResponse);

    assertThat(response.getUrl()).isEqualTo(HOST + "/foo/bar/");
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getReason()).isEqualTo("OK");
    assertThat(response.getHeaders()).hasSize(2) //
        .containsExactly(new Header("foo", "bar"), new Header("kit", "kat"));
    assertThat(response.getBody()).isNull();
  }
}
