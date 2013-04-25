// Copyright 2013 Square, Inc.
package retrofit.http;

import com.google.gson.reflect.TypeToken;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import retrofit.http.mime.TypedOutput;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.fest.assertions.api.Assertions.assertThat;
import static retrofit.http.RestMethodInfo.NO_BASE_URL;
import static retrofit.http.RestMethodInfo.NO_SINGLE_ENTITY;

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
    assertThat(methodInfo.type).isEqualTo(Response.class);
  }

  @Test public void concreteCallbackTypesWithParams() {
    class Example {
      @GET("/foo") void a(@Name("id") String id, ResponseCallback cb) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.type).isEqualTo(Response.class);
  }

  @Test public void genericCallbackTypes() {
    class Example {
      @GET("/foo") void a(Callback<Response> cb) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.type).isEqualTo(Response.class);
  }

  @Test public void genericCallbackTypesWithParams() {
    class Example {
      @GET("/foo") void a(@Name("id") String id, Callback<Response> c) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.type).isEqualTo(Response.class);
  }

  @Test public void wildcardGenericCallbackTypes() {
    class Example {
      @GET("/foo") void a(Callback<? extends Response> c) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.type).isEqualTo(Response.class);
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
    assertThat(methodInfo.type).isEqualTo(expected);
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
    assertThat(methodInfo.type).isEqualTo(
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
    assertThat(methodInfo.type).isEqualTo(Response.class);
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
    assertThat(methodInfo.type).isEqualTo(expected);
  }

  @Test(expected = IllegalArgumentException.class)
  public void missingCallbackTypes() {
    class Example {
      @GET("/foo") void a(@Name("id") String id) {
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

    assertThat(methodInfo.restMethod.value()).isEqualTo("DELETE");
    assertThat(methodInfo.restMethod.hasBody()).isFalse();
    assertThat(methodInfo.path).isEqualTo("/foo");
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

    assertThat(methodInfo.restMethod.value()).isEqualTo("GET");
    assertThat(methodInfo.restMethod.hasBody()).isFalse();
    assertThat(methodInfo.path).isEqualTo("/foo");
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

    assertThat(methodInfo.restMethod.value()).isEqualTo("HEAD");
    assertThat(methodInfo.restMethod.hasBody()).isFalse();
    assertThat(methodInfo.path).isEqualTo("/foo");
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

    assertThat(methodInfo.restMethod.value()).isEqualTo("POST");
    assertThat(methodInfo.restMethod.hasBody()).isTrue();
    assertThat(methodInfo.path).isEqualTo("/foo");
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

    assertThat(methodInfo.restMethod.value()).isEqualTo("PUT");
    assertThat(methodInfo.restMethod.hasBody()).isTrue();
    assertThat(methodInfo.path).isEqualTo("/foo");
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

    assertThat(methodInfo.restMethod.value()).isEqualTo("CUSTOM1");
    assertThat(methodInfo.restMethod.hasBody()).isFalse();
    assertThat(methodInfo.path).isEqualTo("/foo");
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

    assertThat(methodInfo.restMethod.value()).isEqualTo("CUSTOM2");
    assertThat(methodInfo.restMethod.hasBody()).isTrue();
    assertThat(methodInfo.path).isEqualTo("/foo");
  }

  @Test public void singleQueryParam() {
    class Example {
      @GET("/foo")
      @QueryParam(name = "a", value = "b")
      Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.pathQueryParams).hasSize(1);
    QueryParam param = methodInfo.pathQueryParams[0];
    assertThat(param.name()).isEqualTo("a");
    assertThat(param.value()).isEqualTo("b");
  }

  @Test public void multipleQueryParam() {
    class Example {
      @GET("/foo")
      @QueryParams({
          @QueryParam(name = "a", value = "b"),
          @QueryParam(name = "c", value = "d")
      })
      Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.pathQueryParams).hasSize(2);
    QueryParam param1 = methodInfo.pathQueryParams[0];
    assertThat(param1.name()).isEqualTo("a");
    assertThat(param1.value()).isEqualTo("b");
    QueryParam param2 = methodInfo.pathQueryParams[1];
    assertThat(param2.name()).isEqualTo("c");
    assertThat(param2.value()).isEqualTo("d");
  }

  @Test(expected = IllegalStateException.class)
  public void bothQueryParamAnnotations() {
    class Example {
      @GET("/foo")
      @QueryParam(name = "a", value = "b")
      @QueryParams({
          @QueryParam(name = "a", value = "b"),
          @QueryParam(name = "c", value = "d")
      })
      Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
  }

  @Test(expected = IllegalStateException.class)
  public void emptyQueryParams() {
    class Example {
      @GET("/foo")
      @QueryParams({})
      Response a() {
        return null;
      }
    }
    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
  }

  @Test public void noQueryParamsNonNull() {
    class Example {
      @GET("/") Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.pathQueryParams).isEmpty();
    assertThat(methodInfo.isMultipart).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  public void noQueryParamsInUrl() {
    class Example {
      @GET("/foo/{bar}/")
      @QueryParam(name = "bar", value = "baz")
      Response a() {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
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

    assertThat(methodInfo.namedParams).isEmpty();
    assertThat(methodInfo.singleEntityArgumentIndex).isEqualTo(NO_SINGLE_ENTITY);
    assertThat(methodInfo.isMultipart).isFalse();
  }

  @Test public void singleParam() {
    class Example {
      @GET("/") Response a(@Name("a") String a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.namedParams).hasSize(1).containsSequence("a");
    assertThat(methodInfo.singleEntityArgumentIndex).isEqualTo(NO_SINGLE_ENTITY);
    assertThat(methodInfo.isMultipart).isFalse();
  }

  @Test public void multipleParams() {
    class Example {
      @GET("/") Response a(@Name("a") String a, @Name("b") String b, @Name("c") String c) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.namedParams).hasSize(3).containsSequence("a", "b", "c");
    assertThat(methodInfo.singleEntityArgumentIndex).isEqualTo(NO_SINGLE_ENTITY);
    assertThat(methodInfo.isMultipart).isFalse();
  }

  @Test public void emptyParamsWithCallback() {
    class Example {
      @GET("/") void a(ResponseCallback cb) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.namedParams).isEmpty();
    assertThat(methodInfo.singleEntityArgumentIndex).isEqualTo(NO_SINGLE_ENTITY);
    assertThat(methodInfo.isMultipart).isFalse();
  }

  @Test public void singleParamWithCallback() {
    class Example {
      @GET("/") void a(@Name("a") String a, ResponseCallback cb) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.namedParams).hasSize(1).containsSequence("a");
    assertThat(methodInfo.singleEntityArgumentIndex).isEqualTo(NO_SINGLE_ENTITY);
    assertThat(methodInfo.isMultipart).isFalse();
  }

  @Test public void multipleParamsWithCallback() {
    class Example {
      @GET("/") void a(@Name("a") String a, @Name("b") String b, ResponseCallback cb) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.namedParams).hasSize(2).containsSequence("a", "b");
    assertThat(methodInfo.singleEntityArgumentIndex).isEqualTo(NO_SINGLE_ENTITY);
    assertThat(methodInfo.isMultipart).isFalse();
  }

  @Test public void singleEntity() {
    class Example {
      @PUT("/") Response a(@SingleEntity Object o) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.namedParams).hasSize(1);
    assertThat(methodInfo.namedParams[0]).isNull();
    assertThat(methodInfo.singleEntityArgumentIndex).isEqualTo(0);
    assertThat(methodInfo.isMultipart).isFalse();
  }

  @Test public void singleEntityTypedBytes() {
    class Example {
      @PUT("/") Response a(@SingleEntity TypedOutput o) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.namedParams).hasSize(1);
    assertThat(methodInfo.namedParams[0]).isNull();
    assertThat(methodInfo.singleEntityArgumentIndex).isEqualTo(0);
    assertThat(methodInfo.isMultipart).isFalse();
  }

  @Test public void singleEntityWithCallback() {
    class Example {
      @PUT("/") void a(@SingleEntity Object o, ResponseCallback cb) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.namedParams).hasSize(1);
    assertThat(methodInfo.namedParams[0]).isNull();
    assertThat(methodInfo.singleEntityArgumentIndex).isEqualTo(0);
    assertThat(methodInfo.isMultipart).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  public void twoSingleEntities() {
    class Example {
      @PUT("/") Response a(@SingleEntity int o1, @SingleEntity int o2) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
  }

  @Test public void singleEntityWithNamed() {
    class Example {
      @PUT("/{a}/{c}") Response a(@Name("a") int a, @SingleEntity int b, @Name("c") int c) {
        return null;
      }
    }
    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.namedParams).hasSize(3).containsSequence("a", null, "c");
    assertThat(methodInfo.singleEntityArgumentIndex).isEqualTo(1);
    assertThat(methodInfo.isMultipart).isFalse();
  }

  @Test public void singleEntityWithNamedAndCallback() {
    class Example {
      @PUT("/{a}") void a(@Name("a") int a, @SingleEntity int b, ResponseCallback cb) {
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.namedParams).hasSize(2).containsSequence("a", null);
    assertThat(methodInfo.singleEntityArgumentIndex).isEqualTo(1);
    assertThat(methodInfo.isMultipart).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  public void nonPathParamAndSingleEntity() {
    class Example {
      @PUT("/") Response a(@Name("a") int a, @SingleEntity int b) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
  }

  @Test(expected = IllegalStateException.class)
  public void typedBytesUrlParam() {
    class Example {
      @GET("/{a}") Response a(@Name("a") TypedOutput m) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();
  }

  @Test(expected = IllegalStateException.class)
  public void pathParamNonPathParamAndTypedBytes() {
    class Example {
      @PUT("/{a}") Response a(@Name("a") int a, @Name("b") int b, @SingleEntity int c) {
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
      @GET("/") Response a(@SingleEntity Object o) {
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
      @GET("/") Response a(@Name("a") TypedOutput a) {
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
      Response a(@Name("a") TypedOutput a) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.isMultipart).isTrue();
  }

  @Test public void twoTypedBytesMultipart() {
    class Example {
      @Multipart @PUT("/")
      Response a(@Name("a") TypedOutput a, @Name("b") TypedOutput b) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.isMultipart).isTrue();
  }

  @Test public void twoTypesMultipart() {
    class Example {
      @Multipart @PUT("/")
      Response a(@Name("a") TypedOutput a, @Name("b") int b) {
        return null;
      }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.isMultipart).isTrue();
  }

  @Test(expected = IllegalStateException.class)
  public void implicitMultipartForbidden() {
    class Example {
      @POST("/") Response a(@Name("a") int a) {
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

  @Test public void singleBaseURl() {
    class Example {
        @GET("/") Response a(@BaseUrl String a) {
            return null;
        }
    }

    Method method = TestingUtils.getMethod(Example.class, "a");
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.namedParams).hasSize(1);
    assertThat(methodInfo.singleEntityArgumentIndex).isEqualTo(NO_SINGLE_ENTITY);
    assertThat(methodInfo.baseUrlArgumentIndex).isNotEqualTo(NO_BASE_URL);
    assertThat(methodInfo.baseUrlArgumentIndex).isEqualTo(0);
    assertThat(methodInfo.isMultipart).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  public void singleBaseURlNonString() {
    class Example {
        @GET("/") Response a(@BaseUrl int a) {
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
