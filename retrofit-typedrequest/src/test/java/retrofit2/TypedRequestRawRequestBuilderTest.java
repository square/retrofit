/*
 * Copyright (C) 2015 Square, Inc.
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
package retrofit2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class TypedRequestRawRequestBuilderTest {
  @Rule public final MockWebServer server = new MockWebServer();

  private Retrofit retrofit;

  @Before public void setUp() {
    retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/").toString())
        .addConverterFactory(new StringConverterFactory())
        .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
        .build();
  }

  @Test public void get() {
    okhttp3.Request request = buildRequest();
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());
    assertThat(request.body()).isNull();
  }

  @Test public void delete() {
    okhttp3.Request request = buildRequest(Method.DELETE);
    assertThat(request.method()).isEqualTo("DELETE");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());
    assertThat(request.body()).isNull();
  }

  @Test public void head() {
    okhttp3.Request request = buildRequest(Method.HEAD);
    assertThat(request.method()).isEqualTo("HEAD");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());
    assertThat(request.body()).isNull();
  }

  @Test public void post() {
    RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "hi");
    okhttp3.Request request = buildRequest(Method.POST, body);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());
    assertBody(request.body(), "hi");
  }

  @Test public void put() {
    RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "hi");
    okhttp3.Request request = buildRequest(Method.PUT, body);
    assertThat(request.method()).isEqualTo("PUT");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());
    assertBody(request.body(), "hi");
  }

  @Test public void patch() {
    RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "hi");
    okhttp3.Request request = buildRequest(Method.PATCH, body);
    assertThat(request.method()).isEqualTo("PATCH");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());
    assertBody(request.body(), "hi");
  }

  @Test public void getWithPathParam() {
    okhttp3.Request request = buildRequest(Method.GET, null, "/foo/bar/po ng/");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/po%20ng/").toString());
    assertThat(request.body()).isNull();
  }

  @Test public void getWithEncodedPathParam() {
    okhttp3.Request request = buildRequest(Method.GET, null, "/foo/bar/po%20ng/");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/po%20ng/").toString());
    assertThat(request.body()).isNull();
  }

  @Test public void getWithQueryParam() {
    okhttp3.Request request =
        buildRequest(Method.GET, null, "/foo/bar/", ImmutableList.of(new Query("ping", "pong")));
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/?ping=pong").toString());
    assertThat(request.body()).isNull();
  }

  @Test public void getWithEncodeQueryParam() {
    okhttp3.Request request = buildRequest(
        Method.GET, null, "/foo/bar/", ImmutableList.of(new Query("pi ng", "p o n g", false)));
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(
        server.url("/foo/bar/?pi+ng=p+o+n+g").toString());
    assertThat(request.body()).isNull();
  }

  @Test public void getWithEncodedQueryParam() {
    okhttp3.Request request = buildRequest(
        Method.GET, null, "/foo/bar/",
        ImmutableList.of(new Query("pi%20ng", "p%20o%20n%20g", true)));
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(
        server.url("/foo/bar/?pi%20ng=p%20o%20n%20g").toString());
    assertThat(request.body()).isNull();
  }

  @Test public void getWithQueryUrlAndParam() {
    okhttp3.Request request = buildRequest(
        Method.GET, null, "/foo/bar/?hi=mom", ImmutableList.of(new Query("ping", "pong")));
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/?hi=mom&ping=pong").toString());
    assertThat(request.body()).isNull();
  }

  @Test public void getWithQuery() {
    okhttp3.Request request = buildRequest(Method.GET, null, "/foo/bar/?hi=mom");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/?hi=mom").toString());
    assertThat(request.body()).isNull();
  }

  @Test public void queryParamOptionalOmitsQuery() {
    okhttp3.Request request = buildRequest(
        Method.GET, null, "/foo/bar/", ImmutableList.of(new Query("ping", null)));
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());
  }

  @Test public void queryParamOptional() {
    okhttp3.Request request = buildRequest(
        Method.GET, null, "/foo/bar/", ImmutableList.of(new Query("foo", "bar"),
            new Query("ping", null),
            new Query("kit", "kat")));
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/?foo=bar&kit=kat").toString());
  }

  @Test public void bodyGson() {
    okhttp3.Request request =
        buildRequest(Method.POST, Arrays.asList("quick", "brown", "fox"));
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());
    assertBody(request.body(), "[\"quick\",\"brown\",\"fox\"]");
  }

  @Test public void emptyBody() {
    okhttp3.Request request = buildRequest(Method.POST, null);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());
    assertBody(request.body(), "");
  }

  @Test public void bodyRequestBody() {
    RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "hi");
    okhttp3.Request request = buildRequest(Method.POST, body);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());
    assertBody(request.body(), "hi");
  }

  @Test public void simpleMultipart() throws IOException {
    CallableRequest multipartRequest = new CallableRequest.Builder(retrofit)
        .method(Method.POST)
        .path("/foo/bar/")
        .responseType(String.class)
        .parts(ImmutableList.of(
            new Part("ping", "pong"),
            new Part("kit", RequestBody.create(MediaType.parse("text/plain"), "kat"))))
        .build();
    okhttp3.Request request = buildRequest(multipartRequest);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(body.contentType().toString()).contains("multipart/form-data; boundary=");

    assertThat(bodyString)
        .contains("Content-Disposition: form-data; name=\"ping\"\r\n")
        .contains("\r\npong\r\n--");

    assertThat(bodyString)
        .contains("Content-Disposition: form-data; name=\"kit\"")
        .contains("\r\nkat\r\n--");
  }

  @Test public void multipartWithEncoding() throws IOException {
    CallableRequest multipartRequest = new CallableRequest.Builder(retrofit)
        .method(Method.POST)
        .path("/foo/bar/")
        .responseType(String.class)
        .parts(ImmutableList.of(
            new Part("ping", "pong", "8-bit"),
            new Part("kit", RequestBody.create(MediaType.parse("text/plain"), "kat"), "7-bit")))
        .build();

    okhttp3.Request request = buildRequest(multipartRequest);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(body.contentType().toString()).contains("multipart/form-data; boundary=");

    assertThat(bodyString)
        .contains("Content-Disposition: form-data; name=\"ping\"\r\n")
        .contains("Content-Transfer-Encoding: 8-bit")
        .contains("\r\npong\r\n--");

    assertThat(bodyString)
        .contains("Content-Disposition: form-data; name=\"kit\"")
        .contains("Content-Transfer-Encoding: 7-bit")
        .contains("\r\nkat\r\n--");
  }

  @Test public void multipartWithFilename() throws IOException {
    CallableRequest multipartRequest = new CallableRequest.Builder(retrofit)
        .method(Method.POST)
        .path("/foo/bar/")
        .responseType(String.class)
        .parts(ImmutableList.of(
            new Part("a", RequestBody.create(MediaType.parse("text/plain"), "b"), "binary", "bam")))
        .build();

    okhttp3.Request request = buildRequest(multipartRequest);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(body.contentType().toString()).contains("multipart/form-data; boundary=");

    assertThat(bodyString)
        .contains("Content-Disposition: form-data; name=\"a\"; filename=\"bam\"")
        .contains("\r\nb\r\n--");
  }

  @Test public void multipartPartMapRejectsNullKeys() {
    CallableRequest multipartRequest = new CallableRequest.Builder(retrofit)
        .method(Method.POST)
        .path("/foo")
        .responseType(String.class)
        .parts(ImmutableList.of(
            new Part("ping", "pong"),
            new Part(null, RequestBody.create(MediaType.parse("text/plain"), "kat"))))
        .build();
    try {
      buildRequest(multipartRequest);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Part map contained null key.");
    }
  }

  @Test public void multipartPartOptional() {
    try {
      new CallableRequest.Builder(retrofit)
          .method(Method.POST)
          .path("/foo")
          .responseType(String.class)
          .parts(Lists.<Part>emptyList())
          .build();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Multipart method must contain at least one part.");
    }
  }

  @Test public void formEncodedFieldMap() {
    CallableRequest urlEncodedRequest = new CallableRequest.Builder(retrofit)
        .method(Method.POST)
        .path("/foo")
        .responseType(String.class)
        .fields(ImmutableList.of(
            new Field("kit", "kat"),
            new Field("foo", null),
            new Field("ping", "pong")))
        .build();

    okhttp3.Request request = buildRequest(urlEncodedRequest);
    assertBody(request.body(), "kit=kat&ping=pong");
  }

  @Test public void formEncodedWithEncodedFieldParamMap() {
    CallableRequest urlEncodedRequest = new CallableRequest.Builder(retrofit)
        .method(Method.POST)
        .path("/foo")
        .responseType(String.class)
        .fields(ImmutableList.of(
            new Field("k%20it", "k%20at", true), new Field("pin%20g", "po%20ng", true)))
        .build();
    okhttp3.Request request = buildRequest(urlEncodedRequest);
    assertBody(request.body(), "k%20it=k%20at&pin%20g=po%20ng");
  }

  @Test public void formEncodedWithNonEncodedFieldParamMap() {
    CallableRequest urlEncodedRequest = new CallableRequest.Builder(retrofit)
        .method(Method.POST)
        .path("/foo")
        .responseType(String.class)
        .fields(ImmutableList.of(
            new Field("k it", "k at", false), new Field("pin g", "po ng", false)))
        .build();
    okhttp3.Request request = buildRequest(urlEncodedRequest);
    assertBody(request.body(), "k%20it=k%20at&pin%20g=po%20ng");
  }

  @Test public void fieldMapRejectsNullKeys() {
    CallableRequest urlEncodedRequest = new CallableRequest.Builder(retrofit)
        .method(Method.POST)
        .path("/foo")
        .responseType(String.class)
        .fields(ImmutableList.of(
            new Field("kit", "kat"), new Field("foo", null), new Field(null, "pong")))
        .build();
    try {
      buildRequest(urlEncodedRequest);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Parameter field map contained null key.");
    }
  }

  @Test public void simpleHeaders() {
    CallableRequest simpleRequest = new CallableRequest.Builder(retrofit)
        .method(Method.GET)
        .path("/foo/bar/")
        .responseType(String.class)
        .headers(ImmutableMap.of("ping", "pong", "kit", "kat"))
        .build();
    okhttp3.Request request = buildRequest(simpleRequest);
    assertThat(request.method()).isEqualTo("GET");
    okhttp3.Headers headers = request.headers();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.get("ping")).isEqualTo("pong");
    assertThat(headers.get("kit")).isEqualTo("kat");
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());
    assertThat(request.body()).isNull();
  }

  @Test public void tags() {
    CallableRequest simpleRequest = new CallableRequest.Builder(retrofit)
        .method(Method.GET)
        .path("/foo/bar/")
        .tag("hello")
        .responseType(String.class)
        .build();
    okhttp3.Request request = buildRequest(simpleRequest);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());
    assertThat(request.tag()).isEqualTo("hello");
    assertThat(request.body()).isNull();
  }

  @Test public void headerParamToString() {
    CallableRequest simpleRequest = new CallableRequest.Builder(retrofit)
        .method(Method.GET)
        .path("/foo/bar/")
        .responseType(String.class)
        .headers(ImmutableMap.of("kit", "1234"))
        .build();
    okhttp3.Request request = buildRequest(simpleRequest);
    assertThat(request.method()).isEqualTo("GET");
    okhttp3.Headers headers = request.headers();
    assertThat(headers.size()).isEqualTo(1);
    assertThat(headers.get("kit")).isEqualTo("1234");
    assertThat(request.url().toString()).isEqualTo(server.url("/foo/bar/").toString());
    assertThat(request.body()).isNull();
  }

  @Test public void contentTypeAnnotationHeaderOverrides() {
    CallableRequest simpleRequest = new CallableRequest.Builder(retrofit)
        .method(Method.POST)
        .path("/foo/bar/")
        .responseType(String.class)
        .headers(ImmutableMap.of("Content-Type", "text/not-plain"))
        .body(RequestBody.create(MediaType.parse("text/plain"), "hi"))
        .build();
    okhttp3.Request request = buildRequest(simpleRequest);
    assertThat(request.body().contentType().toString()).isEqualTo("text/not-plain");
  }

  @Test public void contentTypeAnnotationHeaderAddsHeaderWithNoBody() {
    CallableRequest simpleRequest = new CallableRequest.Builder(retrofit)
        .method(Method.DELETE)
        .path("/foo/bar/")
        .responseType(String.class)
        .headers(ImmutableMap.of("Content-Type", "text/not-plain"))
        .build();
    okhttp3.Request request = buildRequest(simpleRequest);
    assertThat(request.headers().get("Content-Type")).isEqualTo("text/not-plain");
  }

  private static void assertBody(RequestBody body, String expected) {
    assertThat(body).isNotNull();
    Buffer buffer = new Buffer();
    try {
      body.writeTo(buffer);
      assertThat(buffer.readUtf8()).isEqualTo(expected);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private okhttp3.Request buildRequest() {
    return buildRequest(Method.GET);
  }

  private okhttp3.Request buildRequest(Method method) {
    return buildRequest(method, null);
  }

  private okhttp3.Request buildRequest(Method method, Object body) {
    return buildRequest(method, body, "/foo/bar/");
  }

  private okhttp3.Request buildRequest(Method method, Object body, String path) {
    return buildRequest(method, body, path, Collections.<Query>emptyList());
  }

  private okhttp3.Request buildRequest(Method method, Object body, String path,
      List<Query> query) {
    return buildRequest(new CallableRequest.Builder(retrofit)
        .method(method)
        .body(body)
        .path(path)
        .responseType(String.class)
        .queryParams(query)
        .build());
  }

  private okhttp3.Request buildRequest(TypedRequest request) {
    return new TypedRequestRawRequestBuilder(retrofit, request).build();
  }
}