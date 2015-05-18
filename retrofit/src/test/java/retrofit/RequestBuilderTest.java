// Copyright 2013 Square, Inc.
package retrofit;

import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import okio.Buffer;
import org.junit.Ignore;
import org.junit.Test;
import retrofit.converter.Converter;
import retrofit.converter.GsonConverter;
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
import retrofit.http.Streaming;
import rx.Observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@SuppressWarnings("UnusedParameters") // Parameters inspected reflectively.
public class RequestBuilderTest {
  @Test public void custom1Method() {
    class Example {
      @HTTP(method = "CUSTOM1", path = "/foo")
      Response method() {
        return null;
      }
    }

    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("CUSTOM1");
    assertThat(request.urlString()).isEqualTo("http://example.com/foo");
    assertThat(request.body()).isNull();
  }

  @Ignore // TODO https://github.com/square/okhttp/issues/229
  @Test public void custom2Method() {
    class Example {
      @HTTP(method = "CUSTOM2", path = "/foo", hasBody = true)
      Response method(@Body RequestBody body) {
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
      Response method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.method: Only one encoding annotation is allowed.");
    }
  }

  @Test public void onlyOneEncodingIsAllowedFormEncodingFirst() {
    class Example {
      @FormUrlEncoded //
      @Multipart //
      @POST("/") //
      Response method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.method: Only one encoding annotation is allowed.");
    }
  }

  @Test public void invalidPathParam() throws Exception {
    class Example {
      @GET("/") //
      Response method(@Path("hey!") String thing) {
        return null;
      }
    }

    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: @Path parameter name must match \\{([a-zA-Z][a-zA-Z0-9_-]*)\\}."
              + " Found: hey! (parameter #1)");
    }
  }

  @Test public void pathParamNotAllowedInQuery() throws Exception {
    class Example {
      @GET("/foo?bar={bar}") //
      Response method(@Path("bar") String thing) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: URL query string \"bar={bar}\" must not have replace block."
              + " For dynamic query parameters use @Query.");
    }
  }

  @Test public void multipleParameterAnnotationsNotAllowed() throws Exception {
    class Example {
      @GET("/") //
      Response method(@Body @Query("nope") Object o) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: Multiple Retrofit annotations found, only one allowed:"
              + " @Body, @Query. (parameter #1)");
    }
  }

  @Test public void twoMethodsFail() {
    class Example {
      @PATCH("/foo") //
      @POST("/foo") //
      Response method() {
        return null;
      }
    }

    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: Only one HTTP method is allowed. Found: PATCH and POST.");
    }
  }

  @Test public void pathMustBePrefixedWithSlash() {
    class Example {
      @GET("foo/bar") //
      Response method() {
        return null;
      }
    }

    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.method: URL path \"foo/bar\" must start with '/'.");
    }
  }

  @Test public void streamingResponseNotAllowed() {
    class Example {
      @GET("/foo") //
      @Streaming //
      String method() {
        return null;
      }
    }

    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: Only methods having Response as data type are allowed to have @Streaming annotation.");
    }
  }

  @Test public void streamingResponseWithCallbackNotAllowed() {
    class Example {
      @GET("/foo") //
      @Streaming //
      void method(Callback<String> callback) {
      }
    }

    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: Only methods having Response as data type are allowed to have @Streaming annotation.");
    }
  }

  @Test public void observableWithCallback() {
    class Example {
      @GET("/foo") //
      Observable<Response> method(Callback<Response> callback) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: Must have return type or Callback as last argument, not both.");
    }
  }

  @Test public void missingCallbackTypes() {
    class Example {
      @GET("/foo") //
      void method(@Query("id") String id) {
      }
    }

    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: Must have either a return type or Callback as last argument.");
    }
  }

  @Test public void nonParameterizedCallbackFails() {
    class Example {
      @GET("/foo") //
      void method(Callback cb) {
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: Last parameter must be of type Callback<X> or Callback<? super X>.");
    }
  }

  @Test public void synchronousWithAsyncCallback() {
    class Example {
      @GET("/foo") //
      Response method(Callback<Response> callback) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: Must have return type or Callback as last argument, not both.");
    }
  }

  @Test public void lackingMethod() {
    class Example {
      Response method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: HTTP method annotation is required (e.g., @GET, @POST, etc.).");
    }
  }

  @Test public void implicitMultipartForbidden() {
    class Example {
      @POST("/") //
      Response method(@Part("a") int a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: @Part parameters can only be used with multipart encoding. (parameter #1)");
    }
  }

  @Test public void implicitMultipartWithPartMapForbidden() {
    class Example {
      @POST("/") //
      Response method(@PartMap Map<String, String> params) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: @PartMap parameters can only be used with multipart encoding. (parameter #1)");
    }
  }

  @Test public void multipartFailsOnNonBodyMethod() {
    class Example {
      @Multipart //
      @GET("/") //
      Response method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
    }
  }

  @Test public void multipartFailsWithNoParts() {
    class Example {
      @Multipart //
      @POST("/") //
      Response method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.method: Multipart method must contain at least one @Part.");
    }
  }

  @Test public void implicitFormEncodingByFieldForbidden() {
    class Example {
      @POST("/") //
      Response method(@Field("a") int a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: @Field parameters can only be used with form encoding. (parameter #1)");
    }
  }

  @Test public void implicitFormEncodingByFieldMapForbidden() {
    class Example {
      @POST("/") //
      Response method(@FieldMap Map<String, String> a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: @FieldMap parameters can only be used with form encoding. (parameter #1)");
    }
  }

  //@Test public void formEncodingFailsOnNonBodyMethod() {
  //  class Example {
  //    @FormUrlEncoded //
  //    @GET("/") //
  //    Response method() {
  //      return null;
  //    }
  //  }
  //  try {
  //    buildRequest(Example.class);
  //    fail();
  //  } catch (IllegalArgumentException e) {
  //    assertThat(e).hasMessage(
  //        "Example.method: FormUrlEncoded can only be specified on HTTP methods with request body (e.g., @POST).");
  //  }
  //}
  //
  //@Test public void formEncodingFailsWithNoParts() {
  //  class Example {
  //    @FormUrlEncoded //
  //    @POST("/") //
  //    Response method() {
  //      return null;
  //    }
  //  }
  //  try {
  //    buildRequest(Example.class);
  //    fail();
  //  } catch (IllegalArgumentException e) {
  //    assertThat(e).hasMessage("Example.method: Form-encoded method must contain at least one @Field.");
  //  }
  //}

  @Test public void headersFailWhenEmptyOnMethod() {
    class Example {
      @GET("/") //
      @Headers({}) //
      Response method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.method: @Headers annotation is empty.");
    }
  }

  @Test public void headersFailWhenMalformed() {
    class Example {
      @GET("/") //
      @Headers("Malformed") //
      Response method() {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: @Headers value must be in the form \"Name: Value\". Found: \"Malformed\"");
    }
  }

  @Test public void pathParamNonPathParamAndTypedBytes() {
    class Example {
      @PUT("/{a}") //
      Response method(@Path("a") int a, @Path("b") int b, @Body int c) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.method: URL \"/{a}\" does not contain \"{b}\". (parameter #2)");
    }
  }

  @Test public void parameterWithoutAnnotation() {
    class Example {
      @GET("/") //
      Response method(String a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.method: No Retrofit annotation found. (parameter #1)");
    }
  }

  @Test public void nonBodyHttpMethodWithSingleEntity() {
    class Example {
      @GET("/") //
      Response method(@Body Object o) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: Non-body HTTP method cannot contain @Body or @TypedOutput.");
    }
  }

  @Test public void queryMapMustBeAMap() {
    class Example {
      @GET("/") //
      Response method(@QueryMap List<String> a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.method: @QueryMap parameter type must be Map. (parameter #1)");
    }
  }

  @Test public void queryMapRejectsNullKeys() {
    class Example {
      @GET("/") //
      Response method(@QueryMap Map<String, String> a) {
        return null;
      }
    }

    Map<String, String> queryParams = new LinkedHashMap<String, String>();
    queryParams.put("ping", "pong");
    queryParams.put(null, "kat");

    try {
      buildRequest(Example.class, queryParams);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Parameter #1 query map contained null key.");
    }
  }

  @Test public void twoBodies() {
    class Example {
      @PUT("/") //
      Response method(@Body int o1, @Body int o2) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.method: Multiple @Body method annotations found.");
    }
  }

  @Test public void bodyInNonBodyRequest() {
    class Example {
      @Multipart //
      @PUT("/") //
      Response method(@Part("one") int o1, @Body int o2) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.method: @Body parameters cannot be used with form or multi-part encoding. (parameter #2)");
    }
  }

  @Test public void get() {
    class Example {
      @GET("/foo/bar/") //
      Response method() {
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
      Response method() {
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
      Response method() {
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
      Response method(@Body RequestBody body) {
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
      Response method(@Body RequestBody body) {
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
      Response method(@Body RequestBody body) {
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
      Response method(@Path("ping") String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "po ng");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/po%20ng/");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithEncodedPathParam() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Response method(@Path(value = "ping", encode = false) String ping) {
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
      Response method(@Path("ping") String ping) {
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
      Response method(@Query("ping") String ping) {
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
      Response method(@Query(value = "ping", encodeValue = false) String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "p%20o%20n%20g");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?ping=p%20o%20n%20g");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithEncodeNameQueryParam() {
    class Example {
      @GET("/foo/bar/") //
      Response method(@Query(value = "pi ng", encodeName = true) String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "pong");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?pi%20ng=pong");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithEncodeNameEncodedValueQueryParam() {
    class Example {
      @GET("/foo/bar/") //
      Response method(@Query(value = "pi ng", encodeName = true, encodeValue = false) String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "po%20ng");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?pi%20ng=po%20ng");
    assertThat(request.body()).isNull();
  }

  @Test public void queryParamOptionalOmitsQuery() {
    class Example {
      @GET("/foo/bar/") //
      Response method(@Query("ping") String ping) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, new Object[] { null });
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
  }

  @Test public void queryParamOptional() {
    class Example {
      @GET("/foo/bar/") //
      Response method(@Query("foo") String foo, @Query("ping") String ping,
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
      Response method(@Query("ping") String ping) {
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
      Response method() {
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
      Response method(@Path("ping") String ping, @Query("kit") String kit,
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

  @Test public void getWithPathAndQueryQuestionMarkParam() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Response method(@Path("ping") String ping, @Query("kit") String kit) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, "pong?", "kat?");
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/pong%3F/?kit=kat%3F");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithPathAndQueryAmpersandParam() {
    class Example {
      @GET("/foo/bar/{ping}/") //
      Response method(@Path("ping") String ping, @Query("kit") String kit) {
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
      Response method(@Path("ping") String ping, @Query("kit") String kit) {
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
      Response method(@Query("key") List<Object> keys) {
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
      Response method(@Query("key") Object[] keys) {
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
      Response method(@Query("key") int[] keys) {
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
      Response method(@QueryMap Map<String, Object> query) {
        return null;
      }
    }

    Map<String, Object> params = new LinkedHashMap<String, Object>();
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
      Response method(@QueryMap(encodeValues = false) Map<String, Object> query) {
        return null;
      }
    }

    Map<String, Object> params = new LinkedHashMap<String, Object>();
    params.put("kit", "k%20t");
    params.put("foo", null);
    params.put("ping", "p%20g");

    Request request = buildRequest(Example.class, params);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?kit=k%20t&ping=p%20g");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithEncodeNameQueryParamMap() {
    class Example {
      @GET("/foo/bar/") //
      Response method(@QueryMap(encodeNames = true) Map<String, Object> query) {
        return null;
      }
    }

    Map<String, Object> params = new LinkedHashMap<String, Object>();
    params.put("k it", "k t");
    params.put("fo o", null);
    params.put("pi ng", "p g");

    Request request = buildRequest(Example.class, params);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?k%20it=k%20t&pi%20ng=p%20g");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithEncodeNameEncodedValueQueryParamMap() {
    class Example {
      @GET("/foo/bar/") //
      Response method(
          @QueryMap(encodeNames = true, encodeValues = false) Map<String, Object> query) {
        return null;
      }
    }

    Map<String, Object> params = new LinkedHashMap<String, Object>();
    params.put("k it", "k%20t");
    params.put("fo o", null);
    params.put("pi ng", "p%20g");

    Request request = buildRequest(Example.class, params);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/?k%20it=k%20t&pi%20ng=p%20g");
    assertThat(request.body()).isNull();
  }

  @Test public void normalPostWithPathParam() {
    class Example {
      @POST("/foo/bar/{ping}/") //
      Response method(@Path("ping") String ping, @Body Object body) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, "pong", new Object());
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/pong/");
    assertBody(request.body(), "{}");
  }

  @Test public void bodyGson() {
    class Example {
      @POST("/foo/bar/") //
      Response method(@Body Object body) {
        return null;
      }
    }
    Request request = buildRequest(Example.class, Arrays.asList("quick", "brown", "fox"));
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/");
    assertBody(request.body(), "[\"quick\",\"brown\",\"fox\"]");
  }

  @Test public void bodyResponseBody() {
    class Example {
      @POST("/foo/bar/") //
      Response method(@Body RequestBody body) {
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

  @Test public void bodyRequired() {
    class Example {
      @POST("/foo/bar/") //
      Response method(@Body RequestBody body) {
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
      Response method(@Path("ping") String ping, @Body Object body, @Path("kit") String kit) {
        return null;
      }
    }
    Request request =
        buildRequest(Example.class, "pong", Arrays.asList("quick", "brown", "fox"), "kat");
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().size()).isZero();
    assertThat(request.urlString()).isEqualTo("http://example.com/foo/bar/pong/kat/");
    assertBody(request.body(), "[\"quick\",\"brown\",\"fox\"]");
  }

  @Test public void simpleMultipart() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Response method(@Part("ping") String ping, @Part("kit") ResponseBody kit) {
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
      Response method(@Part(value = "ping", encoding = "8-bit") String ping,
          @Part(value = "kit", encoding = "7-bit") ResponseBody kit) {
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
      Response method(@PartMap Map<String, Object> parts) {
        return null;
      }
    }

    Map<String, Object> params = new LinkedHashMap<String, Object>();
    params.put("ping", "pong");
    params.put("kit", RequestBody.create(MediaType.parse("text/plain"), "kat"));

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
  }

  @Test public void multipartPartMapWithEncoding() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Response method(@PartMap(encoding = "8-bit") Map<String, Object> parts) {
        return null;
      }
    }

    Map<String, Object> params = new LinkedHashMap<String, Object>();
    params.put("ping", "pong");
    params.put("kit", RequestBody.create(MediaType.parse("text/plain"), "kat"));

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
  }

  @Test public void multipartPartMapRejectsNullKeys() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Response method(@PartMap Map<String, Object> parts) {
        return null;
      }
    }

    Map<String, Object> params = new LinkedHashMap<String, Object>();
    params.put("ping", "pong");
    params.put(null, "kat");

    try {
      buildRequest(Example.class, params);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Parameter #1 part map contained null key.");
    }
  }

  @Test public void multipartNullRemovesPart() throws IOException {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Response method(@Part("ping") String ping, @Part("fizz") String fizz) {
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

  @Test public void multipartPartOptional() {
    class Example {
      @Multipart //
      @POST("/foo/bar/") //
      Response method(@Part("ping") RequestBody ping) {
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
      Response method(@Field("foo") String foo, @Field("ping") String ping) {
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
      Response method(@Field(value = "na%20me", encode = false) String foo) {
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
      Response method(@Field("foo") String foo, @Field("ping") String ping,
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
      Response method(@Field("foo") List<Object> fields, @Field("kit") String kit) {
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
      Response method(@Field("foo") Object[] fields, @Field("kit") String kit) {
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
      Response method(@Field("foo") int[] fields, @Field("kit") String kit) {
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
      Response method(@FieldMap(encode = false) Map<String, Object> fieldMap) {
        return null;
      }
    }

    Map<String, Object> fieldMap = new LinkedHashMap<String, Object>();
    fieldMap.put("k%20it", "k%20at");
    fieldMap.put("pin%20g", "po%20ng");

    Request request = buildRequest(Example.class, fieldMap);
    assertBody(request.body(), "k%20it=k%20at&pin%20g=po%20ng");
  }

  @Test public void formEncodedFieldMap() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Response method(@FieldMap Map<String, Object> fieldMap) {
        return null;
      }
    }

    Map<String, Object> fieldMap = new LinkedHashMap<String, Object>();
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
      Response method(@FieldMap Map<String, Object> a) {
        return null;
      }
    }

    Map<String, Object> fieldMap = new LinkedHashMap<String, Object>();
    fieldMap.put("kit", "kat");
    fieldMap.put("foo", null);
    fieldMap.put(null, "pong");

    try {
      buildRequest(Example.class, fieldMap);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Parameter #1 field map contained null key.");
    }
  }

  @Test public void fieldMapMustBeAMap() {
    class Example {
      @FormUrlEncoded //
      @POST("/") //
      Response method(@FieldMap List<String> a) {
        return null;
      }
    }
    try {
      buildRequest(Example.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.method: @FieldMap parameter type must be Map. (parameter #1)");
    }
  }

  @Test public void simpleHeaders() {
    class Example {
      @GET("/foo/bar/")
      @Headers({
          "ping: pong",
          "kit: kat"
      })
      Response method() {
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
      Response method(@Header("kit") BigInteger kit) {
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
      Response method(@Header("kit") String kit) {
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
      Response method(@Header("foo") List<String> kit) {
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
      Response method(@Header("foo") String[] kit) {
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
      Response method(@Body RequestBody body) {
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
      Response method() {
        return null;
      }
    }
    Request request = buildRequest(Example.class);
    assertThat(request.headers().get("Content-Type")).isEqualTo("text/not-plain");
  }

  @Test public void contentTypeParameterHeaderOverrides() {
    class Example {
      @POST("/") //
      Response method(@Header("Content-Type") String contentType, @Body RequestBody body) {
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

  private static final Converter GSON = new GsonConverter(new Gson());

  private Request buildRequest(Class<?> cls, Object... args) {
    Method method = TestingUtils.onlyMethod(cls);
    MethodInfo methodInfo = new MethodInfo(method);

    RequestBuilder builder = new RequestBuilder("http://example.com/", methodInfo, GSON);
    builder.setArguments(args);
    return builder.build();
  }
}
