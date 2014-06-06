// Copyright 2013 Square, Inc.
package retrofit;

import com.google.gson.reflect.TypeToken;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.EncodedPath;
import retrofit.http.EncodedQuery;
import retrofit.http.EncodedQueryMap;
import retrofit.http.Field;
import retrofit.http.FieldMap;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.HEAD;
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
import retrofit.http.RestMethod;
import retrofit.http.Streaming;
import retrofit.mime.TypedOutput;
import rx.Observable;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static retrofit.RestMethodInfo.ParamUsage.BODY;
import static retrofit.RestMethodInfo.ParamUsage.ENCODED_PATH;
import static retrofit.RestMethodInfo.ParamUsage.ENCODED_QUERY;
import static retrofit.RestMethodInfo.ParamUsage.ENCODED_QUERY_MAP;
import static retrofit.RestMethodInfo.ParamUsage.FIELD;
import static retrofit.RestMethodInfo.ParamUsage.FIELD_MAP;
import static retrofit.RestMethodInfo.ParamUsage.HEADER;
import static retrofit.RestMethodInfo.ParamUsage.PATH;
import static retrofit.RestMethodInfo.ParamUsage.QUERY;
import static retrofit.RestMethodInfo.ParamUsage.QUERY_MAP;
import static retrofit.RestMethodInfo.RequestType.FORM_URL_ENCODED;
import static retrofit.RestMethodInfo.RequestType.MULTIPART;
import static retrofit.RestMethodInfo.RequestType.SIMPLE;

@SuppressWarnings("unused") // Lots of unused parameters for example code.
public class RestMethodInfoTest {
  @Test public void pathParameterParsing() throws Exception {
    expectParams("/");
    expectParams("/foo");
    expectParams("/foo/bar");
    expectParams("/foo/bar/{}");
    expectParams("/foo/bar/{taco}", "taco");
    expectParams("/foo/bar/{t}", "t");
    expectParams("/foo/bar/{!!!}/"); // Invalid parameter.
    expectParams("/foo/bar/{}/{taco}", "taco");
    expectParams("/foo/bar/{taco}/or/{burrito}", "taco", "burrito");
    expectParams("/foo/bar/{taco}/or/{taco}", "taco");
    expectParams("/foo/bar/{taco-shell}", "taco-shell");
    expectParams("/foo/bar/{taco_shell}", "taco_shell");
    expectParams("/foo/bar/{sha256}", "sha256");
    expectParams("/foo/bar/{TACO}", "TACO");
    expectParams("/foo/bar/{taco}/{tAco}/{taCo}", "taco", "tAco", "taCo");
    expectParams("/foo/bar/{1}"); // Invalid parameter, name cannot start with digit.
  }

  private static void expectParams(String path, String... expected) {
    Set<String> calculated = RestMethodInfo.parsePathParameters(path);
    assertThat(calculated).hasSize(expected.length);
    if (expected.length > 0) {
      assertThat(calculated).containsExactly(expected);
    }
  }

  @Test public void pathMustBePrefixedWithSlash() {
    class Example {
      @GET("foo/bar") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.a: URL path \"foo/bar\" must start with '/'.");
    }
  }

  @Test public void concreteCallbackTypes() {
    class Example {
      @GET("/foo") void a(ResponseCallback cb) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.responseObjectType).isEqualTo(Response.class);
  }

  @Test public void concreteCallbackTypesWithParams() {
    class Example {
      @GET("/foo") void a(@Query("id") String id, ResponseCallback cb) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.responseObjectType).isEqualTo(Response.class);
  }

  @Test public void genericCallbackTypes() {
    class Example {
      @GET("/foo") void a(Callback<Response> cb) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.responseObjectType).isEqualTo(Response.class);
  }

  @Test public void genericCallbackTypesWithParams() {
    class Example {
      @GET("/foo") void a(@Query("id") String id, Callback<Response> c) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.responseObjectType).isEqualTo(Response.class);
  }

  @Test public void wildcardGenericCallbackTypes() {
    class Example {
      @GET("/foo") void a(Callback<? extends Response> c) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.responseObjectType).isEqualTo(Response.class);
  }

  @Test public void genericCallbackWithGenericType() {
    class Example {
      @GET("/foo") void a(Callback<List<String>> c) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();

    Type expected = new TypeToken<List<String>>() {}.getType();
    assertThat(methodInfo.responseObjectType).isEqualTo(expected);
  }

  // RestMethodInfo reconstructs this type from MultimapCallback<String, Set<Long>>. It contains
  // a little of everything: a parameterized type, a generic array, and a wildcard.
  private static Map<? extends String, Set<Long>[]> extendingGenericCallbackType;

  @Test public void extendingGenericCallback() throws Exception {
    class Example {
      @GET("/foo") void a(MultimapCallback<String, Set<Long>> callback) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.responseObjectType).isEqualTo(
        RestMethodInfoTest.class.getDeclaredField("extendingGenericCallbackType").getGenericType());
  }

  @Test public void synchronousResponse() {
    class Example {
      @GET("/foo") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isTrue();
    assertThat(methodInfo.responseObjectType).isEqualTo(Response.class);
  }

  @Test public void synchronousGenericResponse() {
    class Example {
      @GET("/foo") List<String> a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isTrue();

    Type expected = new TypeToken<List<String>>() {}.getType();
    assertThat(methodInfo.responseObjectType).isEqualTo(expected);
  }

  @Test public void streamingResponse() {
    class Example {
      @GET("/foo") @Streaming Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.isStreaming).isTrue();
    assertThat(methodInfo.responseObjectType).isEqualTo(Response.class);
  }

  @Test public void streamingResponseWithCallback() {
    class Example {
      @GET("/foo") @Streaming void a(Callback<Response> callback) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.isStreaming).isTrue();
    assertThat(methodInfo.responseObjectType).isEqualTo(Response.class);
  }

  @Test public void streamingResponseNotAllowed() {
    class Example {
      @GET("/foo") @Streaming String a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: Only methods having Response as data type are allowed to have @Streaming annotation.");
    }
  }

  @Test public void streamingResponseWithCallbackNotAllowed() {
    class Example {
      @GET("/foo") @Streaming void a(Callback<String> callback) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: Only methods having Response as data type are allowed to have @Streaming annotation.");
    }
  }

  @Test public void observableResponse() {
    class Example {
      @GET("/foo") Observable<Response> a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.isObservable).isTrue();
    assertThat(methodInfo.responseObjectType).isEqualTo(Response.class);
  }

  @Test public void observableGenericResponse() {
    class Example {
      @GET("/foo") Observable<List<String>> a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.isObservable).isTrue();
    Type expected = new TypeToken<List<String>>() {}.getType();
    assertThat(methodInfo.responseObjectType).isEqualTo(expected);
  }

  @Test public void observableWithCallback() {
    class Example {
      @GET("/foo") Observable<Response> a(Callback<Response> callback) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    try {
      new RestMethodInfo(method);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: Must have return type or Callback as last argument, not both.");
    }
  }

  @Test public void missingCallbackTypes() {
    class Example {
      @GET("/foo") void a(@Query("id") String id) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    try {
      new RestMethodInfo(method);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: Must have either a return type or Callback as last argument.");
    }
  }

  @Test public void nonParameterizedCallbackFails() {
    class Example {
      @GET("/foo") void a(Callback cb) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    try {
      new RestMethodInfo(method);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: Last parameter must be of type Callback<X> or Callback<? super X>.");
    }
  }

  @Test public void synchronousWithAsyncCallback() {
    class Example {
      @GET("/foo") Response a(Callback<Response> callback) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    try {
      new RestMethodInfo(method);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: Must have return type or Callback as last argument, not both.");
    }
  }

  @Test
  public void lackingMethod() {
    class Example {
      Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: HTTP method annotation is required (e.g., @GET, @POST, etc.).");
    }
  }

  @Test public void deleteMethod() {
    class Example {
      @DELETE("/foo") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestMethod).isEqualTo("DELETE");
    assertThat(methodInfo.requestHasBody).isFalse();
    assertThat(methodInfo.requestUrl).isEqualTo("/foo");
  }

  @Test public void getMethod() {
    class Example {
      @GET("/foo") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestMethod).isEqualTo("GET");
    assertThat(methodInfo.requestHasBody).isFalse();
    assertThat(methodInfo.requestUrl).isEqualTo("/foo");
  }

  @Test public void headMethod() {
    class Example {
      @HEAD("/foo") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestMethod).isEqualTo("HEAD");
    assertThat(methodInfo.requestHasBody).isFalse();
    assertThat(methodInfo.requestUrl).isEqualTo("/foo");
  }

  @Test public void postMethod() {
    class Example {
      @POST("/foo") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestMethod).isEqualTo("POST");
    assertThat(methodInfo.requestHasBody).isTrue();
    assertThat(methodInfo.requestUrl).isEqualTo("/foo");
  }

  @Test public void putMethod() {
    class Example {
      @PUT("/foo") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestMethod).isEqualTo("PUT");
    assertThat(methodInfo.requestHasBody).isTrue();
    assertThat(methodInfo.requestUrl).isEqualTo("/foo");
  }

  @Test public void patchMethod() {
    class Example {
      @PATCH("/foo") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestMethod).isEqualTo("PATCH");
    assertThat(methodInfo.requestHasBody).isTrue();
    assertThat(methodInfo.requestUrl).isEqualTo("/foo");
  }

  @Test public void twoMethodsFail() {
    class Example {
      @PATCH("/foo") @POST("/foo") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);

    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: Only one HTTP method is allowed. Found: PATCH and POST.");
    }
  }

  @RestMethod("BAD")
  @Target(METHOD) @Retention(RUNTIME)
  private @interface BAD_CUSTOM {
    int value();
  }

  @Test public void customWithoutRestMethod() {
    class Example {
      @BAD_CUSTOM(12) Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);

    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: Failed to extract String 'value' from @BAD_CUSTOM annotation.");
    }
  }

  @RestMethod("CUSTOM1")
  @Target(METHOD) @Retention(RUNTIME)
  private @interface CUSTOM1 {
    String value();
  }

  @Test public void custom1Method() {
    class Example {
      @CUSTOM1("/foo") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestMethod).isEqualTo("CUSTOM1");
    assertThat(methodInfo.requestHasBody).isFalse();
    assertThat(methodInfo.requestUrl).isEqualTo("/foo");
  }

  @RestMethod(value = "CUSTOM2", hasBody = true)
  @Target(METHOD) @Retention(RUNTIME)
  private @interface CUSTOM2 {
    String value();
  }

  @Test public void custom2Method() {
    class Example {
      @CUSTOM2("/foo") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestMethod).isEqualTo("CUSTOM2");
    assertThat(methodInfo.requestHasBody).isTrue();
    assertThat(methodInfo.requestUrl).isEqualTo("/foo");
  }

  @Test public void singlePathQueryParam() {
    class Example {
      @GET("/foo?a=b")
      Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestUrl).isEqualTo("/foo");
    assertThat(methodInfo.requestQuery).isEqualTo("a=b");
  }

  @Test public void emptyParams() {
    class Example {
      @GET("/") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestParamNames).isEmpty();
    assertThat(methodInfo.requestParamUsage).isEmpty();
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
  }

  @Test public void singlePathParam() {
    class Example {
      @GET("/{a}") Response a(@Path("a") String a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestParamNames).hasSize(1).containsExactly("a");
    assertThat(methodInfo.requestParamUsage).hasSize(1).containsExactly(PATH);
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
  }

  @Test public void singleEncodedPathParam() {
    class Example {
      @GET("/{a}") Response a(@EncodedPath("a") String a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestParamNames).hasSize(1).containsExactly("a");
    assertThat(methodInfo.requestParamUsage).hasSize(1).containsExactly(ENCODED_PATH);
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
  }

  @Test public void singleQueryParam() {
    class Example {
      @GET("/") Response a(@Query("a") String a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestParamNames).hasSize(1).containsExactly("a");
    assertThat(methodInfo.requestParamUsage).hasSize(1).containsExactly(QUERY);
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
  }

  @Test public void singleEncodedQueryParam() {
    class Example {
      @GET("/") Response a(@EncodedQuery("a") String a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestParamNames).hasSize(1).containsExactly("a");
    assertThat(methodInfo.requestParamUsage).hasSize(1).containsExactly(ENCODED_QUERY);
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
  }

  @Test public void multipleQueryParams() {
    class Example {
      @GET("/") Response a(@Query("a") String a, @Query("b") String b, @Query("c") String c) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestParamNames).hasSize(3).containsExactly("a", "b", "c");
    assertThat(methodInfo.requestParamUsage).hasSize(3).containsExactly(QUERY, QUERY, QUERY);
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
  }

  @Test public void queryMap() {
    class Example {
      @GET("/") Response a(@QueryMap Map<String, String> a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestParamNames).hasSize(1).containsNull();
    assertThat(methodInfo.requestParamUsage).hasSize(1).containsExactly(QUERY_MAP);
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
  }

  @Test public void queryMapMustBeAMap() {
    class Example {
      @GET("/") Response a(@QueryMap List<String> a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.a: @QueryMap parameter type must be Map. (parameter #1)");
    }
  }

  @Test public void encodedQueryMap() {
    class Example {
      @GET("/") Response a(@EncodedQueryMap Map<String, String> a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestParamNames).hasSize(1).containsNull();
    assertThat(methodInfo.requestParamUsage).hasSize(1).containsExactly(ENCODED_QUERY_MAP);
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
  }

  @Test public void encodedQueryMapMustBeAMap() {
    class Example {
      @GET("/") Response a(@EncodedQueryMap List<String> a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: @EncodedQueryMap parameter type must be Map. (parameter #1)");
    }
  }

  @Test public void fieldMap() {
    class Example {
      @FormUrlEncoded @POST("/") Response a(@FieldMap Map<String, String> a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestParamNames).hasSize(1).containsNull();
    assertThat(methodInfo.requestParamUsage).hasSize(1).containsExactly(FIELD_MAP);
    assertThat(methodInfo.requestType).isEqualTo(FORM_URL_ENCODED);
  }

  @Test public void fieldMapMustBeAMap() {
    class Example {
      @FormUrlEncoded @POST("/") Response a(@FieldMap List<String> a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.a: @FieldMap parameter type must be Map. (parameter #1)");
    }
  }

  @Test public void bodyObject() {
    class Example {
      @PUT("/") Response a(@Body Object o) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestParamNames).hasSize(1).containsExactly(new String[] { null });
    assertThat(methodInfo.requestParamUsage).hasSize(1).containsExactly(BODY);
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
  }

  @Test public void bodyTypedBytes() {
    class Example {
      @PUT("/") Response a(@Body TypedOutput o) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestParamNames).hasSize(1).containsExactly(new String[] { null });
    assertThat(methodInfo.requestParamUsage).hasSize(1).containsExactly(BODY);
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
  }

  @Test public void twoBodies() {
    class Example {
      @PUT("/") Response a(@Body int o1, @Body int o2) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.a: Multiple @Body method annotations found.");
    }
  }

  @Test public void bodyInNonBodyRequest() {
    class Example {
      @Multipart
      @PUT("/") Response a(@Part("one") int o1, @Body int o2) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: @Body parameters cannot be used with form or multi-part encoding. (parameter #2)");
    }
  }

  @Test public void bodyWithOtherParams() {
    class Example {
      @PUT("/{a}/{c}") Response a(@Path("a") int a, @Body int b, @Path("c") int c) {
        return null;
      }
    }
    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestParamNames).containsExactly("a", null, "c");
    assertThat(methodInfo.requestParamUsage).containsExactly(PATH, BODY, PATH);
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
  }

  @Test public void pathParamNonPathParamAndTypedBytes() {
    class Example {
      @PUT("/{a}") Response a(@Path("a") int a, @Path("b") int b, @Body int c) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.a: URL \"/{a}\" does not contain \"{b}\". (parameter #2)");
    }
  }

  @Test public void parameterWithoutAnnotation() {
    class Example {
      @GET("/") Response a(String a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.a: No Retrofit annotation found. (parameter #1)");
    }
  }

  @Test public void nonBodyHttpMethodWithSingleEntity() {
    class Example {
      @GET("/") Response a(@Body Object o) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: Non-body HTTP method cannot contain @Body or @TypedOutput.");
    }
  }

  @Test public void simpleMultipart() {
    class Example {
      @Multipart @PUT("/")
      Response a(@Part("a") TypedOutput a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestType).isEqualTo(MULTIPART);
  }

  @Test public void twoTypedBytesMultipart() {
    class Example {
      @Multipart @PUT("/")
      Response a(@Part("a") TypedOutput a, @Part("b") TypedOutput b) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestType).isEqualTo(MULTIPART);
  }

  @Test public void twoTypesMultipart() {
    class Example {
      @Multipart @PUT("/")
      Response a(@Part("a") TypedOutput a, @Part("b") int b) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestType).isEqualTo(MULTIPART);
  }

  @Test public void partMapMultipart() {
    class Example {
      @Multipart @PUT("/")
      Response a(@Part("a") TypedOutput a, @PartMap Map<String, String> b) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestType).isEqualTo(MULTIPART);
  }

  @Test public void implicitMultipartForbidden() {
    class Example {
      @POST("/") Response a(@Part("a") int a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: @Part parameters can only be used with multipart encoding. (parameter #1)");
    }
  }

  @Test public void implicitMultipartWithPartMapForbidden() {
    class Example {
      @POST("/") Response a(@PartMap Map<String, String> params) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: @PartMap parameters can only be used with multipart encoding. (parameter #1)");
    }
  }

  @Test public void multipartFailsOnNonBodyMethod() {
    class Example {
      @Multipart @GET("/") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
    }
  }

  @Test public void multipartFailsWithNoParts() {
    class Example {
      @Multipart @POST("/") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.a: Multipart method must contain at least one @Part.");
    }
  }

  @Test public void implicitFormEncodingByFieldForbidden() {
    class Example {
      @POST("/") Response a(@Field("a") int a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: @Field parameters can only be used with form encoding. (parameter #1)");
    }
  }

  @Test public void implicitFormEncodingByFieldMapForbidden() {
    class Example {
      @POST("/") Response a(@FieldMap Map<String, String> a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: @FieldMap parameters can only be used with form encoding. (parameter #1)");
    }
  }

  @Test public void formEncodingFailsOnNonBodyMethod() {
    class Example {
      @FormUrlEncoded @GET("/") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: FormUrlEncoded can only be specified on HTTP methods with request body (e.g., @POST).");
    }
  }

  @Test public void formEncodingFailsWithNoParts() {
    class Example {
      @FormUrlEncoded @POST("/") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.a: Form-encoded method must contain at least one @Field.");
    }
  }

  @Test public void simpleFormEncoding() {
    class Example {
      @FormUrlEncoded @PUT("/")
      Response a(@Field("a") String a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestType).isEqualTo(FORM_URL_ENCODED);
    assertThat(methodInfo.requestParamUsage).containsExactly(FIELD);
  }

  @Test public void headersFailWhenEmptyOnMethod() {
    class Example {
      @GET("/") @Headers({}) Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.a: @Headers annotation is empty.");
    }
  }

  @Test public void headersFailWhenMalformed() {
    class Example {
      @GET("/") @Headers("Malformed") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: @Headers value must be in the form \"Name: Value\". Found: \"Malformed\"");
    }
  }

  @Test public void twoMethodHeaders() {

    class Example {
      @GET("/") @Headers({
        "X-Foo: Bar",
        "X-Ping: Pong"
      }) Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.headers).isEqualTo(
        Arrays.asList(new retrofit.client.Header("X-Foo", "Bar"),
            new retrofit.client.Header("X-Ping", "Pong")));
  }

  @Test public void twoHeaderParams() {
    class Example {
      @GET("/")
      Response a(@Header("a") String a, @Header("b") String b) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestParamNames).containsExactly("a", "b");
    assertThat(methodInfo.requestParamUsage).containsExactly(HEADER, HEADER);
  }

  @Test public void headerParamMustBeString() {
    class Example {
      @GET("/")
      Response a(@Header("a") TypedOutput a, @Header("b") int b) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: @Header parameter type must be String. Found: TypedOutput. (parameter #1)");
    }
  }

  @Test public void onlyOneEncodingIsAllowedMultipartFirst() {
    class Example {
      @Multipart
      @FormUrlEncoded
      @POST("/")
      Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.a: Only one encoding annotation is allowed.");
    }
  }

  @Test public void onlyOneEncodingIsAllowedFormEncodingFirst() {
    class Example {
      @FormUrlEncoded
      @Multipart
      @POST("/")
      Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Example.a: Only one encoding annotation is allowed.");
    }
  }

  @Test public void invalidPathParam() throws Exception {
    class Example {
      @GET("/") Response a(@Path("hey!") String thing) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: @Path parameter name must match \\{([a-zA-Z][a-zA-Z0-9_-]*)\\}. Found: hey! (parameter #1)");
    }
  }

  @Test public void pathParamNotAllowedInQuery() throws Exception {
    class Example {
      @GET("/foo?bar={bar}") Response a(@Path("bar") String thing) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    try {
      methodInfo.init();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Example.a: URL query string \"bar={bar}\" must not have replace block.");
    }
  }

  private static interface ResponseCallback extends Callback<Response> {
  }

  private static interface MultimapCallback<K, V> extends Callback<Map<? extends K, V[]>> {
  }
}
