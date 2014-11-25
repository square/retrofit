// Copyright 2014 Square, Inc.
package retrofit.appengine;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import retrofit.TestingUtils;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.mime.TypedOutput;
import retrofit.mime.TypedString;

import static com.google.appengine.api.urlfetch.HTTPMethod.GET;
import static com.google.appengine.api.urlfetch.HTTPMethod.POST;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static retrofit.TestingUtils.assertBytes;

public class UrlFetchClientTest {
  private static final String HOST = "http://example.com";

  @Test public void get() throws IOException {
    Request request = new Request("GET", HOST + "/foo/bar/?kit=kat", null, null);
    HTTPRequest fetchRequest = UrlFetchClient.createRequest(request);

    assertThat(fetchRequest.getMethod()).isEqualTo(GET);
    assertThat(fetchRequest.getURL().toString()).isEqualTo(HOST + "/foo/bar/?kit=kat");
    assertThat(fetchRequest.getHeaders()).isEmpty();
    assertThat(fetchRequest.getPayload()).isNull();
  }

  @Test public void post() throws IOException {
    TypedString body = new TypedString("hi");
    Request request = new Request("POST", HOST + "/foo/bar/", null, body);
    HTTPRequest fetchRequest = UrlFetchClient.createRequest(request);

    assertThat(fetchRequest.getMethod()).isEqualTo(POST);
    assertThat(fetchRequest.getURL().toString()).isEqualTo(HOST + "/foo/bar/");
    List<HTTPHeader> fetchHeaders = fetchRequest.getHeaders();
    assertThat(fetchHeaders).hasSize(1);
    assertHeader(fetchHeaders.get(0), "Content-Type", "text/plain; charset=UTF-8");
    assertBytes(fetchRequest.getPayload(), "hi");
  }

  @Test public void multipart() throws IOException {
    Map<String, TypedOutput> bodyParams = new LinkedHashMap<String, TypedOutput>();
    bodyParams.put("foo", new TypedString("bar"));
    bodyParams.put("ping", new TypedString("pong"));
    TypedOutput body = TestingUtils.createMultipart(bodyParams);
    Request request = new Request("POST", HOST + "/that/", null, body);
    HTTPRequest fetchRequest = UrlFetchClient.createRequest(request);

    assertThat(fetchRequest.getMethod()).isEqualTo(POST);
    assertThat(fetchRequest.getURL().toString()).isEqualTo(HOST + "/that/");
    List<HTTPHeader> fetchHeaders = fetchRequest.getHeaders();
    assertThat(fetchHeaders).hasSize(1);
    assertHeader(fetchHeaders.get(0), "Content-Type", "multipart/form-data; boundary=foobarbaz");
    assertThat(fetchRequest.getPayload()).isNotEmpty();
  }

  @Test public void headers() throws IOException {
    List<Header> headers = new ArrayList<Header>();
    headers.add(new Header("kit", "kat"));
    headers.add(new Header("foo", "bar"));
    Request request = new Request("GET", HOST + "/this/", headers, null);
    HTTPRequest fetchRequest = UrlFetchClient.createRequest(request);

    List<HTTPHeader> fetchHeaders = fetchRequest.getHeaders();
    assertThat(fetchHeaders).hasSize(2);
    assertHeader(fetchHeaders.get(0), "kit", "kat");
    assertHeader(fetchHeaders.get(1), "foo", "bar");
  }

  @Test public void response() throws Exception {
    HTTPRequest creatingRequest = mock(HTTPRequest.class);

    HTTPResponse fetchResponse = mock(HTTPResponse.class);
    when(fetchResponse.getHeaders()).thenReturn(
        asList(new HTTPHeader("foo", "bar"), new HTTPHeader("kit", "kat"),
            new HTTPHeader("Content-Type", "text/plain")));
    when(fetchResponse.getContent()).thenReturn("hello".getBytes("UTF-8"));
    when(fetchResponse.getFinalUrl()).thenReturn(new URL(HOST + "/foo/bar/"));
    when(fetchResponse.getResponseCode()).thenReturn(200);

    Response response = UrlFetchClient.parseResponse(fetchResponse, creatingRequest);

    assertThat(response.getUrl()).isEqualTo(HOST + "/foo/bar/");
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getReason()).isEqualTo("");
    assertThat(response.getHeaders()).hasSize(3) //
        .containsOnly(new Header("foo", "bar"), new Header("kit", "kat"),
            new Header("Content-Type", "text/plain"));
    assertBytes(ByteStreams.toByteArray(response.getBody().in()), "hello");

    verifyNoMoreInteractions(creatingRequest);
  }

  @Test public void responseNullUrlPullsFromRequest() throws Exception {
    HTTPRequest creatingRequest = mock(HTTPRequest.class);
    when(creatingRequest.getURL()).thenReturn(new URL(HOST + "/foo/bar/"));

    HTTPResponse fetchResponse = mock(HTTPResponse.class);
    when(fetchResponse.getHeaders()).thenReturn(
        asList(new HTTPHeader("foo", "bar"), new HTTPHeader("kit", "kat"),
            new HTTPHeader("Content-Type", "text/plain")));
    when(fetchResponse.getContent()).thenReturn("hello".getBytes("UTF-8"));
    when(fetchResponse.getFinalUrl()).thenReturn(null);
    when(fetchResponse.getResponseCode()).thenReturn(200);

    Response response = UrlFetchClient.parseResponse(fetchResponse, creatingRequest);

    assertThat(response.getUrl()).isEqualTo(HOST + "/foo/bar/");
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getReason()).isEqualTo("");
    assertThat(response.getHeaders()).hasSize(3) //
        .containsOnly(new Header("foo", "bar"), new Header("kit", "kat"),
            new Header("Content-Type", "text/plain"));
    assertBytes(ByteStreams.toByteArray(response.getBody().in()), "hello");
  }

  @Test public void emptyResponse() throws Exception {
    HTTPRequest creatingRequest = mock(HTTPRequest.class);

    HTTPResponse fetchResponse = mock(HTTPResponse.class);
    when(fetchResponse.getHeaders()).thenReturn(
        asList(new HTTPHeader("foo", "bar"), new HTTPHeader("kit", "kat")));
    when(fetchResponse.getContent()).thenReturn(null);
    when(fetchResponse.getFinalUrl()).thenReturn(new URL(HOST + "/foo/bar/"));
    when(fetchResponse.getResponseCode()).thenReturn(200);

    Response response = UrlFetchClient.parseResponse(fetchResponse, creatingRequest);

    assertThat(response.getUrl()).isEqualTo(HOST + "/foo/bar/");
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getReason()).isEqualTo("");
    assertThat(response.getHeaders()).hasSize(2) //
        .containsExactly(new Header("foo", "bar"), new Header("kit", "kat"));
    assertThat(response.getBody()).isNull();

    verifyNoMoreInteractions(creatingRequest);
  }

  private static void assertHeader(HTTPHeader header, String name, String value) {
    assertThat(header.getName()).isEqualTo(name);
    assertThat(header.getValue()).isEqualTo(value);
  }
}
