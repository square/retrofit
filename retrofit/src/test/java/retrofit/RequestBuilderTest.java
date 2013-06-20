// Copyright 2013 Square, Inc.
package retrofit;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;
import retrofit.converter.Converter;
import retrofit.converter.GsonConverter;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.mime.MimeHelper;
import retrofit.mime.MultipartTypedOutput;
import retrofit.mime.TypedOutput;
import retrofit.mime.TypedString;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static retrofit.RestMethodInfo.ParamUsage;
import static retrofit.RestMethodInfo.ParamUsage.BODY;
import static retrofit.RestMethodInfo.ParamUsage.FIELD;
import static retrofit.RestMethodInfo.ParamUsage.HEADER;
import static retrofit.RestMethodInfo.ParamUsage.PART;
import static retrofit.RestMethodInfo.ParamUsage.PATH;
import static retrofit.RestMethodInfo.ParamUsage.QUERY;
import static retrofit.RestMethodInfo.RequestType;

public class RequestBuilderTest {
  @Test public void normalGet() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithPathParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addPathParam("ping", "pong") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void pathParamRequired() throws Exception {
    try {
      new Helper() //
          .setMethod("GET") //
          .setUrl("http://example.com") //
          .setPath("/foo/bar/{ping}/") //
          .addPathParam("ping", null) //
          .build();
      fail("Null path parameters not allowed.");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Path parameter \"ping\" value must not be null.");
    }
  }

  @Test public void getWithQueryParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addQueryParam("ping", "pong") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/?ping=pong");
    assertThat(request.getBody()).isNull();
  }

  @Test public void queryParamOptional() throws Exception {
    Request request1 = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addQueryParam("ping", null) //
        .build();
    assertThat(request1.getUrl()).isEqualTo("http://example.com/foo/bar/");

    Request request2 = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addQueryParam("foo", "bar") //
        .addQueryParam("ping", null) //
        .addQueryParam("kit", "kat") //
        .build();
    assertThat(request2.getUrl()).isEqualTo("http://example.com/foo/bar/?foo=bar&kit=kat");
  }

  @Test public void getWithQueryUrlAndParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .setQuery("hi=mom") //
        .addQueryParam("ping", "pong") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/?hi=mom&ping=pong");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithQuery() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .setQuery("hi=mom") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/?hi=mom");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithPathAndQueryParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addPathParam("ping", "pong") //
        .addQueryParam("kit", "kat") //
        .addQueryParam("riff", "raff") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/?kit=kat&riff=raff");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithPathAndQueryQuestionMarkParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addPathParam("ping", "pong?") //
        .addQueryParam("kit", "kat?") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong%3F/?kit=kat%3F");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithPathAndQueryAmpersandParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addPathParam("ping", "pong&") //
        .addQueryParam("kit", "kat&") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong%26/?kit=kat%26");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithPathAndQueryHashParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addPathParam("ping", "pong#") //
        .addQueryParam("kit", "kat#") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong%23/?kit=kat%23");
    assertThat(request.getBody()).isNull();
  }

  @Test public void normalPost() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void normalPostWithPathParam() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addPathParam("ping", "pong") //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void body() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .setBody(Arrays.asList("quick", "brown", "fox")) //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertTypedBytes(request.getBody(), "[\"quick\",\"brown\",\"fox\"]");
  }

  @Test public void bodyRequired() throws Exception {
    try {
      new Helper() //
          .setMethod("POST") //
          .setHasBody() //
          .setUrl("http://example.com") //
          .setPath("/foo/bar/") //
          .setBody(null) //
          .build();
      fail("Null body not allowed.");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Body parameter value must not be null.");
    }
  }

  @Test public void bodyWithPathParams() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/{kit}/") //
        .addPathParam("ping", "pong") //
        .setBody(Arrays.asList("quick", "brown", "fox")) //
        .addPathParam("kit", "kat") //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/kat/");
    assertTypedBytes(request.getBody(), "[\"quick\",\"brown\",\"fox\"]");
  }

  @Test public void simpleMultipart() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .setMultipart() //
        .addPart("ping", "pong") //
        .addPart("kit", new TypedString("kat")) //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");

    MultipartTypedOutput body = (MultipartTypedOutput) request.getBody();
    List<byte[]> bodyParts = MimeHelper.getParts(body);
    assertThat(bodyParts).hasSize(2);

    Iterator<byte[]> iterator = bodyParts.iterator();

    String one = new String(iterator.next(), "UTF-8");
    assertThat(one).contains("ping").contains("pong");

    String two = new String(iterator.next(), "UTF-8");
    assertThat(two).contains("kit").contains("kat");
  }

  @Test public void multipartPartOptional() throws Exception {
    try {
      new Helper() //
          .setMethod("POST") //
          .setHasBody() //
          .setUrl("http://example.com") //
          .setPath("/foo/bar/") //
          .setMultipart() //
          .addPart("ping", null) //
          .build();
      fail("Null multipart part is not allowed.");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Multipart part \"ping\" value must not be null.");
    }
  }

  @Test public void simpleFormEncoded() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo") //
        .setFormEncoded() //
        .addField("foo", "bar") //
        .addField("ping", "pong") //
        .build();
    assertTypedBytes(request.getBody(), "foo=bar&ping=pong");
  }

  @Test public void formEncodedFieldOptional() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo") //
        .setFormEncoded() //
        .addField("foo", "bar") //
        .addField("ping", null) //
        .addField("kit", "kat") //
        .build();
    assertTypedBytes(request.getBody(), "foo=bar&kit=kat");
  }

  @Test public void simpleHeaders() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addHeader("ping", "pong") //
        .addHeader("kit", "kat") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()) //
        .containsExactly(new Header("ping", "pong"), new Header("kit", "kat"));
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void methodHeader() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addHeader("ping", "pong") //
        .addHeaderParam("kit", "kat") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()) //
        .containsExactly(new Header("ping", "pong"), new Header("kit", "kat"));
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void headerParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addHeader("ping", "pong") //
        .addHeaderParam("kit", "kat") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()) //
        .containsExactly(new Header("ping", "pong"), new Header("kit", "kat"));
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void noDuplicateSlashes() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com/") //
        .setPath("/foo/bar/") //
        .build();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
  }

  private static void assertTypedBytes(TypedOutput bytes, String expected) throws IOException {
    assertThat(bytes).isNotNull();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bytes.writeTo(baos);
    assertThat(new String(baos.toByteArray(), "UTF-8")).isEqualTo(expected);
  }

  private static class Helper {
    private static final Converter GSON = new GsonConverter(new Gson());

    private RequestType requestType = RequestType.SIMPLE;
    private String method;
    private boolean hasBody = false;
    private String path;
    private String query;
    private final List<String> paramNames = new ArrayList<String>();
    private final List<ParamUsage> paramUsages = new ArrayList<ParamUsage>();
    private final List<Object> args = new ArrayList<Object>();
    private final List<Header> headers = new ArrayList<Header>();
    private String url;

    Helper setMethod(String method) {
      this.method = method;
      return this;
    }

    Helper setHasBody() {
      hasBody = true;
      return this;
    }

    Helper setUrl(String url) {
      this.url = url;
      return this;
    }

    Helper setPath(String path) {
      this.path = path;
      return this;
    }

    Helper setQuery(String query) {
      this.query = query;
      return this;
    }

    Helper addPathParam(String name, Object value) {
      paramNames.add(name);
      paramUsages.add(PATH);
      args.add(value);
      return this;
    }

    Helper addQueryParam(String name, String value) {
      paramNames.add(name);
      paramUsages.add(QUERY);
      args.add(value);
      return this;
    }

    Helper addField(String name, String value) {
      paramNames.add(name);
      paramUsages.add(FIELD);
      args.add(value);
      return this;
    }

    Helper addPart(String name, Object value) {
      paramNames.add(name);
      paramUsages.add(PART);
      args.add(value);
      return this;
    }

    Helper setBody(Object value) {
      paramNames.add(null);
      paramUsages.add(BODY);
      args.add(value);
      return this;
    }

    Helper addHeaderParam(String name, Object value) {
      paramNames.add(name);
      paramUsages.add(HEADER);
      args.add(value);
      return this;
    }

    Helper addHeader(String name, String value) {
      headers.add(new Header(name, value));
      return this;
    }

    Helper setMultipart() {
      requestType = RequestType.MULTIPART;
      return this;
    }

    Helper setFormEncoded() {
      requestType = RequestType.FORM_URL_ENCODED;
      return this;
    }

    Request build() throws Exception {
      if (method == null) {
        throw new IllegalStateException("Method must be set.");
      }
      if (path == null) {
        throw new IllegalStateException("Path must be set.");
      }

      Method method = getClass().getDeclaredMethod("dummySync");

      RestMethodInfo methodInfo = new RestMethodInfo(method);
      methodInfo.requestMethod = this.method;
      methodInfo.requestHasBody = hasBody;
      methodInfo.requestType = requestType;
      methodInfo.requestUrl = path;
      methodInfo.requestUrlParamNames = RestMethodInfo.parsePathParameters(path);
      methodInfo.requestQuery = query;
      methodInfo.requestParamNames = paramNames.toArray(new String[paramNames.size()]);
      methodInfo.requestParamUsage = paramUsages.toArray(new ParamUsage[paramUsages.size()]);
      methodInfo.loaded = true;

      RequestBuilder requestBuilder = new RequestBuilder(GSON, methodInfo);

      for (Header header : headers) {
        requestBuilder.addHeader(header.getName(), header.getValue());
      }

      requestBuilder.setApiUrl(url);
      requestBuilder.setArguments(args.toArray(new Object[args.size()]));

      return requestBuilder.build();
    }

    @SuppressWarnings("UnusedDeclaration") // Accessed via reflection.
    private Object dummySync() {
      return null;
    }
  }
}
