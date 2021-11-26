/*
 * Copyright (C) 2013 Square, Inc.
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static retrofit2.TestingUtils.buildRequest;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.Ignore;
import org.junit.Test;
import retrofit2.helpers.NullObjectConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.HeaderMap;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.OPTIONS;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import retrofit2.http.QueryName;
import retrofit2.http.Tag;
import retrofit2.http.Url;

@SuppressWarnings({"UnusedParameters", "unused"}) // Parameters inspected reflectively.
public final class RequestFactoryTest {
  private static final MediaType TEXT_PLAIN = MediaType.get("text/plain");

  @Test
  public void customMethodNoBody() {
    class Example {
      @HTTP(method = "CUSTOM1", path = "/foo")
      Call<ResponseBody> method() {
        return null;
      }
    }

    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("CUSTOM1");
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo");
    assertThat(request.body()).isNull();
  }

  @Test
  public void customMethodWithBody() {
    class Example {
      @HTTP(method = "CUSTOM2", path = "/foo", hasBody = true)
      Call<ResponseBody> method(@Body RequestBody body) {
        return null;
      }
    }

    RequestBody body = RequestBody.create(TEXT_PLAIN, "hi");
    Request request = buildRequest(Example.class, body);
    assertThat(request.method()).isEqualTo("CUSTOM2");
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo");
    assertBody(request.body(), "hi");
  }

  @Test
  public void onlyOneEncodingIsAllowedMultipartFirst() {
    class Example {
      @Multipart //
      @FormUrlEncoded //
      @POST("/") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage("Only one encoding annotation is allowed.\n    for method Example.method");
    }
  }

  @Test
  public void onlyOneEncodingIsAllowedFormEncodingFirst() {
    class Example {
      @FormUrlEncoded //
      @Multipart //
      @POST("/") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage("Only one encoding annotation is allowed.\n    for method Example.method");
    }
  }

  @Test
  public void invalidPathParam() throws Exception {
    class Example {
      @GET("/") //
      Call<ResponseBody> method(@Path("hey!") String thing) {
        return null;
      }
    }

    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Path parameter name must match \\{([a-zA-Z][a-zA-Z0-9_-]*)\\}."
                  + " Found: hey! (parameter #1)\n    for method Example.method");
    }
  }

  @Test
  public void pathParamNotAllowedInQuery() throws Exception {
    class Example {
      @GET("/foo?bar={bar}") //
      Call<ResponseBody> method(@Path("bar") String thing) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "URL query string \"bar={bar}\" must not have replace block."
                  + " For dynamic query parameters use @Query.\n    for method Example.method");
    }
  }

  @Test
  public void multipleParameterAnnotationsNotAllowed() throws Exception {
    class Example {
      @GET("/") //
      Call<ResponseBody> method(@Body @Query("nope") String o) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Multiple Retrofit annotations found, only one allowed. (parameter #1)\n    for method Example.method");
    }
  }

  @interface NonNull {}

  @Test
  public void multipleParameterAnnotationsOnlyOneRetrofitAllowed() throws Exception {
    class Example {
      @GET("/") //
      Call<ResponseBody> method(@Query("maybe") @NonNull Object o) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "yep");
    assertThat(request.url().toString()).isEqualTo("http://example.com/?maybe=yep");
  }

  @Test
  public void twoMethodsFail() {
    class Example {
      @PATCH("/foo") //
      @POST("/foo") //
      Call<ResponseBody> method() {
        return null;
      }
    }

    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage())
          .isIn(
              "Only one HTTP method is allowed. Found: PATCH and POST.\n    for method Example.method",
              "Only one HTTP method is allowed. Found: POST and PATCH.\n    for method Example.method");
    }
  }

  @Test
  public void lackingMethod() {
    class Example {
      Call<ResponseBody> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "HTTP method annotation is required (e.g., @GET, @POST, etc.).\n    for method Example.method");
    }
  }

  @Test
  public void implicitMultipartForbidden() {
    class Example {
      @POST("/") //
      Call<ResponseBody> method(@Part("a") int a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Part parameters can only be used with multipart encoding. (parameter #1)\n    for method Example.method");
    }
  }

  @Test
  public void implicitMultipartWithPartMapForbidden() {
    class Example {
      @POST("/") //
      Call<ResponseBody> method(@PartMap Map<String, String> params) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@PartMap parameters can only be used with multipart encoding. (parameter #1)\n    for method Example.method");
    }
  }

  @Test
  public void multipartFailsOnNonBodyMethod() {
    class Example {
      @Multipart //
      @GET("/") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Multipart can only be specified on HTTP methods with request body (e.g., @POST).\n    for method Example.method");
    }
  }

  @Test
  public void multipartFailsWithNoParts() {
    class Example {
      @Multipart //
      @POST("/") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Multipart method must contain at least one @Part.\n    for method Example.method");
    }
  }

  @Test
  public void implicitFormEncodingByFieldForbidden() {
    class Example {
      @POST("/") //
      Call<ResponseBody> method(@Field("a") int a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Field parameters can only be used with form encoding. (parameter #1)\n    for method Example.method");
    }
  }

  @Test
  public void implicitFormEncodingByFieldMapForbidden() {
    class Example {
      @POST("/") //
      Call<ResponseBody> method(@FieldMap Map<String, String> a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@FieldMap parameters can only be used with form encoding. (parameter #1)\n    for method Example.method");
    }
  }

  @Test
  public void formEncodingFailsOnNonBodyMethod() {
    class Example {
      @FormUrlEncoded //
      @GET("/") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "FormUrlEncoded can only be specified on HTTP methods with request body (e.g., @POST).\n    for method Example.method");
    }
  }

  @Test
  public void formEncodingFailsWithNoParts() {
    class Example {
      @FormUrlEncoded //
      @POST("/") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Form-encoded method must contain at least one @Field.\n    for method Example.method");
    }
  }

  @Test
  public void headersFailWhenEmptyOnMethod() {
    class Example {
      @GET("/") //
      @Headers({}) //
      Call<ResponseBody> method() {
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

  @Test
  public void headersFailWhenMalformed() {
    class Example {
      @GET("/") //
      @Headers("Malformed") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Headers value must be in the form \"Name: Value\". Found: \"Malformed\"\n    for method Example.method");
    }
  }

  @Test
  public void pathParamNonPathParamAndTypedBytes() {
    class Example {
      @PUT("/{a}") //
      Call<ResponseBody> method(@Path("a") int a, @Path("b") int b, @Body int c) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "URL \"/{a}\" does not contain \"{b}\". (parameter #2)\n    for method Example.method");
    }
  }

  @Test
  public void parameterWithoutAnnotation() {
    class Example {
      @GET("/") //
      Call<ResponseBody> method(String a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "No Retrofit annotation found. (parameter #1)\n    for method Example.method");
    }
  }

  @Test
  public void nonBodyHttpMethodWithSingleEntity() {
    class Example {
      @GET("/") //
      Call<ResponseBody> method(@Body String o) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage("Non-body HTTP method cannot contain @Body.\n    for method Example.method");
    }
  }

  @Test
  public void queryMapMustBeAMap() {
    class Example {
      @GET("/") //
      Call<ResponseBody> method(@QueryMap List<String> a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@QueryMap parameter type must be Map. (parameter #1)\n    for method Example.method");
    }
  }

  @Test
  public void queryMapSupportsSubclasses() {
    class Foo extends HashMap<String, String> {}

    class Example {
      @GET("/") //
      Call<ResponseBody> method(@QueryMap Foo a) {
        return null;
      }
    }

    Foo foo = new Foo();
    foo.put("hello", "world");

    Request request = buildRequest(Example.class, foo);
    assertThat(request.url().toString()).isEqualTo("http://example.com/?hello=world");
  }

  @Test
  public void queryMapRejectsNull() {
    class Example {
      @GET("/") //
      Call<ResponseBody> method(@QueryMap Map<String, String> a) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, new Object[] {null});
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage("Query map was null (parameter #1)\n" + "    for method Example.method");
    }
  }

  @Test
  public void queryMapRejectsNullKeys() {
    class Example {
      @GET("/") //
      Call<ResponseBody> method(@QueryMap Map<String, String> a) {
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
      assertThat(e)
          .hasMessage(
              "Query map contained null key. (parameter #1)\n" + "    for method Example.method");
    }
  }

  @Test
  public void queryMapRejectsNullValues() {
    class Example {
      @GET("/") //
      Call<ResponseBody> method(@QueryMap Map<String, String> a) {
        return null;
      }
    }

    Map<String, String> queryParams = new LinkedHashMap<>();
    queryParams.put("ping", "pong");
    queryParams.put("kit", null);

    try {
      buildRequest(Example.class, queryParams);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Query map contained null value for key 'kit'. (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void getWithHeaderMap() {
    class Example {
      @GET("/search")
      Call<ResponseBody> method(@HeaderMap Map<String, Object> headers) {
        return null;
      }
    }

    Map<String, Object> headers = new LinkedHashMap<>();
    headers.put("Accept", "text/plain");
    headers.put("Accept-Charset", "utf-8");

    Request request = buildRequest(Example.class, headers);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.url().toString()).isEqualTo("http://example.com/search");
    assertThat(request.body()).isNull();
    assertThat(request.headers().size()).isEqualTo(2);
    assertThat(request.header("Accept")).isEqualTo("text/plain");
    assertThat(request.header("Accept-Charset")).isEqualTo("utf-8");
  }

  @Test
  public void headerMapMustBeAMapOrHeaders() {
    class Example {
      @GET("/")
      Call<ResponseBody> method(@HeaderMap okhttp3.Headers headers, @HeaderMap List<String> headerMap) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@HeaderMap parameter type must be Map or Headers. (parameter #2)\n    for method Example.method");
    }
  }

  @Test
  public void headerMapSupportsSubclasses() {
    class Foo extends HashMap<String, String> {}

    class Example {
      @GET("/search")
      Call<ResponseBody> method(@HeaderMap Foo headers) {
        return null;
      }
    }

    Foo headers = new Foo();
    headers.put("Accept", "text/plain");

    Request request = buildRequest(Example.class, headers);
    assertThat(request.url().toString()).isEqualTo("http://example.com/search");
    assertThat(request.headers().size()).isEqualTo(1);
    assertThat(request.header("Accept")).isEqualTo("text/plain");
  }

  @Test
  public void headerMapRejectsNull() {
    class Example {
      @GET("/")
      Call<ResponseBody> method(@HeaderMap Map<String, String> headers) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, (Map<String, String>) null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage("Header map was null. (parameter #1)\n" + "    for method Example.method");
    }
  }

  @Test
  public void headerMapRejectsNullKeys() {
    class Example {
      @GET("/")
      Call<ResponseBody> method(@HeaderMap Map<String, String> headers) {
        return null;
      }
    }

    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("Accept", "text/plain");
    headers.put(null, "utf-8");

    try {
      buildRequest(Example.class, headers);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Header map contained null key. (parameter #1)\n" + "    for method Example.method");
    }
  }

  @Test
  public void headerMapRejectsNullValues() {
    class Example {
      @GET("/")
      Call<ResponseBody> method(@HeaderMap Map<String, String> headers) {
        return null;
      }
    }

    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("Accept", "text/plain");
    headers.put("Accept-Charset", null);

    try {
      buildRequest(Example.class, headers);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Header map contained null value for key 'Accept-Charset'. (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void getWithHeaders() {
    class Example {
      @GET("/search")
      Call<ResponseBody> method(@HeaderMap okhttp3.Headers headers) {
        throw new AssertionError();
      }
    }

    okhttp3.Headers headers =
        new okhttp3.Headers.Builder()
            .add("Accept", "text/plain")
            .add("Accept", "application/json")
            .add("Accept-Charset", "utf-8")
            .build();

    Request request = buildRequest(Example.class, headers);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.url().toString()).isEqualTo("http://example.com/search");
    assertThat(request.body()).isNull();
    assertThat(request.headers().size()).isEqualTo(3);
    assertThat(request.headers("Accept")).isEqualTo(asList("text/plain", "application/json"));
    assertThat(request.header("Accept-Charset")).isEqualTo("utf-8");
  }

  @Test
  public void getWithHeadersAndHeaderMap() {
    class Example {
      @GET("/search")
      Call<ResponseBody> method(
          @HeaderMap okhttp3.Headers headers, @HeaderMap Map<String, Object> headerMap) {
        throw new AssertionError();
      }
    }

    okhttp3.Headers headers =
        new okhttp3.Headers.Builder()
            .add("Accept", "text/plain")
            .add("Accept-Charset", "utf-8")
            .build();
    Map<String, String> headerMap = Collections.singletonMap("Accept", "application/json");

    Request request = buildRequest(Example.class, headers, headerMap);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.url().toString()).isEqualTo("http://example.com/search");
    assertThat(request.body()).isNull();
    assertThat(request.headers().size()).isEqualTo(3);
    assertThat(request.headers("Accept")).isEqualTo(asList("text/plain", "application/json"));
    assertThat(request.header("Accept-Charset")).isEqualTo("utf-8");
  }

  @Test
  public void headersRejectsNull() {
    class Example {
      @GET("/")
      Call<ResponseBody> method(@HeaderMap okhttp3.Headers headers) {
        throw new AssertionError();
      }
    }

    try {
      buildRequest(Example.class, (okhttp3.Headers) null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Headers parameter must not be null. (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void getWithHeaderMapAllowingUnsafeNonAsciiValues() {
    class Example {
      @GET("/search")
      Call<ResponseBody> method(
          @HeaderMap(allowUnsafeNonAsciiValues = true) Map<String, Object> headers) {
        return null;
      }
    }

    Map<String, Object> headers = new LinkedHashMap<>();
    headers.put("Accept", "text/plain");
    headers.put("Title", "Kein plötzliches");

    Request request = buildRequest(Example.class, headers);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.url().toString()).isEqualTo("http://example.com/search");
    assertThat(request.body()).isNull();
    assertThat(request.headers().size()).isEqualTo(2);
    assertThat(request.header("Accept")).isEqualTo("text/plain");
    assertThat(request.header("Title")).isEqualTo("Kein plötzliches");
  }

  @Test
  public void twoBodies() {
    class Example {
      @PUT("/") //
      Call<ResponseBody> method(@Body String o1, @Body String o2) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Multiple @Body method annotations found. (parameter #2)\n    for method Example.method");
    }
  }

  @Test
  public void bodyInNonBodyRequest() {
    class Example {
      @Multipart //
      @PUT("/") //
      Call<ResponseBody> method(@Part("one") String o1, @Body String o2) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Body parameters cannot be used with form or multi-part encoding. (parameter #2)\n    for method Example.method");
    }
  }

  @Test
  public void get() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void delete() {
    class Example {
      @DELETE("/foo/bar/") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("DELETE");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertNull(request.body());
  }

  @Test
  public void headVoid() {
    class Example {
      @HEAD("/foo/bar/") //
      Call<Void> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("HEAD");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Ignore("This test is valid but isn't validated by RequestFactory so it needs moved")
  @Test
  public void headWithoutVoidThrows() {
    class Example {
      @HEAD("/foo/bar/") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "HEAD method must use Void or Unit as response type.\n    for method Example.method");
    }
  }

  @Test
  public void post() {
    class Example {
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(TEXT_PLAIN, "hi");
    Request request = buildRequest(Example.class, body);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertBody(request.body(), "hi");
  }

  @Test
  public void put() {
    class Example {
      @PUT("/foo/bar/") //
      Call<ResponseBody> method(@Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(TEXT_PLAIN, "hi");
    Request request = buildRequest(Example.class, body);
    assertThat(request.method()).isEqualTo("PUT");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertBody(request.body(), "hi");
  }

  @Test
  public void patch() {
    class Example {
      @PATCH("/foo/bar/") //
      Call<ResponseBody> method(@Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(TEXT_PLAIN, "hi");
    Request request = buildRequest(Example.class, body);
    assertThat(request.method()).isEqualTo("PATCH");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertBody(request.body(), "hi");
  }

  @Test
  public void options() {
    class Example {
      @OPTIONS("/foo/bar/") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("OPTIONS");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithPathParam() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<ResponseBody> method(@Path("ping") String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "po ng");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/po%20ng/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithUnusedAndInvalidNamedPathParam() {
    class Example {
      @GET("/foo/bar/{ping}/{kit,kat}/") //
      Call<ResponseBody> method(@Path("ping") String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "pong");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString())
        .isEqualTo("http://example.com/foo/bar/pong/%7Bkit,kat%7D/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithEncodedPathParam() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<ResponseBody> method(@Path(value = "ping", encoded = true) String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "po%20ng");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/po%20ng/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithEncodedPathSegments() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<ResponseBody> method(@Path(value = "ping", encoded = true) String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "baz/pong/more");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/baz/pong/more/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithUnencodedPathSegmentsPreventsRequestSplitting() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<ResponseBody> method(@Path(value = "ping", encoded = false) String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "baz/\r\nheader: blue");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString())
        .isEqualTo("http://example.com/foo/bar/baz%2F%0D%0Aheader:%20blue/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithEncodedPathStillPreventsRequestSplitting() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<ResponseBody> method(@Path(value = "ping", encoded = true) String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "baz/\r\npong");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/baz/pong/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void pathParametersAndPathTraversal() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<ResponseBody> method(@Path(value = "ping") String ping) {
        return null;
      }
    }

    assertMalformedRequest(Example.class, ".");
    assertMalformedRequest(Example.class, "..");

    assertThat(buildRequest(Example.class, "./a").url().encodedPath()).isEqualTo("/foo/bar/.%2Fa/");
    assertThat(buildRequest(Example.class, "a/.").url().encodedPath()).isEqualTo("/foo/bar/a%2F./");
    assertThat(buildRequest(Example.class, "a/..").url().encodedPath())
        .isEqualTo("/foo/bar/a%2F../");
    assertThat(buildRequest(Example.class, "../a").url().encodedPath())
        .isEqualTo("/foo/bar/..%2Fa/");
    assertThat(buildRequest(Example.class, "..\\..").url().encodedPath())
        .isEqualTo("/foo/bar/..%5C../");

    assertThat(buildRequest(Example.class, "%2E").url().encodedPath()).isEqualTo("/foo/bar/%252E/");
    assertThat(buildRequest(Example.class, "%2E%2E").url().encodedPath())
        .isEqualTo("/foo/bar/%252E%252E/");
  }

  @Test
  public void encodedPathParametersAndPathTraversal() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<ResponseBody> method(@Path(value = "ping", encoded = true) String ping) {
        return null;
      }
    }

    assertMalformedRequest(Example.class, ".");
    assertMalformedRequest(Example.class, "%2E");
    assertMalformedRequest(Example.class, "%2e");
    assertMalformedRequest(Example.class, "..");
    assertMalformedRequest(Example.class, "%2E.");
    assertMalformedRequest(Example.class, "%2e.");
    assertMalformedRequest(Example.class, ".%2E");
    assertMalformedRequest(Example.class, ".%2e");
    assertMalformedRequest(Example.class, "%2E%2e");
    assertMalformedRequest(Example.class, "%2e%2E");
    assertMalformedRequest(Example.class, "./a");
    assertMalformedRequest(Example.class, "a/.");
    assertMalformedRequest(Example.class, "../a");
    assertMalformedRequest(Example.class, "a/..");
    assertMalformedRequest(Example.class, "a/../b");
    assertMalformedRequest(Example.class, "a/%2e%2E/b");

    assertThat(buildRequest(Example.class, "...").url().encodedPath()).isEqualTo("/foo/bar/.../");
    assertThat(buildRequest(Example.class, "a..b").url().encodedPath()).isEqualTo("/foo/bar/a..b/");
    assertThat(buildRequest(Example.class, "a..").url().encodedPath()).isEqualTo("/foo/bar/a../");
    assertThat(buildRequest(Example.class, "a..b").url().encodedPath()).isEqualTo("/foo/bar/a..b/");
    assertThat(buildRequest(Example.class, "..b").url().encodedPath()).isEqualTo("/foo/bar/..b/");
    assertThat(buildRequest(Example.class, "..\\..").url().encodedPath())
        .isEqualTo("/foo/bar/..%5C../");
  }

  @Test
  public void dotDotsOkayWhenNotFullPathSegment() {
    class Example {
      @GET("/foo{ping}bar/") //
      Call<ResponseBody> method(@Path(value = "ping", encoded = true) String ping) {
        return null;
      }
    }

    assertMalformedRequest(Example.class, "/./");
    assertMalformedRequest(Example.class, "/../");

    assertThat(buildRequest(Example.class, ".").url().encodedPath()).isEqualTo("/foo.bar/");
    assertThat(buildRequest(Example.class, "..").url().encodedPath()).isEqualTo("/foo..bar/");
  }

  @Test
  public void pathParamRequired() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<ResponseBody> method(@Path("ping") String ping) {
        return null;
      }
    }
    try {
      buildRequest(Example.class, new Object[] {null});
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage())
          .isEqualTo(
              "Path parameter \"ping\" value must not be null. (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void getWithQueryParam() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@Query("ping") String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "pong");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/?ping=pong");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithEncodedQueryParam() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@Query(value = "pi%20ng", encoded = true) String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "p%20o%20n%20g");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString())
        .isEqualTo("http://example.com/foo/bar/?pi%20ng=p%20o%20n%20g");
    assertThat(request.body()).isNull();
  }

  @Test
  public void queryParamOptionalOmitsQuery() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@Query("ping") String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, new Object[] {null});
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
  }

  @Test
  public void queryParamOptional() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(
          @Query("foo") String foo, @Query("ping") String ping, @Query("kit") String kit) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "bar", null, "kat");
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/?foo=bar&kit=kat");
  }

  @Test
  public void getWithQueryUrlAndParam() {
    class Example {
      @GET("/foo/bar/?hi=mom") //
      Call<ResponseBody> method(@Query("ping") String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "pong");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/?hi=mom&ping=pong");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithQuery() {
    class Example {
      @GET("/foo/bar/?hi=mom") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/?hi=mom");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithPathAndQueryParam() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<ResponseBody> method(
          @Path("ping") String ping, @Query("kit") String kit, @Query("riff") String riff) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "pong", "kat", "raff");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString())
        .isEqualTo("http://example.com/foo/bar/pong/?kit=kat&riff=raff");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithQueryThenPathThrows() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<ResponseBody> method(@Query("kit") String kit, @Path("ping") String ping) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, "kat", "pong");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "A @Path parameter must not come after a @Query. (parameter #2)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void getWithQueryNameThenPathThrows() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<ResponseBody> method(@QueryName String kit, @Path("ping") String ping) {
        throw new AssertionError();
      }
    }

    try {
      buildRequest(Example.class, "kat", "pong");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "A @Path parameter must not come after a @QueryName. (parameter #2)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void getWithQueryMapThenPathThrows() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<ResponseBody> method(@QueryMap Map<String, String> queries, @Path("ping") String ping) {
        throw new AssertionError();
      }
    }

    try {
      buildRequest(Example.class, Collections.singletonMap("kit", "kat"), "pong");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "A @Path parameter must not come after a @QueryMap. (parameter #2)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void getWithPathAndQueryQuestionMarkParam() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<ResponseBody> method(@Path("ping") String ping, @Query("kit") String kit) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "pong?", "kat?");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString())
        .isEqualTo("http://example.com/foo/bar/pong%3F/?kit=kat%3F");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithPathAndQueryAmpersandParam() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<ResponseBody> method(@Path("ping") String ping, @Query("kit") String kit) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "pong&", "kat&");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/pong&/?kit=kat%26");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithPathAndQueryHashParam() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Call<ResponseBody> method(@Path("ping") String ping, @Query("kit") String kit) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "pong#", "kat#");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString())
        .isEqualTo("http://example.com/foo/bar/pong%23/?kit=kat%23");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithQueryParamList() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@Query("key") List<Object> keys) {
        return null;
      }
    }

    List<Object> values = Arrays.asList(1, 2, null, "three", "1");
    Request request = buildRequest(Example.class, values);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString())
        .isEqualTo("http://example.com/foo/bar/?key=1&key=2&key=three&key=1");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithQueryParamArray() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@Query("key") Object[] keys) {
        return null;
      }
    }

    Object[] values = {1, 2, null, "three", "1"};
    Request request = buildRequest(Example.class, new Object[] {values});
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString())
        .isEqualTo("http://example.com/foo/bar/?key=1&key=2&key=three&key=1");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithQueryParamPrimitiveArray() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@Query("key") int[] keys) {
        return null;
      }
    }

    int[] values = {1, 2, 3, 1};
    Request request = buildRequest(Example.class, new Object[] {values});
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString())
        .isEqualTo("http://example.com/foo/bar/?key=1&key=2&key=3&key=1");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithQueryNameParam() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@QueryName String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "pong");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/?pong");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithEncodedQueryNameParam() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@QueryName(encoded = true) String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "p%20o%20n%20g");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/?p%20o%20n%20g");
    assertThat(request.body()).isNull();
  }

  @Test
  public void queryNameParamOptionalOmitsQuery() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@QueryName String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, new Object[] {null});
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
  }

  @Test
  public void getWithQueryNameParamList() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@QueryName List<Object> keys) {
        return null;
      }
    }

    List<Object> values = Arrays.asList(1, 2, null, "three", "1");
    Request request = buildRequest(Example.class, values);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/?1&2&three&1");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithQueryNameParamArray() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@QueryName Object[] keys) {
        return null;
      }
    }

    Object[] values = {1, 2, null, "three", "1"};
    Request request = buildRequest(Example.class, new Object[] {values});
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/?1&2&three&1");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithQueryNameParamPrimitiveArray() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@QueryName int[] keys) {
        return null;
      }
    }

    int[] values = {1, 2, 3, 1};
    Request request = buildRequest(Example.class, new Object[] {values});
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/?1&2&3&1");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithQueryParamMap() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@QueryMap Map<String, Object> query) {
        return null;
      }
    }

    Map<String, Object> params = new LinkedHashMap<>();
    params.put("kit", "kat");
    params.put("ping", "pong");

    Request request = buildRequest(Example.class, params);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/?kit=kat&ping=pong");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithEncodedQueryParamMap() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@QueryMap(encoded = true) Map<String, Object> query) {
        return null;
      }
    }

    Map<String, Object> params = new LinkedHashMap<>();
    params.put("kit", "k%20t");
    params.put("pi%20ng", "p%20g");

    Request request = buildRequest(Example.class, params);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString())
        .isEqualTo("http://example.com/foo/bar/?kit=k%20t&pi%20ng=p%20g");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getAbsoluteUrl() {
    class Example {
      @GET("http://example2.com/foo/bar/")
      Call<ResponseBody> method() {
        return null;
      }
    }

    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example2.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithStringUrl() {
    class Example {
      @GET
      Call<ResponseBody> method(@Url String url) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "foo/bar/");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithJavaUriUrl() {
    class Example {
      @GET
      Call<ResponseBody> method(@Url URI url) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, URI.create("foo/bar/"));
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithStringUrlAbsolute() {
    class Example {
      @GET
      Call<ResponseBody> method(@Url String url) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "https://example2.com/foo/bar/");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("https://example2.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithJavaUriUrlAbsolute() {
    class Example {
      @GET
      Call<ResponseBody> method(@Url URI url) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, URI.create("https://example2.com/foo/bar/"));
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("https://example2.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithUrlAbsoluteSameHost() {
    class Example {
      @GET
      Call<ResponseBody> method(@Url String url) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "http://example.com/foo/bar/");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithHttpUrl() {
    class Example {
      @GET
      Call<ResponseBody> method(@Url HttpUrl url) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, HttpUrl.get("http://example.com/foo/bar/"));
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url()).isEqualTo(HttpUrl.get("http://example.com/foo/bar/"));
    assertThat(request.body()).isNull();
  }

  @Test
  public void getWithNullUrl() {
    class Example {
      @GET
      Call<ResponseBody> method(@Url HttpUrl url) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, (HttpUrl) null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected)
          .hasMessage("@Url parameter is null. (parameter #1)\n" + "    for method Example.method");
    }
  }

  @Test
  public void getWithNonStringUrlThrows() {
    class Example {
      @GET
      Call<ResponseBody> method(@Url Object url) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, "foo/bar");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Url must be okhttp3.HttpUrl, String, java.net.URI, or android.net.Uri type."
                  + " (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void getUrlAndUrlParamThrows() {
    class Example {
      @GET("foo/bar")
      Call<ResponseBody> method(@Url Object url) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, "foo/bar");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Url cannot be used with @GET URL (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void getWithoutUrlThrows() {
    class Example {
      @GET
      Call<ResponseBody> method() {
        return null;
      }
    }

    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Missing either @GET URL or @Url parameter.\n" + "    for method Example.method");
    }
  }

  @Test
  public void getWithUrlThenPathThrows() {
    class Example {
      @GET
      Call<ResponseBody> method(@Url String url, @Path("hey") String hey) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, "foo/bar");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Path parameters may not be used with @Url. (parameter #2)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void getWithPathThenUrlThrows() {
    class Example {
      @GET
      Call<ResponseBody> method(@Path("hey") String hey, @Url Object url) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, "foo/bar");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Path can only be used with relative url on @GET (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void getWithQueryThenUrlThrows() {
    class Example {
      @GET("foo/bar")
      Call<ResponseBody> method(@Query("hey") String hey, @Url Object url) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, "hey", "foo/bar/");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "A @Url parameter must not come after a @Query. (parameter #2)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void getWithQueryNameThenUrlThrows() {
    class Example {
      @GET
      Call<ResponseBody> method(@QueryName String name, @Url String url) {
        throw new AssertionError();
      }
    }

    try {
      buildRequest(Example.class, Collections.singletonMap("kit", "kat"), "foo/bar/");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "A @Url parameter must not come after a @QueryName. (parameter #2)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void getWithQueryMapThenUrlThrows() {
    class Example {
      @GET
      Call<ResponseBody> method(@QueryMap Map<String, String> queries, @Url String url) {
        throw new AssertionError();
      }
    }

    try {
      buildRequest(Example.class, Collections.singletonMap("kit", "kat"), "foo/bar/");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "A @Url parameter must not come after a @QueryMap. (parameter #2)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void getWithUrlThenQuery() {
    class Example {
      @GET
      Call<ResponseBody> method(@Url String url, @Query("hey") String hey) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "foo/bar/", "hey!");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/?hey=hey%21");
  }

  @Test
  public void postWithUrl() {
    class Example {
      @POST
      Call<ResponseBody> method(@Url String url, @Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(TEXT_PLAIN, "hi");
    Request request = buildRequest(Example.class, "http://example.com/foo/bar", body);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar");
    assertBody(request.body(), "hi");
  }

  @Test
  public void normalPostWithPathParam() {
    class Example {
      @POST("/foo/bar/{ping}/") //
      Call<ResponseBody> method(@Path("ping") String ping, @Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(TEXT_PLAIN, "Hi!");
    Request request = buildRequest(Example.class, "pong", body);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/pong/");
    assertBody(request.body(), "Hi!");
  }

  @Test
  public void emptyBody() {
    class Example {
      @POST("/foo/bar/") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertBody(request.body(), "");
  }

  @Test
  public void customMethodEmptyBody() {
    class Example {
      @HTTP(method = "CUSTOM", path = "/foo/bar/", hasBody = true) //
      Call<ResponseBody> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("CUSTOM");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertBody(request.body(), "");
  }

  @Test
  public void bodyRequired() {
    class Example {
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Body RequestBody body) {
        return null;
      }
    }
    try {
      buildRequest(Example.class, new Object[] {null});
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage())
          .isEqualTo(
              "Body parameter value must not be null. (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void bodyWithPathParams() {
    class Example {
      @POST("/foo/bar/{ping}/{kit}/") //
      Call<ResponseBody> method(
          @Path("ping") String ping, @Body RequestBody body, @Path("kit") String kit) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(TEXT_PLAIN, "Hi!");
    Request request = buildRequest(Example.class, "pong", body, "kat");
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/pong/kat/");
    assertBody(request.body(), "Hi!");
  }

  @Test
  public void simpleMultipart() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Part("ping") String ping, @Part("kit") RequestBody kit) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "pong", RequestBody.create(TEXT_PLAIN, "kat"));
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    assertThat(body.contentType().toString()).startsWith("multipart/form-data; boundary=");

    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"ping\"\r\n")
        .contains("\r\npong\r\n--");

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"kit\"")
        .contains("\r\nkat\r\n--");
  }

  @Test
  public void multipartArray() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Part("ping") String[] ping) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, new Object[] {new String[] {"pong1", "pong2"}});
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"ping\"\r\n")
        .contains("\r\npong1\r\n--");

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"ping\"")
        .contains("\r\npong2\r\n--");
  }

  @Test
  public void multipartRequiresName() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Part RequestBody part) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, new Object[] {null});
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Part annotation must supply a name or use MultipartBody.Part parameter type. (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void multipartIterableRequiresName() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Part List<RequestBody> part) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, new Object[] {null});
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Part annotation must supply a name or use MultipartBody.Part parameter type. (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void multipartArrayRequiresName() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Part RequestBody[] part) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, new Object[] {null});
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Part annotation must supply a name or use MultipartBody.Part parameter type. (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void multipartOkHttpPartForbidsName() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Part("name") MultipartBody.Part part) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, new Object[] {null});
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Part parameters using the MultipartBody.Part must not include a part name in the annotation. (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void multipartOkHttpPart() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Part MultipartBody.Part part) {
        return null;
      }
    }

    MultipartBody.Part part = MultipartBody.Part.createFormData("kit", "kat");
    Request request = buildRequest(Example.class, part);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"kit\"\r\n")
        .contains("\r\nkat\r\n--");
  }

  @Test
  public void multipartOkHttpIterablePart() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Part List<MultipartBody.Part> part) {
        return null;
      }
    }

    MultipartBody.Part part1 = MultipartBody.Part.createFormData("foo", "bar");
    MultipartBody.Part part2 = MultipartBody.Part.createFormData("kit", "kat");
    Request request = buildRequest(Example.class, asList(part1, part2));
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"foo\"\r\n")
        .contains("\r\nbar\r\n--");

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"kit\"\r\n")
        .contains("\r\nkat\r\n--");
  }

  @Test
  public void multipartOkHttpArrayPart() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Part MultipartBody.Part[] part) {
        return null;
      }
    }

    MultipartBody.Part part1 = MultipartBody.Part.createFormData("foo", "bar");
    MultipartBody.Part part2 = MultipartBody.Part.createFormData("kit", "kat");
    Request request =
        buildRequest(Example.class, new Object[] {new MultipartBody.Part[] {part1, part2}});
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"foo\"\r\n")
        .contains("\r\nbar\r\n--");

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"kit\"\r\n")
        .contains("\r\nkat\r\n--");
  }

  @Test
  public void multipartOkHttpPartWithFilename() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Part MultipartBody.Part part) {
        return null;
      }
    }

    MultipartBody.Part part =
        MultipartBody.Part.createFormData("kit", "kit.txt", RequestBody.create(null, "kat"));
    Request request = buildRequest(Example.class, part);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"kit\"; filename=\"kit.txt\"\r\n")
        .contains("\r\nkat\r\n--");
  }

  @Test
  public void multipartIterable() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Part("ping") List<String> ping) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, asList("pong1", "pong2"));
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"ping\"\r\n")
        .contains("\r\npong1\r\n--");

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"ping\"")
        .contains("\r\npong2\r\n--");
  }

  @Test
  public void multipartIterableOkHttpPart() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Part("ping") List<MultipartBody.Part> part) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, new Object[] {null});
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Part parameters using the MultipartBody.Part must not include a part name in the annotation. (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void multipartArrayOkHttpPart() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Part("ping") MultipartBody.Part[] part) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, new Object[] {null});
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Part parameters using the MultipartBody.Part must not include a part name in the annotation. (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void multipartWithEncoding() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(
          @Part(value = "ping", encoding = "8-bit") String ping,
          @Part(value = "kit", encoding = "7-bit") RequestBody kit) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "pong", RequestBody.create(TEXT_PLAIN, "kat"));
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"ping\"\r\n")
        .contains("Content-Transfer-Encoding: 8-bit")
        .contains("\r\npong\r\n--");

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"kit\"")
        .contains("Content-Transfer-Encoding: 7-bit")
        .contains("\r\nkat\r\n--");
  }

  @Test
  public void multipartPartMap() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@PartMap Map<String, RequestBody> parts) {
        return null;
      }
    }

    Map<String, RequestBody> params = new LinkedHashMap<>();
    params.put("ping", RequestBody.create(null, "pong"));
    params.put("kit", RequestBody.create(null, "kat"));

    Request request = buildRequest(Example.class, params);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"ping\"\r\n")
        .contains("\r\npong\r\n--");

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"kit\"")
        .contains("\r\nkat\r\n--");
  }

  @Test
  public void multipartPartMapWithEncoding() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@PartMap(encoding = "8-bit") Map<String, RequestBody> parts) {
        return null;
      }
    }

    Map<String, RequestBody> params = new LinkedHashMap<>();
    params.put("ping", RequestBody.create(null, "pong"));
    params.put("kit", RequestBody.create(null, "kat"));

    Request request = buildRequest(Example.class, params);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"ping\"\r\n")
        .contains("Content-Transfer-Encoding: 8-bit")
        .contains("\r\npong\r\n--");

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"kit\"")
        .contains("Content-Transfer-Encoding: 8-bit")
        .contains("\r\nkat\r\n--");
  }

  @Test
  public void multipartPartMapRejectsNonStringKeys() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@PartMap Map<Object, RequestBody> parts) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, new Object[] {null});
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@PartMap keys must be of type String: class java.lang.Object (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void multipartPartMapRejectsOkHttpPartValues() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@PartMap Map<String, MultipartBody.Part> parts) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, new Object[] {null});
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@PartMap values cannot be MultipartBody.Part. Use @Part List<Part> or a different value type instead. (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void multipartPartMapRejectsNull() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@PartMap Map<String, RequestBody> parts) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, new Object[] {null});
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage("Part map was null. (parameter #1)\n" + "    for method Example.method");
    }
  }

  @Test
  public void multipartPartMapRejectsNullKeys() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@PartMap Map<String, RequestBody> parts) {
        return null;
      }
    }

    Map<String, RequestBody> params = new LinkedHashMap<>();
    params.put("ping", RequestBody.create(null, "pong"));
    params.put(null, RequestBody.create(null, "kat"));

    try {
      buildRequest(Example.class, params);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Part map contained null key. (parameter #1)\n" + "    for method Example.method");
    }
  }

  @Test
  public void multipartPartMapRejectsNullValues() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@PartMap Map<String, RequestBody> parts) {
        return null;
      }
    }

    Map<String, RequestBody> params = new LinkedHashMap<>();
    params.put("ping", RequestBody.create(null, "pong"));
    params.put("kit", null);

    try {
      buildRequest(Example.class, params);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Part map contained null value for key 'kit'. (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void multipartPartMapMustBeMap() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@PartMap List<Object> parts) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, emptyList());
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@PartMap parameter type must be Map. (parameter #1)\n    for method Example.method");
    }
  }

  @Test
  public void multipartPartMapSupportsSubclasses() throws IOException {
    class Foo extends HashMap<String, String> {}

    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@PartMap Foo parts) {
        return null;
      }
    }

    Foo foo = new Foo();
    foo.put("hello", "world");

    Request request = buildRequest(Example.class, foo);
    Buffer buffer = new Buffer();
    request.body().writeTo(buffer);
    assertThat(buffer.readUtf8()).contains("name=\"hello\"").contains("\r\n\r\nworld\r\n--");
  }

  @Test
  public void multipartNullRemovesPart() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Part("ping") String ping, @Part("fizz") String fizz) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "pong", null);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");

    RequestBody body = request.body();
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String bodyString = buffer.readUtf8();

    assertThat(bodyString)
        .contains("Content-Disposition: form-data;")
        .contains("name=\"ping\"")
        .contains("\r\npong\r\n--");
  }

  @Test
  public void multipartPartOptional() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Call<ResponseBody> method(@Part("ping") RequestBody ping) {
        return null;
      }
    }
    try {
      buildRequest(Example.class, new Object[] {null});
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Multipart body must have at least one part.");
    }
  }

  @Test
  public void simpleFormEncoded() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<ResponseBody> method(@Field("foo") String foo, @Field("ping") String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "bar", "pong");
    RequestBody body = request.body();
    assertBody(body, "foo=bar&ping=pong");
    assertThat(body.contentType().toString()).isEqualTo("application/x-www-form-urlencoded");
  }

  @Test
  public void formEncodedWithEncodedNameFieldParam() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<ResponseBody> method(@Field(value = "na%20me", encoded = true) String foo) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "ba%20r");
    assertBody(request.body(), "na%20me=ba%20r");
  }

  @Test
  public void formEncodedFieldOptional() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<ResponseBody> method(
          @Field("foo") String foo, @Field("ping") String ping, @Field("kit") String kit) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "bar", null, "kat");
    assertBody(request.body(), "foo=bar&kit=kat");
  }

  @Test
  public void formEncodedFieldList() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<ResponseBody> method(@Field("foo") List<Object> fields, @Field("kit") String kit) {
        return null;
      }
    }

    List<Object> values = Arrays.asList("foo", "bar", null, 3);
    Request request = buildRequest(Example.class, values, "kat");
    assertBody(request.body(), "foo=foo&foo=bar&foo=3&kit=kat");
  }

  @Test
  public void formEncodedFieldArray() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<ResponseBody> method(@Field("foo") Object[] fields, @Field("kit") String kit) {
        return null;
      }
    }

    Object[] values = {1, 2, null, "three"};
    Request request = buildRequest(Example.class, values, "kat");
    assertBody(request.body(), "foo=1&foo=2&foo=three&kit=kat");
  }

  @Test
  public void formEncodedFieldPrimitiveArray() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<ResponseBody> method(@Field("foo") int[] fields, @Field("kit") String kit) {
        return null;
      }
    }

    int[] values = {1, 2, 3};
    Request request = buildRequest(Example.class, values, "kat");
    assertBody(request.body(), "foo=1&foo=2&foo=3&kit=kat");
  }

  @Test
  public void formEncodedWithEncodedNameFieldParamMap() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<ResponseBody> method(@FieldMap(encoded = true) Map<String, Object> fieldMap) {
        return null;
      }
    }

    Map<String, Object> fieldMap = new LinkedHashMap<>();
    fieldMap.put("k%20it", "k%20at");
    fieldMap.put("pin%20g", "po%20ng");

    Request request = buildRequest(Example.class, fieldMap);
    assertBody(request.body(), "k%20it=k%20at&pin%20g=po%20ng");
  }

  @Test
  public void formEncodedFieldMap() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<ResponseBody> method(@FieldMap Map<String, Object> fieldMap) {
        return null;
      }
    }

    Map<String, Object> fieldMap = new LinkedHashMap<>();
    fieldMap.put("kit", "kat");
    fieldMap.put("ping", "pong");

    Request request = buildRequest(Example.class, fieldMap);
    assertBody(request.body(), "kit=kat&ping=pong");
  }

  @Test
  public void fieldMapRejectsNull() {
    class Example {
      @FormUrlEncoded //
      @POST("/") //
      Call<ResponseBody> method(@FieldMap Map<String, Object> a) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, new Object[] {null});
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage("Field map was null. (parameter #1)\n" + "    for method Example.method");
    }
  }

  @Test
  public void fieldMapRejectsNullKeys() {
    class Example {
      @FormUrlEncoded //
      @POST("/") //
      Call<ResponseBody> method(@FieldMap Map<String, Object> a) {
        return null;
      }
    }

    Map<String, Object> fieldMap = new LinkedHashMap<>();
    fieldMap.put("kit", "kat");
    fieldMap.put(null, "pong");

    try {
      buildRequest(Example.class, fieldMap);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Field map contained null key. (parameter #1)\n" + "    for method Example.method");
    }
  }

  @Test
  public void fieldMapRejectsNullValues() {
    class Example {
      @FormUrlEncoded //
      @POST("/") //
      Call<ResponseBody> method(@FieldMap Map<String, Object> a) {
        return null;
      }
    }

    Map<String, Object> fieldMap = new LinkedHashMap<>();
    fieldMap.put("kit", "kat");
    fieldMap.put("foo", null);

    try {
      buildRequest(Example.class, fieldMap);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Field map contained null value for key 'foo'. (parameter #1)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void fieldMapMustBeAMap() {
    class Example {
      @FormUrlEncoded //
      @POST("/") //
      Call<ResponseBody> method(@FieldMap List<String> a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@FieldMap parameter type must be Map. (parameter #1)\n    for method Example.method");
    }
  }

  @Test
  public void fieldMapSupportsSubclasses() throws IOException {
    class Foo extends HashMap<String, String> {}

    class Example {
      @FormUrlEncoded //
      @POST("/") //
      Call<ResponseBody> method(@FieldMap Foo a) {
        return null;
      }
    }

    Foo foo = new Foo();
    foo.put("hello", "world");

    Request request = buildRequest(Example.class, foo);
    Buffer buffer = new Buffer();
    request.body().writeTo(buffer);
    assertThat(buffer.readUtf8()).isEqualTo("hello=world");
  }

  @Test
  public void simpleHeaders() {
    class Example {
      @GET("/foo/bar/")
      @Headers({"ping: pong", "kit: kat"})
      Call<ResponseBody> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("GET");
    okhttp3.Headers headers = request.headers();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.get("ping")).isEqualTo("pong");
    assertThat(headers.get("kit")).isEqualTo("kat");
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void simpleHeadersAllowingUnsafeNonAsciiValues() {
    class Example {
      @GET("/foo/bar/")
      @Headers(
          value = {"ping: pong", "title: Kein plötzliches"},
          allowUnsafeNonAsciiValues = true)
      Call<ResponseBody> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("GET");
    okhttp3.Headers headers = request.headers();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.get("ping")).isEqualTo("pong");
    assertThat(headers.get("title")).isEqualTo("Kein plötzliches");
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void headersDoNotOverwriteEachOther() {
    class Example {
      @GET("/foo/bar/")
      @Headers({
        "ping: pong",
        "kit: kat",
        "kit: -kat",
      })
      Call<ResponseBody> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("GET");
    okhttp3.Headers headers = request.headers();
    assertThat(headers.size()).isEqualTo(3);
    assertThat(headers.get("ping")).isEqualTo("pong");
    assertThat(headers.values("kit")).containsOnly("kat", "-kat");
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void headerParamToString() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@Header("kit") BigInteger kit) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, new BigInteger("1234"));
    assertThat(request.method()).isEqualTo("GET");
    okhttp3.Headers headers = request.headers();
    assertThat(headers.size()).isEqualTo(1);
    assertThat(headers.get("kit")).isEqualTo("1234");
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void headerParam() {
    class Example {
      @GET("/foo/bar/") //
      @Headers("ping: pong") //
      Call<ResponseBody> method(@Header("kit") String kit) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "kat");
    assertThat(request.method()).isEqualTo("GET");
    okhttp3.Headers headers = request.headers();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.get("ping")).isEqualTo("pong");
    assertThat(headers.get("kit")).isEqualTo("kat");
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void headerParamAllowingUnsafeNonAsciiValues() {
    class Example {
      @GET("/foo/bar/") //
      @Headers("ping: pong") //
      Call<ResponseBody> method(
          @Header(value = "title", allowUnsafeNonAsciiValues = true) String kit) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "Kein plötzliches");
    assertThat(request.method()).isEqualTo("GET");
    okhttp3.Headers headers = request.headers();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.get("ping")).isEqualTo("pong");
    assertThat(headers.get("title")).isEqualTo("Kein plötzliches");
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void headerParamList() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@Header("foo") List<String> kit) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, asList("bar", null, "baz"));
    assertThat(request.method()).isEqualTo("GET");
    okhttp3.Headers headers = request.headers();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.values("foo")).containsExactly("bar", "baz");
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void headerParamArray() {
    class Example {
      @GET("/foo/bar/") //
      Call<ResponseBody> method(@Header("foo") String[] kit) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, (Object) new String[] {"bar", null, "baz"});
    assertThat(request.method()).isEqualTo("GET");
    okhttp3.Headers headers = request.headers();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.values("foo")).containsExactly("bar", "baz");
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void contentTypeAnnotationHeaderOverrides() {
    class Example {
      @POST("/") //
      @Headers("Content-Type: text/not-plain") //
      Call<ResponseBody> method(@Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(TEXT_PLAIN, "hi");
    Request request = buildRequest(Example.class, body);
    assertThat(request.body().contentType().toString()).isEqualTo("text/not-plain");
  }

  @Test
  public void contentTypeAnnotationHeaderOverridesFormEncoding() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      @Headers("Content-Type: text/not-plain") //
      Call<ResponseBody> method(@Field("foo") String foo, @Field("ping") String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "bar", "pong");
    assertThat(request.body().contentType().toString()).isEqualTo("text/not-plain");
  }

  @Test
  public void contentTypeAnnotationHeaderOverridesMultipart() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      @Headers("Content-Type: text/not-plain") //
      Call<ResponseBody> method(@Part("ping") String ping, @Part("kit") RequestBody kit) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "pong", RequestBody.create(TEXT_PLAIN, "kat"));

    RequestBody body = request.body();
    assertThat(request.body().contentType().toString()).isEqualTo("text/not-plain");
  }

  @Test
  public void malformedContentTypeHeaderThrows() {
    class Example {
      @POST("/") //
      @Headers("Content-Type: hello, world!") //
      Call<ResponseBody> method(@Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(TEXT_PLAIN, "hi");
    try {
      buildRequest(Example.class, body);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage("Malformed content type: hello, world!\n" + "    for method Example.method");
      assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class); // OkHttp's cause.
    }
  }

  @Test
  public void contentTypeAnnotationHeaderAddsHeaderWithNoBodyGet() {
    class Example {
      @GET("/") //
      @Headers("Content-Type: text/not-plain") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.headers().get("Content-Type")).isEqualTo("text/not-plain");
  }

  @Test
  public void contentTypeAnnotationHeaderAddsHeaderWithNoBodyDelete() {
    class Example {
      @DELETE("/") //
      @Headers("Content-Type: text/not-plain") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.headers().get("Content-Type")).isEqualTo("text/not-plain");
  }

  @Test
  public void contentTypeParameterHeaderOverrides() {
    class Example {
      @POST("/") //
      Call<ResponseBody> method(
          @Header("Content-Type") String contentType, @Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(TEXT_PLAIN, "Plain");
    Request request = buildRequest(Example.class, "text/not-plain", body);
    assertThat(request.body().contentType().toString()).isEqualTo("text/not-plain");
  }

  @Test
  public void malformedContentTypeParameterThrows() {
    class Example {
      @POST("/") //
      Call<ResponseBody> method(
          @Header("Content-Type") String contentType, @Body RequestBody body) {
        return null;
      }
    }
    RequestBody body = RequestBody.create(TEXT_PLAIN, "hi");
    try {
      buildRequest(Example.class, "hello, world!", body);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Malformed content type: hello, world!");
      assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class); // OkHttp's cause.
    }
  }

  @Test
  public void malformedAnnotationRelativeUrlThrows() {
    class Example {
      @GET("ftp://example.org")
      Call<ResponseBody> get() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage("Malformed URL. Base: http://example.com/, Relative: ftp://example.org");
    }
  }

  @Test
  public void malformedParameterRelativeUrlThrows() {
    class Example {
      @GET
      Call<ResponseBody> get(@Url String relativeUrl) {
        return null;
      }
    }
    try {
      buildRequest(Example.class, "ftp://example.org");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage("Malformed URL. Base: http://example.com/, Relative: ftp://example.org");
    }
  }

  @Test
  public void multipartPartsShouldBeInOrder() throws IOException {
    class Example {
      @Multipart
      @POST("/foo")
      Call<ResponseBody> get(
          @Part("first") String data,
          @Part("second") String dataTwo,
          @Part("third") String dataThree) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "firstParam", "secondParam", "thirdParam");
    MultipartBody body = (MultipartBody) request.body();

    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    String readBody = buffer.readUtf8();

    assertThat(readBody.indexOf("firstParam")).isLessThan(readBody.indexOf("secondParam"));
    assertThat(readBody.indexOf("secondParam")).isLessThan(readBody.indexOf("thirdParam"));
  }

  @Test
  public void queryParamsSkippedIfConvertedToNull() throws Exception {
    class Example {
      @GET("/query")
      Call<ResponseBody> queryPath(@Query("a") Object a) {
        return null;
      }
    }

    Retrofit.Builder retrofitBuilder =
        new Retrofit.Builder()
            .baseUrl("http://example.com")
            .addConverterFactory(new NullObjectConverterFactory());

    Request request = buildRequest(Example.class, retrofitBuilder, "Ignored");

    assertThat(request.url().toString()).doesNotContain("Ignored");
  }

  @Test
  public void queryParamMapsConvertedToNullShouldError() throws Exception {
    class Example {
      @GET("/query")
      Call<ResponseBody> queryPath(@QueryMap Map<String, String> a) {
        return null;
      }
    }

    Retrofit.Builder retrofitBuilder =
        new Retrofit.Builder()
            .baseUrl("http://example.com")
            .addConverterFactory(new NullObjectConverterFactory());

    Map<String, String> queryMap = Collections.singletonMap("kit", "kat");

    try {
      buildRequest(Example.class, retrofitBuilder, queryMap);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessageContaining(
              "Query map value 'kat' converted to null by retrofit2.helpers.NullObjectConverterFactory$1 for key 'kit'.");
    }
  }

  @Test
  public void fieldParamsSkippedIfConvertedToNull() throws Exception {
    class Example {
      @FormUrlEncoded
      @POST("/query")
      Call<ResponseBody> queryPath(@Field("a") Object a) {
        return null;
      }
    }

    Retrofit.Builder retrofitBuilder =
        new Retrofit.Builder()
            .baseUrl("http://example.com")
            .addConverterFactory(new NullObjectConverterFactory());

    Request request = buildRequest(Example.class, retrofitBuilder, "Ignored");

    assertThat(request.url().toString()).doesNotContain("Ignored");
  }

  @Test
  public void fieldParamMapsConvertedToNullShouldError() throws Exception {
    class Example {
      @FormUrlEncoded
      @POST("/query")
      Call<ResponseBody> queryPath(@FieldMap Map<String, String> a) {
        return null;
      }
    }

    Retrofit.Builder retrofitBuilder =
        new Retrofit.Builder()
            .baseUrl("http://example.com")
            .addConverterFactory(new NullObjectConverterFactory());

    Map<String, String> queryMap = Collections.singletonMap("kit", "kat");

    try {
      buildRequest(Example.class, retrofitBuilder, queryMap);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessageContaining(
              "Field map value 'kat' converted to null by retrofit2.helpers.NullObjectConverterFactory$1 for key 'kit'.");
    }
  }

  @Test
  public void tag() {
    class Example {
      @GET("/")
      Call<ResponseBody> method(@Tag String tag) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "tagValue");
    assertThat(request.tag(String.class)).isEqualTo("tagValue");
  }

  @Test
  public void tagGeneric() {
    class Example {
      @GET("/")
      Call<ResponseBody> method(@Tag List<String> tag) {
        return null;
      }
    }

    List<String> strings = asList("tag", "value");
    Request request = buildRequest(Example.class, strings);
    assertThat(request.tag(List.class)).isSameAs(strings);
  }

  @Test
  public void tagDuplicateFails() {
    class Example {
      @GET("/")
      Call<ResponseBody> method(@Tag String one, @Tag String two) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, "one", "two");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Tag type java.lang.String is duplicate of parameter #1 and would always overwrite its value. (parameter #2)\n"
                  + "    for method Example.method");
    }
  }

  @Test
  public void tagGenericDuplicateFails() {
    class Example {
      @GET("/")
      Call<ResponseBody> method(@Tag List<String> one, @Tag List<Long> two) {
        return null;
      }
    }

    try {
      buildRequest(Example.class, emptyList(), emptyList());
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "@Tag type java.util.List is duplicate of parameter #1 and would always overwrite its value. (parameter #2)\n"
                  + "    for method Example.method");
    }
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

  static void assertMalformedRequest(Class<?> cls, Object... args) {
    try {
      Request request = buildRequest(cls, args);
      fail("expected a malformed request but was " + request);
    } catch (IllegalArgumentException expected) {
      // Ignored
    }
  }
}
