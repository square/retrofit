// Copyright 2013 Square, Inc.
package retrofit.http.client;

import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import retrofit.http.Header;
import retrofit.http.TestingUtils;
import retrofit.http.mime.TypedOutput;
import retrofit.http.mime.TypedString;

import static org.fest.assertions.api.Assertions.assertThat;
import static retrofit.http.TestingUtils.assertBytes;

public class UrlConnectionClientTest {
  private static final String HOST = "http://example.com";

  private UrlConnectionClient client = new UrlConnectionClient() {
    @Override protected HttpURLConnection openConnection(Request request) throws IOException {
      return new DummyHttpUrlConnection(request.getUrl());
    }
  };

  @Test public void get() throws Exception {
    Request request = new Request("GET", HOST + "/foo/bar/?kit=kat", null, null);

    HttpURLConnection connection = client.openConnection(request);
    client.prepareRequest(connection, request);

    assertThat(connection.getRequestMethod()).isEqualTo("GET");
    assertThat(connection.getURL().toString()).isEqualTo(HOST + "/foo/bar/?kit=kat");
    assertThat(connection.getHeaderFields()).isEmpty();
  }

  @Test public void post() throws Exception {
    TypedString body = new TypedString("hi");
    Request request = new Request("POST", HOST + "/foo/bar/", null, body);

    DummyHttpUrlConnection connection = (DummyHttpUrlConnection) client.openConnection(request);
    client.prepareRequest(connection, request);

    assertThat(connection.getRequestMethod()).isEqualTo("POST");
    assertThat(connection.getURL().toString()).isEqualTo(HOST + "/foo/bar/");
    assertThat(connection.getRequestProperties()).hasSize(2);
    assertThat(connection.getRequestProperty("Content-Type")) //
        .isEqualTo("text/plain; charset=UTF-8");
    assertThat(connection.getRequestProperty("Content-Length")).isEqualTo("2");
    assertBytes(connection.getOutputStream().toByteArray(), "hi");
  }

  @Test public void multipart() throws Exception {
    Map<String, TypedOutput> bodyParams = new LinkedHashMap<String, TypedOutput>();
    bodyParams.put("foo", new TypedString("bar"));
    bodyParams.put("ping", new TypedString("pong"));
    TypedOutput body = TestingUtils.createMultipart(bodyParams);
    Request request = new Request("POST", HOST + "/that/", null, body);

    DummyHttpUrlConnection connection = (DummyHttpUrlConnection) client.openConnection(request);
    client.prepareRequest(connection, request);

    assertThat(connection.getRequestMethod()).isEqualTo("POST");
    assertThat(connection.getURL().toString()).isEqualTo(HOST + "/that/");
    assertThat(connection.getRequestProperties()).hasSize(2);
    assertThat(connection.getRequestProperty("Content-Type")).startsWith("multipart/form-data;");
    assertThat(connection.getRequestProperty("Content-Length")).isNotNull();
    assertThat(connection.getOutputStream().toByteArray().length).isGreaterThan(0);
  }

  @Test public void headers() throws Exception {
    List<Header> headers = new ArrayList<Header>();
    headers.add(new Header("kit", "kat"));
    headers.add(new Header("foo", "bar"));
    Request request = new Request("GET", HOST + "/this/", headers, null);

    HttpURLConnection connection = client.openConnection(request);
    client.prepareRequest(connection, request);

    assertThat(connection.getRequestProperties()).hasSize(2);
    assertThat(connection.getRequestProperty("kit")).isEqualTo("kat");
    assertThat(connection.getRequestProperty("foo")).isEqualTo("bar");
  }

  @Test public void response() throws Exception {
    DummyHttpUrlConnection connection = new DummyHttpUrlConnection(HOST);
    connection.setResponseCode(200);
    connection.setResponseMessage("OK");
    connection.addResponseHeader("Content-Type", "text/plain");
    connection.addResponseHeader("foo", "bar");
    connection.addResponseHeader("kit", "kat");
    connection.setInputStream(new ByteArrayInputStream("hello".getBytes("UTF-8")));
    Response response = client.readResponse(connection);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getReason()).isEqualTo("OK");
    assertThat(response.getHeaders()).hasSize(3) //
        .containsOnly(new Header("foo", "bar"), new Header("kit", "kat"),
            new Header("Content-Type", "text/plain"));
    assertBytes(ByteStreams.toByteArray(response.getBody().in()), "hello");
  }

  @Test public void createdResponse() throws Exception {
    DummyHttpUrlConnection connection = new DummyHttpUrlConnection(HOST);
    connection.setResponseCode(201);
    connection.setResponseMessage("OK");
    connection.addResponseHeader("Content-Type", "text/plain");
    connection.addResponseHeader("foo", "bar");
    connection.addResponseHeader("kit", "kat");
    connection.setInputStream(new ByteArrayInputStream("hello".getBytes("UTF-8")));
    Response response = client.readResponse(connection);

    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getReason()).isEqualTo("OK");
    assertThat(response.getHeaders()).hasSize(3) //
        .containsOnly(new Header("foo", "bar"), new Header("kit", "kat"),
            new Header("Content-Type", "text/plain"));
    assertBytes(ByteStreams.toByteArray(response.getBody().in()), "hello");
  }

  @Test public void errorResponse() throws Exception {
    DummyHttpUrlConnection connection = new DummyHttpUrlConnection(HOST);
    connection.setResponseCode(401);
    connection.setResponseMessage("Not Authorized");
    connection.addResponseHeader("Content-Type", "text/plain");
    connection.setInputStream(new ByteArrayInputStream("input".getBytes("UTF-8")));
    connection.setErrorStream(new ByteArrayInputStream("error".getBytes("UTF-8")));
    Response response = client.readResponse(connection);

    assertBytes(ByteStreams.toByteArray(response.getBody().in()), "error");
  }

  @Test public void emptyResponse() throws Exception {
    DummyHttpUrlConnection connection = new DummyHttpUrlConnection(HOST);
    connection.setResponseCode(200);
    connection.setResponseMessage("OK");
    connection.addResponseHeader("foo", "bar");
    connection.addResponseHeader("kit", "kat");
    Response response = client.readResponse(connection);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getReason()).isEqualTo("OK");
    assertThat(response.getHeaders()).hasSize(2) //
        .containsExactly(new Header("foo", "bar"), new Header("kit", "kat"));
  }
}
