// Copyright 2013 Square, Inc.
package retrofit.http;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import retrofit.http.client.Request;
import retrofit.http.mime.TypedOutput;
import retrofit.http.mime.TypedString;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static retrofit.http.RestMethodInfo.NO_SINGLE_ENTITY;

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
        .addNamedParam("ping", "pong") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/");
    assertThat(request.getBody()).isNull();
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

  @Test public void getWithPathAndQueryParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addNamedParam("ping", "pong") //
        .addNamedParam("kit", "kat") //
        .addNamedParam("riff", "raff") //
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
        .addNamedParam("ping", "pong?") //
        .addNamedParam("kit", "kat?") //
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
        .addNamedParam("ping", "pong&") //
        .addNamedParam("kit", "kat&") //
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
        .addNamedParam("ping", "pong#") //
        .addNamedParam("kit", "kat#") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong%23/?kit=kat%23");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithPathAndQueryParamAsync() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addNamedParam("ping", "pong") //
        .addNamedParam("kit", "kat") //
        .setAsynchronous() //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/?kit=kat");
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
        .addNamedParam("ping", "pong") //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void singleEntity() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addSingleEntityParam(Arrays.asList("quick", "brown", "fox")) //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertTypedBytes(request.getBody(), "[\"quick\",\"brown\",\"fox\"]");
  }

  @Test public void singleEntityWithPathParams() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/{kit}/") //
        .addNamedParam("ping", "pong") //
        .addSingleEntityParam(Arrays.asList("quick", "brown", "fox")) //
        .addNamedParam("kit", "kat") //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/kat/");
    assertTypedBytes(request.getBody(), "[\"quick\",\"brown\",\"fox\"]");
  }

  @Test public void singleEntityWithPathParamsAsync() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/{kit}/") //
        .addNamedParam("ping", "pong") //
        .addSingleEntityParam(Arrays.asList("quick", "brown", "fox")) //
        .addNamedParam("kit", "kat") //
        .setAsynchronous() //
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
        .addNamedParam("ping", "pong") //
        .addNamedParam("kit", new TypedString("kat")) //
        .setMultipart() //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");

    MultipartTypedOutput body = (MultipartTypedOutput) request.getBody();
    assertThat(body.parts).hasSize(2);

    Iterator<byte[]> iterator = body.parts.iterator();

    String one = new String(iterator.next(), "UTF-8");
    assertThat(one).contains("ping").contains("pong");

    String two = new String(iterator.next(), "UTF-8");
    assertThat(two).contains("kit").contains("kat");
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
        .containsExactly(new HeaderPair("ping", "pong"), new HeaderPair("kit", "kat"));
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void methodHeader() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addHeader("ping", "pong") //
        .addMethodHeader("kit", "kat") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()) //
        .containsExactly(new HeaderPair("ping", "pong"), new HeaderPair("kit", "kat"));
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
        .containsExactly(new HeaderPair("ping", "pong"), new HeaderPair("kit", "kat"));
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void nullHeaderParamRemovesHeader() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addHeader("ping", "pong") //
        .addHeaderParam("ping", null) //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  //RFC 2616: Field names are case-insensitive
  @Test public void nullHeaderParamRemovesHeaderCaseInsensitive() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addHeader("ping", "pong") //
        .addHeaderParam("Ping", null) //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void nullHeaderParamRemovesMethodHeader() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addHeader("ping", "pong") //
        .addMethodHeader("kit", "kat") //
        .addHeaderParam("kit", null) //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()) //
        .containsExactly(new HeaderPair("ping", "pong"));
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

    private boolean isSynchronous = true;
    private boolean isMultipart = false;
    private String method;
    private boolean hasBody = false;
    private String path;
    private Set<String> pathParams;
    private final List<QueryParam> queryParams = new ArrayList<QueryParam>();
    private final List<String> headerParams = new ArrayList<String>();
    private final List<String> namedParams = new ArrayList<String>();
    private final List<Object> args = new ArrayList<Object>();
    private final List<HeaderPair> headers = new ArrayList<HeaderPair>();
    private final List<HeaderPair> methodHeaders = new ArrayList<HeaderPair>();
    private int singleEntityArgumentIndex = NO_SINGLE_ENTITY;
    private String url;

    Helper setAsynchronous() {
      isSynchronous = false;
      return this;
    }

    Helper setMethod(String method) {
      this.method = method;
      return this;
    }

    Helper setHasBody() {
      hasBody = true;
      return this;
    }

    Helper setPath(String path) {
      this.path = path;
      pathParams = RestMethodInfo.parsePathParameters(path);
      return this;
    }

    Helper addQueryParam(String name, String value) {
      QueryParam queryParam = mock(QueryParam.class);
      when(queryParam.name()).thenReturn(name);
      when(queryParam.value()).thenReturn(value);
      queryParams.add(queryParam);
      return this;
    }

    Helper addNamedParam(String name, Object value) {
      if (name == null) {
        throw new IllegalArgumentException("Name can not be null.");
      }
      namedParams.add(name);
      args.add(value);
      return this;
    }

    Helper addHeaderParam(String name, Object value) {
      if (name == null) {
        throw new IllegalArgumentException("Name can not be null.");
      }
      headerParams.add(name);
      args.add(value);
      return this;
    }

    Helper addSingleEntityParam(Object value) {
      if (singleEntityArgumentIndex != NO_SINGLE_ENTITY) {
        throw new IllegalStateException("Single entity param already added.");
      }
      // Relying on the fact that this is already less one.
      singleEntityArgumentIndex = namedParams.size();
      namedParams.add(null);
      args.add(value);
      return this;
    }

    Helper addHeader(String name, String value) {
      headers.add(new HeaderPair(name, value));
      return this;
    }

    Helper addMethodHeader(String name, String value) {
      methodHeaders.add(new HeaderPair(name, value));
      return this;
    }

    Helper setMultipart() {
      isMultipart = true;
      return this;
    }

    Helper setUrl(String url) {
      this.url = url;
      return this;
    }

    Request build() throws NoSuchMethodException, URISyntaxException {
      if (method == null) {
        throw new IllegalStateException("Method must be set.");
      }
      if (path == null) {
        throw new IllegalStateException("Path must be set.");
      }

      final Method method;
      if (isSynchronous) {
        method = getClass().getDeclaredMethod("dummySync");
      } else {
        method = getClass().getDeclaredMethod("dummyAsync", Callback.class);
        args.add(mock(Callback.class));
      }

      // Create a fake rest method annotation based on set values.
      RestMethod restMethod = mock(RestMethod.class);
      when(restMethod.hasBody()).thenReturn(hasBody);
      when(restMethod.value()).thenReturn(this.method);

      RestMethodInfo methodInfo = new RestMethodInfo(method);
      methodInfo.restMethod = restMethod;
      methodInfo.path = path;
      methodInfo.pathParams = pathParams;
      methodInfo.pathQueryParams = queryParams.toArray(new QueryParam[queryParams.size()]);
      methodInfo.headers = methodHeaders;
      methodInfo.headerParams = headerParams.toArray(new String[headerParams.size()]);
      methodInfo.namedParams = namedParams.toArray(new String[namedParams.size()]);
      methodInfo.singleEntityArgumentIndex = singleEntityArgumentIndex;
      methodInfo.isMultipart = isMultipart;
      methodInfo.loaded = true;

      return new RequestBuilder(GSON) //
          .setApiUrl(url)
          .setHeaders(headers)
          .setArgs(args.toArray(new Object[args.size()]))
          .setMethodInfo(methodInfo)
          .build();
    }

    @SuppressWarnings("UnusedDeclaration") // Accessed via reflection.
    private Object dummySync() {
      return null;
    }

    @SuppressWarnings("UnusedDeclaration") // Accessed via reflection.
    private void dummyAsync(Callback<Object> cb) {
    }
  }
}
