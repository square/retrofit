// Copyright 2011 Square, Inc.
package retrofit.http;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.UUID;
import javax.inject.Named;
import junit.framework.TestCase;
import org.apache.http.HttpMessage;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;

/** @author Eric Denman (edenman@squareup.com) */
public class HttpRequestBuilderTest extends TestCase {
  public static final String API_URL = "http://taqueria.com/lengua/taco";
  public static final Headers BLANK_HEADERS = new Headers() {
    @Override public void setOn(HttpMessage message, String mimeType) {
    }
  };

  public void testRegex() throws Exception {
    expectParams("");
    expectParams("foo");
    expectParams("foo/bar");
    expectParams("foo/bar/{taco}", "taco");
    expectParams("foo/bar/{t}", "t");
    expectParams("foo/bar/{taco}/or/{burrito}", "taco", "burrito");
    expectParams("foo/bar/{taco}/or/{taco}", "taco");
    expectParams("foo/bar/{taco-shell}", "taco-shell");
    expectParams("foo/bar/{taco_shell}", "taco_shell");
  }

  private void expectParams(String path, String... expected) {
    Set<String> calculated = HttpRequestBuilder.getPathParameters(path);
    assertEquals(expected.length, calculated.size());
    for (String val : expected) {
      assertTrue(calculated.contains(val));
    }
  }

  public void testNormalGet() throws Exception {
    Method method =
        MyService.class.getMethod("normalGet", String.class, Callback.class);
    String expectedId = UUID.randomUUID().toString();
    Object[] args = new Object[] {expectedId, new MyCallback()};
    HttpUriRequest request = build(method, args);

    assertTrue(request instanceof HttpGet);

    HttpGet put = (HttpGet) request;
    // Make sure the url param got translated.
    final String uri = put.getURI().toString();
    assertEquals(API_URL + "/foo/bar?id=" + expectedId, uri);
  }

  public void testGetWithPathParam() throws Exception {
    Method method =
        MyService.class.getMethod("getWithPathParam", String.class, String.class, Callback.class);
    String expectedId = UUID.randomUUID().toString();
    String category = UUID.randomUUID().toString();
    Object[] args = new Object[] {expectedId, category, new MyCallback()};
    HttpUriRequest request = build(method, args);

    assertTrue(request instanceof HttpGet);

    HttpGet put = (HttpGet) request;
    // Make sure the url param got translated.
    final String uri = put.getURI().toString();
    assertEquals(API_URL + "/foo/" + expectedId + "/bar?category=" + category, uri);
  }

  public void testSingleEntityWithPathParams() throws Exception {
    Method method =
        MyService.class.getMethod("singleEntityPut", MyJsonObj.class, String.class, Callback.class);
    String expectedId = UUID.randomUUID().toString();
    String bodyText = UUID.randomUUID().toString();
    Object[] args = new Object[] {new MyJsonObj(bodyText), expectedId, new MyCallback()};
    HttpUriRequest request = build(method, args);

    assertTrue(request instanceof HttpPut);

    HttpPut put = (HttpPut) request;
    // Make sure the url param got translated.
    final String uri = put.getURI().toString();
    assertEquals(API_URL + "/foo/bar/" + expectedId, uri);

    // Make sure the request body has the json string.
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    put.getEntity().writeTo(out);
    final String requestBody = out.toString();
    assertEquals("{\"bodyText\":\"" + bodyText + "\"}", requestBody);
  }

  public void testNormalPutWithPathParams() throws Exception {
    Method method =
        MyService.class.getMethod("normalPut", String.class, String.class, Callback.class);
    String expectedId = UUID.randomUUID().toString();
    String bodyText = UUID.randomUUID().toString();
    Object[] args = new Object[] {expectedId, bodyText, new MyCallback()};
    HttpUriRequest request = build(method, args);

    assertTrue(request instanceof HttpPut);

    HttpPut put = (HttpPut) request;
    // Make sure the url param got translated.
    final String uri = put.getURI().toString();
    assertEquals(API_URL + "/foo/bar/" + expectedId, uri);

    // Make sure the request body has the json string.
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    put.getEntity().writeTo(out);
    final String requestBody = out.toString();
    assertEquals("id=" + expectedId + "&body=" + bodyText, requestBody);
  }

  public void testSingleEntityWithTooManyParams() throws Exception {
    Method method =
        MyService.class.getMethod("tooManyParams", MyJsonObj.class, String.class, String.class,
            Callback.class);
    String expectedId = UUID.randomUUID().toString();
    String bodyText = UUID.randomUUID().toString();
    Object[] args = new Object[] {new MyJsonObj(bodyText), expectedId, "EXTRA", new MyCallback()};
    try {
      build(method, args);
      fail("Didn't throw exception with too many params");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public void testSingleEntityWithNoPathParam() throws Exception {
    Method method =
        MyService.class.getMethod("singleEntityNoPathParam", MyJsonObj.class, Callback.class);
    String bodyText = UUID.randomUUID().toString();
    Object[] args = new Object[] {new MyJsonObj(bodyText), new MyCallback()};
    try {
      build(method, args);
      fail("Didn't throw exception with too few params");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public void testRegularWithNoPathParam() throws Exception {
    Method method = MyService.class.getMethod("regularNoPathParam", String.class, Callback.class);
    String otherParam = UUID.randomUUID().toString();
    Object[] args = new Object[] {otherParam, new MyCallback()};
    try {
      build(method, args);
      fail("Didn't throw exception with too few params");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  @SuppressWarnings({"UnusedDeclaration"}) // Methods are accessed by reflection.
  private static interface MyService {
    @GET("foo/bar") void normalGet(@Named("id") String id, Callback<SimpleResponse> callback);

    @GET("foo/{id}/bar")
    void getWithPathParam(@Named("id") String id, @Named("category") String category,
        Callback<SimpleResponse> callback);

    @PUT("foo/bar/{id}") void singleEntityPut(@SingleEntity MyJsonObj card, @Named("id") String id,
        Callback<SimpleResponse> callback);

    @PUT("foo/bar/{id}") void normalPut(@Named("id") String id, @Named("body") String body,
        Callback<SimpleResponse> callback);

    @PUT("foo/bar/{id}") void tooManyParams(@SingleEntity MyJsonObj card, @Named("id") String id,
        @Named("extra") String extraParam, Callback<SimpleResponse> callback);

    @PUT("foo/bar/{id}")
    void singleEntityNoPathParam(@SingleEntity MyJsonObj card, Callback<SimpleResponse> callback);

    @PUT("foo/bar/{id}")
    void regularNoPathParam(@Named("other") String other, Callback<SimpleResponse> callback);
  }

  private HttpUriRequest build(Method method, Object[] args) throws URISyntaxException {
    return new HttpRequestBuilder(new Gson()).setMethod(method)
        .setArgs(args)
        .setApiUrl(API_URL)
        .setHeaders(BLANK_HEADERS)
        .build();
  }

  private static class MyJsonObj {
    @SuppressWarnings({"UnusedDeclaration"}) // Accessed by json serialization.
    private String bodyText;

    public MyJsonObj(String bodyText) {
      this.bodyText = bodyText;
    }
  }

  private static class SimpleResponse {

  }

  private class MyCallback implements Callback<SimpleResponse> {
    @Override public void call(SimpleResponse simpleResponse) {
    }

    @Override public void sessionExpired() {
    }

    @Override public void networkError() {
    }

    @Override public void clientError(SimpleResponse response, int statusCode) {
    }

    @Override public void serverError(String message, int statusCode) {
    }

    @Override public void unexpectedError(Throwable t) {
    }
  }
}
