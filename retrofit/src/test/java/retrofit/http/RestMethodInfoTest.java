// Copyright 2013 Square, Inc.
package retrofit.http;

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
import retrofit.http.mime.TypedOutput;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.fest.assertions.api.Assertions.assertThat;
import static retrofit.http.RestMethodInfo.NO_BODY;
import static retrofit.http.RestMethodInfo.RequestType.MULTIPART;
import static retrofit.http.RestMethodInfo.RequestType.SIMPLE;

public class RestMethodInfoTest {
  @Test public void pathParameterParsing() throws Exception {
    expectParams("/");
    expectParams("/foo");
    expectParams("foo/bar");
    expectParams("foo/bar/{}");
    expectParams("foo/bar/{taco}", "taco");
    expectParams("foo/bar/{t}", "t");
    expectParams("foo/bar/{!!!}/"); // Invalid parameter.
    expectParams("foo/bar/{}/{taco}", "taco");
    expectParams("foo/bar/{taco}/or/{burrito}", "taco", "burrito");
    expectParams("foo/bar/{taco}/or/{taco}", "taco");
    expectParams("foo/bar/{taco-shell}", "taco-shell");
    expectParams("foo/bar/{taco_shell}", "taco_shell");
    expectParams("foo/bar/{sha256}", "sha256");
    expectParams("foo/bar/{1}"); // Invalid parameter, name cannot start with digit.
  }

  private static void expectParams(String path, String... expected) {
    Set<String> calculated = RestMethodInfo.parsePathParameters(path);
    assertThat(calculated).hasSize(expected.length);
    if (expected.length > 0) {
      assertThat(calculated).containsExactly(expected);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void pathMustBePrefixedWithSlash() {
    class Example {
      @GET("foo/bar") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
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
      @GET("/foo") void a(@Path("id") String id, ResponseCallback cb) {
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
      @GET("/foo") void a(@Path("id") String id, Callback<Response> c) {
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

  @Test(expected = IllegalArgumentException.class)
  public void missingCallbackTypes() {
    class Example {
      @GET("/foo") void a(@Path("id") String id) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    new RestMethodInfo(method);
  }

  @Test(expected = IllegalArgumentException.class)
  public void synchronousWithAsyncCallback() {
    class Example {
      @GET("/foo") Response a(Callback<Response> callback) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    new RestMethodInfo(method);
  }

  @Test(expected = IllegalStateException.class)
  public void lackingMethod() {
    class Example {
      Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
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

  @Test public void singleQueryParam() {
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

    assertThat(methodInfo.requestUrlParam).isEmpty();
    assertThat(methodInfo.requestQueryName).isEmpty();
    assertThat(methodInfo.requestFormPair).isEmpty();
    assertThat(methodInfo.requestMultipartPart).isEmpty();
    assertThat(methodInfo.bodyIndex).isEqualTo(NO_BODY);
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
  }

  @Test public void singleParam() {
    class Example {
      @GET("/") Response a(@Query("a") String a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestQueryName).hasSize(1).containsSequence("a");
    assertThat(methodInfo.bodyIndex).isEqualTo(NO_BODY);
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
  }

  @Test public void multipleParams() {
    class Example {
      @GET("/") Response a(@Query("a") String a, @Query("b") String b, @Query("c") String c) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.requestQueryName).hasSize(3).containsSequence("a", "b", "c");
    assertThat(methodInfo.bodyIndex).isEqualTo(NO_BODY);
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
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

    assertThat(methodInfo.requestUrlParam).containsOnly(new String[] { null });
    assertThat(methodInfo.requestQueryName).containsOnly(new String[] { null });
    assertThat(methodInfo.requestFormPair).containsOnly(new String[] { null });
    assertThat(methodInfo.requestMultipartPart).containsOnly(new String[] { null });
    assertThat(methodInfo.bodyIndex).isEqualTo(0);
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

    assertThat(methodInfo.requestUrlParam).containsOnly(new String[] { null });
    assertThat(methodInfo.requestQueryName).containsOnly(new String[] { null });
    assertThat(methodInfo.requestFormPair).containsOnly(new String[] { null });
    assertThat(methodInfo.requestMultipartPart).containsOnly(new String[] { null });
    assertThat(methodInfo.bodyIndex).isEqualTo(0);
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
  }

  @Test(expected = IllegalStateException.class)
  public void twoBodies() {
    class Example {
      @PUT("/") Response a(@Body int o1, @Body int o2) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
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

    assertThat(methodInfo.requestUrlParam).containsExactly("a", null, "c");
    assertThat(methodInfo.requestQueryName).containsExactly(null, null, null);
    assertThat(methodInfo.requestFormPair).containsExactly(null, null, null);
    assertThat(methodInfo.requestMultipartPart).containsExactly(null, null, null);
    assertThat(methodInfo.bodyIndex).isEqualTo(1);
    assertThat(methodInfo.requestType).isEqualTo(SIMPLE);
  }

  @Test(expected = IllegalStateException.class)
  public void pathParamNonPathParamAndTypedBytes() {
    class Example {
      @PUT("/{a}") Response a(@Path("a") int a, @Path("b") int b, @Body int c) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
  }

  @Test(expected = IllegalStateException.class)
  public void parameterWithoutAnnotation() {
    class Example {
      @GET("/") Response a(String a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
  }

  @Test(expected = IllegalStateException.class)
  public void nonBodyHttpMethodWithSingleEntity() {
    class Example {
      @GET("/") Response a(@Body Object o) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
  }

  @Test(expected = IllegalStateException.class)
  public void nonBodyHttpMethodWithTypedBytes() {
    class Example {
      @GET("/") Response a(@Path("a") TypedOutput a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
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

  @Test(expected = IllegalStateException.class)
  public void implicitMultipartForbidden() {
    class Example {
      @POST("/") Response a(@Part("a") int a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
  }

  @Test(expected = IllegalStateException.class)
  public void multipartFailsOnNonBodyMethod() {
    class Example {
      @Multipart @GET("/") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
  }

  @Test(expected = IllegalStateException.class)
  public void multipartFailsWithNoParts() {
    class Example {
      @Multipart @POST("/") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
  }

  @Test(expected = IllegalStateException.class)
  public void implicitFormEncodingForbidden() {
    class Example {
      @POST("/") Response a(@Field("a") int a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
  }

  @Test(expected = IllegalStateException.class)
  public void formEncodingFailsOnNonBodyMethod() {
    class Example {
      @FormUrlEncoded @GET("/") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
  }

  @Test(expected = IllegalStateException.class)
  public void formEncodingFailsWithNoParts() {
    class Example {
      @FormUrlEncoded @POST("/") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
  }

  @Test(expected = IllegalStateException.class)
  public void headersFailWhenEmptyOnMethod() {
    class Example {
      @GET("/") @Headers({}) Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
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
        Arrays.asList(new retrofit.http.client.Header("X-Foo", "Bar"),
            new retrofit.http.client.Header("X-Ping", "Pong")));
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

    assertThat(Arrays.asList(methodInfo.requestParamHeader))
      .isEqualTo(Arrays.asList("a", "b"));
  }

  @Test(expected = IllegalStateException.class)
  public void headerParamMustBeString() {
    class Example {
      @GET("/")
      Response a(@Header("a") TypedOutput a, @Header("b") int b) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
  }

  @Test(expected = IllegalStateException.class)
  public void onlyOneEncodingIsAllowed() {
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
    methodInfo.init();
  }

  private static class Response {
  }

  private static interface ResponseCallback extends Callback<Response> {
  }

  private static interface MultimapCallback<K, V> extends Callback<Map<? extends K, V[]>> {
  }
}
