// Copyright 2013 Square, Inc.
package retrofit;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import okio.Buffer;
import org.junit.Ignore;
import org.junit.Test;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FieldMap;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.HEAD;
import retrofit.http.HTTP;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.Multipart;
import retrofit.http.PATCH;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.PartMap;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.QueryMap;
import retrofit.http.Url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@SuppressWarnings({"UnusedParameters", "unused"}) // Parameters inspected reflectively.
public final class RequestBuilderTest {
  private static final MediaType TEXT_PLAIN = MediaType.parse("text/plain");

  @Test public void customMethodNoBody() {
    class Example {
      @HTTP(method = "CUSTOM1", path = "/foo")
      Call<Object> method() {
        return null;
      }
    }

    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("CUSTOM1");
    assertThat(request.urlString()).isEqualTo("http://example.com/foo");
    assertThat(request.body()).isNull();
  }

  @Ignore("https://github.com/square/okhttp/issues/229")
  @Test public void customMethodWithBody() {
    class Example {
      @HTTP(method = "CUSTOM2", path = "/foo", hasBody = true)
      Call<Object> method(@Body RequestBody body) {
        return null;
      }
    }

    RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "hi");
    Request request = buildRequest(Example.class, body);
    assertThat(request.method()).isEqualTo("CUSTOM2");
    assertThat(request.urlString()).isEqualTo("http://example.com/foo");
    assertBody(request.body(), "hi");
  }

  @Test public void onlyOneEncodingIsAllowedMultipartFirst() {
    class Example {
      @Multipart //
      @FormUrlEncoded //
      @POST("/") //
      Call<Object> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Only one encoding annotation is allowed.\n    for method Example.method");
    }
  }

  @Test public void onlyOneEncodingIsAllowedFormEncodingFirst() {
    class Example {
      @FormUrlEncoded //
      @Multipart //
      @POST("/") //
      Call<Object> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Only one encoding annotation is allowed.\n    for method Example.method");
    }
  }

  @Test public void invalidPathParam() throws Exception {
    class Example {
      @GET("/") //
      Call<Object> method(@Path("hey!") String thing) {
        return null;
      }
    }

    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "@Path parameter name must match \\{([a-zA-Z][a-zA-Z0-9_-]*)\\}."
              + " Found: hey! (parameter #1)\n    for method Example.method");
    }
  }

  @Test public void pathParamNotAllowedInQuery() throws Exception {
    class Example {
      @GET("/foo?bar={bar}") //
      Call<Object> method(@Path("bar") String thing) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "URL query string \"bar={bar}\" must not have replace block."
              + " For dynamic query parameters use @Query.\n    for method Example.method");
    }
  }

  @Test public void multipleParameterAnnotationsNotAllowed() throws Exception {
    class Example {
      @GET("/") //
      Call<Object> method(@Body @Query("nope") Object o) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Multiple Retrofit annotations found, only one allowed. (parameter #1)\n    for method Example.method");
    }
  }

  @Test public void twoMethodsFail() {
    class Example {
      @PATCH("/foo") //
      @POST("/foo") //
      Call<Object> method() {
        return null;
      }
    }

    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Only one HTTP method is allowed. Found: PATCH and POST.\n    for method Example.method");
    }
  }

  @Test public void lackingMethod() {
    class Example {
      Call<Object> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "HTTP method annotation is required (e.g., @GET, @POST, etc.).\n    for method Example.method");
    }
  }

  @Test public void implicitMultipartForbidden() {
    class Example {
      @POST("/") //
      Call<Object> method(@Part("a") int a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "@Part parameters can only be used with multipart encoding. (parameter #1)\n    for method Example.method");
    }
  }

  @Test public void implicitMultipartWithPartMapForbidden() {
    class Example {
      @POST("/") //
      Call<Object> method(@PartMap Map<String, String> params) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "@PartMap parameters can only be used with multipart encoding. (parameter #1)\n    for method Example.method");
    }
  }

  @Test public void multipartFailsOnNonBodyMethod() {
    class Example {
      @Multipart //
      @GET("/") //
      Call<Object> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Multipart can only be specified on HTTP methods with request body (e.g., @POST).\n    for method Example.method");
    }
  }

  @Test public void multipartFailsWithNoParts() {
    class Example {
      @Multipart //
      @POST("/") //
      Call<Object> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Multipart method must contain at least one @Part.\n    for method Example.method");
    }
  }

  @Test public void implicitFormEncodingByFieldForbidden() {
    class Example {
      @POST("/") //
      Call<Object> method(@Field("a") int a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "@Field parameters can only be used with form encoding. (parameter #1)\n    for method Example.method");
    }
  }

  @Test public void implicitFormEncodingByFieldMapForbidden() {
    class Example {
      @POST("/") //
      Call<Object> method(@FieldMap Map<String, String> a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "@FieldMap parameters can only be used with form encoding. (parameter #1)\n    for method Example.method");
    }
  }

  @Test public void formEncodingFailsOnNonBodyMethod() {
    class Example {
      @FormUrlEncoded //
      @GET("/") //
      Call<Object> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "FormUrlEncoded can only be specified on HTTP methods with request body (e.g., @POST).\n    for method Example.method");
    }
  }

  @Test public void formEncodingFailsWithNoParts() {
    class Example {
      @FormUrlEncoded //
      @POST("/") //
      Call<Object> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Form-encoded method must contain at least one @Field.\n    for method Example.method");
    }
  }

  @Test public void headersFailWhenEmptyOnMethod() {
    class Example {
      @GET("/") //
      @Headers({}) //
      Call<Object> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("@Headers annotation is empty.\n    for method Example.method");
    }
  }

  @Test public void headersFailWhenMalformed() {
    class Example {
      @GET("/") //
      @Headers("Malformed") //
      Call<Object> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "@Headers value must be in the form \"Name: Value\". Found: \"Malformed\"\n    for method Example.method");
    }
  }

  @Test public void pathParamNonPathParamAndTypedBytes() {
    class Example {
      @PUT("/{a}") //
      Call<Object> method(@Path("a") int a, @Path("b") int b, @Body int c) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "URL \"/{a}\" does not contain \"{b}\". (parameter #2)\n    for method Example.method");
    }
  }

  @Test public void parameterWithoutAnnotation() {
    class Example {
      @GET("/") //
      Call<Object> method(String a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "No Retrofit annotation found. (parameter #1)\n    for method Example.method");
    }
  }

  @Test public void nonBodyHttpMethodWithSingleEntity() {
    class Example {
      @GET("/") //
      Call<Object> method(@Body Object o) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Non-body HTTP method cannot contain @Body.\n    for method Example.method");
    }
  }

  @Test public void queryMapMustBeAMap() {
    class Example {
      @GET("/") //
      Call<Object> method(@QueryMap List<String> a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "@QueryMap parameter type must be Map. (parameter #1)\n    for method Example.method");
    }
  }

  @Test public void queryMapRejectsNullKeys() {
    class Example {
      @GET("/") //
      Call<Object> method(@QueryMap Map<String, String> a) {
        return null;
      }
    }

    Map<String, String> queryParams = new LinkedHashMap<>();
    queryParams.put("ping", "pong");
    queryParams.put(null, "kat");

    try {
      buildRequest(Example.class, queryParams);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Query map contained null key.");
    }
  }

  @Test public void twoBodies() {
    class Example {
      @PUT("/") //
      Call<Object> method(@Body int o1, @Body int o2) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Multiple @Body method annotations found. (parameter #2)\n    for method Example.method");
    }
  }

  @Test public void bodyInNonBodyRequest() {
    class Example {
      @Multipart //
      @PUT("/") //
      Call<Object> method(@Part("one") int o1, @Body int o2) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "@Body parameters cannot be used with form or multi-part encoding. (parameter #2)\n    for method Example.method");
    }
  }

  @Test public void get() {
    class Example {
      @GET("/foo/bar/") //
      Call<Object> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test public void delete() {
    class Example {
      @DELETE("/foo/bar/") //
      Call<Object> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("DELETE");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertNull(request.body());
  }

  @Test public void head() {
    class Example {
      @HEAD("/foo/bar/") //
      Call<Object> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("HEAD");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test public void post() {
    class Example {
      @POST("/foo/bar/") //
      Call<Object> method(@Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "hi");
    Request request = buildRequest(Example.class, body);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertBody(request.body(), "hi");
  }

  @Test public void put() {
    class Example {
      @PUT("/foo/bar/") //
      Call<Object> method(@Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "hi");
    Request request = buildRequest(Example.class, body);
    assertThat(request.method()).isEqualTo("PUT");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertBody(request.body(), "hi");
  }

  @Test public void patch() {
    class Example {
      @PATCH("/foo/bar/") //
      Call<Object> method(@Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "hi");
    Request request = buildRequest(Example.class, body);
    assertThat(request.method()).isEqualTo("PATCH");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertBody(request.body(), "hi");
  }

  @Test public void getWithPathParam() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<Object> method(@Path("ping") String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "po ng");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/po%20ng/");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithUnusedAndInvalidNamedPathParam() {
    class Example {
      @GET("/foo/bar/{ping}/{kit,kat}/") //
      Call<Object> method(@Path("ping") String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "pong");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/pong/%7Bkit,kat%7D/");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithEncodedPathParam() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<Object> method(@Path(value = "ping", encoded = true) String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "po%20ng");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/po%20ng/");
    assertThat(request.body()).isNull();
  }

  @Test public void pathParamRequired() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<Object> method(@Path("ping") String ping) {
        return null;
      }
    }
    try {
      buildRequest(Example.class, new Object[] { null });
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Path parameter \"ping\" value must not be null.");
    }
  }

  @Test public void getWithQueryParam() {
    class Example {
      @GET("/foo/bar/") //
      Call<Object> method(@Query("ping") String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "pong");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?ping=pong");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithEncodedQueryParam() {
    class Example {
      @GET("/foo/bar/") //
      Call<Object> method(@Query(value = "pi%20ng", encoded = true) String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "p%20o%20n%20g");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?pi%20ng=p%20o%20n%20g");
    assertThat(request.body()).isNull();
  }

  @Test public void queryParamOptionalOmitsQuery() {
    class Example {
      @GET("/foo/bar/") //
      Call<Object> method(@Query("ping") String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, new Object[] { null });
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
  }

  @Test public void queryParamOptional() {
    class Example {
      @GET("/foo/bar/") //
      Call<Object> method(@Query("foo") String foo, @Query("ping") String ping,
          @Query("kit") String kit) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "bar", null, "kat");
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?foo=bar&kit=kat");
  }

  @Test public void getWithQueryUrlAndParam() {
    class Example {
      @GET("/foo/bar/?hi=mom") //
      Call<Object> method(@Query("ping") String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "pong");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?hi=mom&ping=pong");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithQuery() {
    class Example {
      @GET("/foo/bar/?hi=mom") //
      Call<Object> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?hi=mom");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithPathAndQueryParam() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<Object> method(@Path("ping") String ping, @Query("kit") String kit,
          @Query("riff") String riff) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "pong", "kat", "raff");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/pong/?kit=kat&riff=raff");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithQueryThenPathThrows() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<Object> method(@Query("kit") String kit, @Path("ping") String ping) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, "kat", "pong");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("A @Path parameter must not come after a @Query. (parameter #2)\n"
          + "    for method Example.method");
    }
  }

  @Test public void getWithPathAndQueryQuestionMarkParam() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<Object> method(@Path("ping") String ping, @Query("kit") String kit) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "pong?", "kat?");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/pong%3F/?kit=kat?");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithPathAndQueryAmpersandParam() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<Object> method(@Path("ping") String ping, @Query("kit") String kit) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "pong&", "kat&");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/pong%26/?kit=kat%26");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithPathAndQueryHashParam() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<Object> method(@Path("ping") String ping, @Query("kit") String kit) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "pong#", "kat#");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/pong%23/?kit=kat%23");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithQueryParamList() {
    class Example {
      @GET("/foo/bar/") //
      Call<Object> method(@Query("key") List<Object> keys) {
        return null;
      }
    }

    List<Object> values = Arrays.<Object>asList(1, 2, null, "three");
    Request request = buildRequest(Example.class, values);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?key=1&key=2&key=three");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithQueryParamArray() {
    class Example {
      @GET("/foo/bar/") //
      Call<Object> method(@Query("key") Object[] keys) {
        return null;
      }
    }

    Object[] values = { 1, 2, null, "three" };
    Request request = buildRequest(Example.class, new Object[] { values });
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?key=1&key=2&key=three");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithQueryParamPrimitiveArray() {
    class Example {
      @GET("/foo/bar/") //
      Call<Object> method(@Query("key") int[] keys) {
        return null;
      }
    }

    int[] values = { 1, 2, 3 };
    Request request = buildRequest(Example.class, new Object[] { values });
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?key=1&key=2&key=3");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithQueryParamMap() {
    class Example {
      @GET("/foo/bar/") //
      Call<Object> method(@QueryMap Map<String, Object> query) {
        return null;
      }
    }

    Map<String, Object> params = new LinkedHashMap<>();
    params.put("kit", "kat");
    params.put("foo", null);
    params.put("ping", "pong");

    Request request = buildRequest(Example.class, params);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?kit=kat&ping=pong");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithEncodedQueryParamMap() {
    class Example {
      @GET("/foo/bar/") //
      Call<Object> method(@QueryMap(encoded = true) Map<String, Object> query) {
        return null;
      }
    }

    Map<String, Object> params = new LinkedHashMap<>();
    params.put("kit", "k%20t");
    params.put("foo", null);
    params.put("pi%20ng", "p%20g");

    Request request = buildRequest(Example.class, params);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?kit=k%20t&pi%20ng=p%20g");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithUrl() {
    class Example {
      @GET
      Call<Object> method(@Url String url) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "foo/bar/");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test public void getAbsoluteUrl() {
    class Example {
      @GET("http://example2.com/foo/bar/")
      Call<Object> method() {
        return null;
      }
    }

    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example2.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithUrlAbsolute() {
    class Example {
      @GET
      Call<Object> method(@Url String url) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "https://example2.com/foo/bar/");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("https://example2.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithUrlAbsoluteSameHost() {
    class Example {
      @GET
      Call<Object> method(@Url String url) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "http://example.com/foo/bar/");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithNonStringUrlThrows() {
    class Example {
      @GET
      Call<Object> method(@Url Object url) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, "foo/bar");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("@Url must be String type. (parameter #1)\n"
          + "    for method Example.method");
    }
  }

  @Test public void getUrlAndUrlParamThrows() {
    class Example {
      @GET("foo/bar")
      Call<Object> method(@Url Object url) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, "foo/bar");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("@Url must be String type. (parameter #1)\n"
          + "    for method Example.method");
    }
  }

  @Test public void getWithoutUrlThrows() {
    class Example {
      @GET
      Call<Object> method() {
        return null;
      }
    }

    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Missing either @GET URL or @Url parameter.\n"
          + "    for method Example.method");
    }
  }

  @Test public void getWithUrlThenPathThrows() {
    class Example {
      @GET
      Call<Object> method(@Url String url, @Path("hey") String hey) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, "foo/bar");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("@Path parameters may not be used with @Url. (parameter #2)\n"
          + "    for method Example.method");
    }
  }

  @Test public void getWithPathThenUrlThrows() {
    class Example {
      @GET
      Call<Object> method(@Path("hey") String hey, @Url Object url) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, "foo/bar");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("@Path can only be used with relative url on @GET (parameter #1)\n"
          + "    for method Example.method");
    }
  }

  @Test public void getWithQueryThenUrlThrows() {
    class Example {
      @GET("foo/bar")
      Call<Object> method(@Query("hey") String hey, @Url Object url) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, "hey", "foo/bar/");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("A @Url parameter must not come after a @Query (parameter #2)\n"
          + "    for method Example.method");
    }
  }

  @Test public void getWithUrlThenQuery() {
    class Example {
      @GET
      Call<Object> method(@Url String url, @Query("hey") String hey) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "foo/bar/", "hey!");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?hey=hey!");
  }

  @Test public void postWithUrl() {
    class Example {
      @POST
      Call<Object> method(@Url String url, @Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "hi");
    Request request = buildRequest(Example.class, "http://example.com/foo/bar", body);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar");
    assertBody(request.body(), "hi");
  }

  @Test public void normalPostWithPathParam() {
    class Example {
      @POST("/foo/bar/{ping}/") //
      Call<Object> method(@Path("ping") String ping, @Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(TEXT_PLAIN, "Hi!");
    Request request = buildRequest(Example.class, "pong", body);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/pong/");
    assertBody(request.body(), "Hi!");
  }

  @Test public void emptyBody() {
    class Example {
      @POST("/foo/bar/") //
      Call<Object> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertBody(request.body(), "");
  }

  @Ignore("https://github.com/square/okhttp/issues/229")
  @Test public void customMethodEmptyBody() {
    class Example {
      @HTTP(method = "CUSTOM", path = "/foo/bar/", hasBody = true) //
      Call<Object> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("CUSTOM");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertBody(request.body(), "");
  }

  @Test public void bodyResponseBody() {
    class Example {
      @POST("/foo/bar/") //
      Call<Object> method(@Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(TEXT_PLAIN, "hi");
    Request request = buildRequest(Example.class, body);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertBody(request.body(), "hi");
  }

  @Test public void bodyRequired() {
    class Example {
      @POST("/foo/bar/") //
      Call<Object> method(@Body RequestBody body) {
        return null;
      }
    }
    try {
      buildRequest(Example.class, new Object[] { null });
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Body parameter value must not be null.");
    }
  }

  @Test public void bodyWithPathParams() {
    class Example {
      @POST("/foo/bar/{ping}/{kit}/") //
      Call<Object> method(@Path("ping") String ping, @Body RequestBody body, @Path("kit") String kit) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(TEXT_PLAIN, "Hi!");
    Request request = buildRequest(Example.class, "pong", body, "kat");
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/pong/kat/");
    assertBody(request.body(), "Hi!");
  }

  @Test public void simpleMultipart() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<Object> method(@Part("ping") String ping, @Part("kit") RequestBody kit) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "pong", RequestBody.create(
        MediaType.parse("text/plain"), "kat"));
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("name=\"ping\"\r\n")
        .contains("\r\npong\r\n--");

    assertThat(bodyString)
        .contains("name=\"kit\"")
        .contains("\r\nkat\r\n--");
  }

  @Test public void multipartWithEncoding() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<Object> method(@Part(value = "ping", encoding = "8-bit") String ping,
          @Part(value = "kit", encoding = "7-bit") RequestBody kit) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "pong", RequestBody.create(
        MediaType.parse("text/plain"), "kat"));
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString).contains("name=\"ping\"\r\n")
        .contains("Content-Transfer-Encoding: 8-bit")
        .contains("\r\npong\r\n--");

    assertThat(bodyString).contains("name=\"kit\"")
        .contains("Content-Transfer-Encoding: 7-bit")
        .contains("\r\nkat\r\n--");
  }

  @Test public void multipartPartMap() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<Object> method(@PartMap Map<String, Object> parts) {
        return null;
      }
    }

    Map<String, Object> params = new LinkedHashMap<>();
    params.put("ping", "pong");
    params.put("foo", null); // Should be skipped.
    params.put("kit", "kat");

    Request request = buildRequest(Example.class, params);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("name=\"ping\"\r\n")
        .contains("\r\npong\r\n--");

    assertThat(bodyString)
        .contains("name=\"kit\"")
        .contains("\r\nkat\r\n--");

    assertThat(bodyString).doesNotContain("name=\"foo\"\r\n");
  }

  @Test public void multipartPartMapWithEncoding() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<Object> method(@PartMap(encoding = "8-bit") Map<String, Object> parts) {
        return null;
      }
    }

    Map<String, Object> params = new LinkedHashMap<>();
    params.put("ping", "pong");
    params.put("foo", null); // Should be skipped.
    params.put("kit", "kat");

    Request request = buildRequest(Example.class, params);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString).contains("name=\"ping\"\r\n")
        .contains("Content-Transfer-Encoding: 8-bit")
        .contains("\r\npong\r\n--");

    assertThat(bodyString).contains("name=\"kit\"")
        .contains("Content-Transfer-Encoding: 8-bit")
        .contains("\r\nkat\r\n--");

    assertThat(bodyString).doesNotContain("name=\"foo\"\r\n");
  }

  @Test public void multipartPartMapRejectsNullKeys() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<Object> method(@PartMap Map<String, Object> parts) {
        return null;
      }
    }

    Map<String, Object> params = new LinkedHashMap<>();
    params.put("ping", "pong");
    params.put(null, "kat");

    try {
      buildRequest(Example.class, params);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Part map contained null key.");
    }
  }

  @Test public void multipartPartMapMustBeMap() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<Object> method(@PartMap List<Object> parts) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, Collections.emptyList());
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "@PartMap parameter type must be Map. (parameter #1)\n    for method Example.method");
    }
  }

  @Test public void multipartNullRemovesPart() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<Object> method(@Part("ping") String ping, @Part("fizz") String fizz) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "pong", null);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("name=\"ping\"")
        .contains("\r\npong\r\n--");
  }

  @Test public void multipartPartArray() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<Object> method(@Part("ping") String[] ping) {
        return null;
      }
    }
    Object args = new String[] { "pong", "pang" };
    Request request = buildRequest(Example.class, args);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("name=\"ping\"")
        .contains("pong")
        .contains("name=\"ping\"")
        .contains("pang");
  }

  @Test public void multipartPartList() throws IOException {

    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<Object> method(@Part("ping") List<String> ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, Arrays.asList("pong", "pang"));
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("name=\"ping\"")
        .contains("pong")
        .contains("name=\"ping\"")
        .contains("pang");
  }

  @Test public void multipartPartOptional() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<Object> method(@Part("ping") RequestBody ping) {
        return null;
      }
    }
    try {
      buildRequest(Example.class, new Object[] { null });
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Multipart body must have at least one part.");
    }
  }

  @Test public void simpleFormEncoded() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<Object> method(@Field("foo") String foo, @Field("ping") String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "bar", "pong");
    assertBody(request.body(), "foo=bar&ping=pong");
  }

  @Test public void formEncodedWithEncodedNameFieldParam() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<Object> method(@Field(value = "na%20me", encoded = true) String foo) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "ba%20r");
    assertBody(request.body(), "na%20me=ba%20r");
  }

  @Test public void formEncodedFieldOptional() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<Object> method(@Field("foo") String foo, @Field("ping") String ping,
          @Field("kit") String kit) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "bar", null, "kat");
    assertBody(request.body(), "foo=bar&kit=kat");
  }

  @Test public void formEncodedFieldList() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<Object> method(@Field("foo") List<Object> fields, @Field("kit") String kit) {
        return null;
      }
    }

    List<Object> values = Arrays.<Object>asList("foo", "bar", null, 3);
    Request request = buildRequest(Example.class, values, "kat");
    assertBody(request.body(), "foo=foo&foo=bar&foo=3&kit=kat");
  }

  @Test public void formEncodedFieldArray() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<Object> method(@Field("foo") Object[] fields, @Field("kit") String kit) {
        return null;
      }
    }

    Object[] values = { 1, 2, null, "three" };
    Request request = buildRequest(Example.class, values, "kat");
    assertBody(request.body(), "foo=1&foo=2&foo=three&kit=kat");
  }

  @Test public void formEncodedFieldPrimitiveArray() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<Object> method(@Field("foo") int[] fields, @Field("kit") String kit) {
        return null;
      }
    }

    int[] values = { 1, 2, 3 };
    Request request = buildRequest(Example.class, values, "kat");
    assertBody(request.body(), "foo=1&foo=2&foo=3&kit=kat");
  }

  @Test public void formEncodedWithEncodedNameFieldParamMap() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<Object> method(@FieldMap(encoded = true) Map<String, Object> fieldMap) {
        return null;
      }
    }

    Map<String, Object> fieldMap = new LinkedHashMap<>();
    fieldMap.put("k%20it", "k%20at");
    fieldMap.put("pin%20g", "po%20ng");

    Request request = buildRequest(Example.class, fieldMap);
    assertBody(request.body(), "k%20it=k%20at&pin%20g=po%20ng");
  }

  @Test public void formEncodedFieldMap() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<Object> method(@FieldMap Map<String, Object> fieldMap) {
        return null;
      }
    }

    Map<String, Object> fieldMap = new LinkedHashMap<>();
    fieldMap.put("kit", "kat");
    fieldMap.put("foo", null);
    fieldMap.put("ping", "pong");

    Request request = buildRequest(Example.class, fieldMap);
    assertBody(request.body(), "kit=kat&ping=pong");
  }

  @Test public void fieldMapRejectsNullKeys() {
    class Example {
      @FormUrlEncoded //
      @POST("/") //
      Call<Object> method(@FieldMap Map<String, Object> a) {
        return null;
      }
    }

    Map<String, Object> fieldMap = new LinkedHashMap<>();
    fieldMap.put("kit", "kat");
    fieldMap.put("foo", null);
    fieldMap.put(null, "pong");

    try {
      buildRequest(Example.class, fieldMap);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Field map contained null key.");
    }
  }

  @Test public void fieldMapMustBeAMap() {
    class Example {
      @FormUrlEncoded //
      @POST("/") //
      Call<Object> method(@FieldMap List<String> a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "@FieldMap parameter type must be Map. (parameter #1)\n    for method Example.method");
    }
  }

  @Test public void simpleHeaders() {
    class Example {
      @GET("/foo/bar/")
      @Headers({
          "ping: pong",
          "kit: kat"
      })
      Call<Object> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("GET");
    com.squareup.okhttp.Headers headers = request.headers();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.get("ping")).isEqualTo("pong");
    assertThat(headers.get("kit")).isEqualTo("kat");
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test public void headerParamToString() {
    class Example {
      @GET("/foo/bar/") //
      Call<Object> method(@Header("kit") BigInteger kit) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, new BigInteger("1234"));
    assertThat(request.method()).isEqualTo("GET");
    com.squareup.okhttp.Headers headers = request.headers();
    assertThat(headers.size()).isEqualTo(1);
    assertThat(headers.get("kit")).isEqualTo("1234");
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test public void headerParam() {
    class Example {
      @GET("/foo/bar/") //
      @Headers("ping: pong") //
      Call<Object> method(@Header("kit") String kit) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "kat");
    assertThat(request.method()).isEqualTo("GET");
    com.squareup.okhttp.Headers headers = request.headers();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.get("ping")).isEqualTo("pong");
    assertThat(headers.get("kit")).isEqualTo("kat");
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test public void headerParamList() {
    class Example {
      @GET("/foo/bar/") //
      Call<Object> method(@Header("foo") List<String> kit) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, Arrays.asList("bar", null, "baz"));
    assertThat(request.method()).isEqualTo("GET");
    com.squareup.okhttp.Headers headers = request.headers();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.values("foo")).containsExactly("bar", "baz");
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test public void headerParamArray() {
    class Example {
      @GET("/foo/bar/") //
      Call<Object> method(@Header("foo") String[] kit) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, (Object) new String[] { "bar", null, "baz" });
    assertThat(request.method()).isEqualTo("GET");
    com.squareup.okhttp.Headers headers = request.headers();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.values("foo")).containsExactly("bar", "baz");
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test public void contentTypeAnnotationHeaderOverrides() {
    class Example {
      @POST("/") //
      @Headers("Content-Type: text/not-plain") //
      Call<Object> method(@Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "hi");
    Request request = buildRequest(Example.class, body);
    assertThat(request.body().contentType().toString()).isEqualTo("text/not-plain");
  }

  @Test public void contentTypeAnnotationHeaderAddsHeaderWithNoBody() {
    class Example {
      @DELETE("/") //
      @Headers("Content-Type: text/not-plain") //
      Call<Object> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.headers().get("Content-Type")).isEqualTo("text/not-plain");
  }

  @Test public void contentTypeParameterHeaderOverrides() {
    class Example {
      @POST("/") //
      Call<Object> method(@Header("Content-Type") String contentType, @Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "Plain");
    Request request = buildRequest(Example.class, "text/not-plain", body);
    assertThat(request.body().contentType().toString()).isEqualTo("text/not-plain");
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

  private Request buildRequest(Class<?> cls, Object... args) {
    Method method = TestingUtils.onlyMethod(cls);

    BaseUrl baseUrl = new BaseUrl() {
      @Override public HttpUrl url() {
        return HttpUrl.parse("http://example.com/");
      }
    };
    Converter.Factory converterFactory = new ToStringConverterFactory();

    RequestFactory requestFactory = RequestFactoryParser.parse(method, baseUrl, converterFactory);
    return requestFactory.create(args);
  }
}
