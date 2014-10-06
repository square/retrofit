// Copyright 2013 Square, Inc.
package retrofit;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Query;
import retrofit.http.Streaming;
import rx.Observable;

import static org.assertj.core.api.Assertions.assertThat;

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

  @Test public void concreteCallbackTypes() {
    class Example {
      @GET("/foo") void a(ResponseCallback cb) {
      }
    }

    Method method = TestingUtils.onlyMethod(Example.class);
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.responseObjectType).isEqualTo(Response.class);
  }

  @Test public void concreteCallbackTypesWithParams() {
    class Example {
      @GET("/foo") void a(@Query("id") String id, ResponseCallback cb) {
      }
    }

    Method method = TestingUtils.onlyMethod(Example.class);
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.responseObjectType).isEqualTo(Response.class);
  }

  @Test public void genericCallbackTypes() {
    class Example {
      @GET("/foo") void a(Callback<Response> cb) {
      }
    }

    Method method = TestingUtils.onlyMethod(Example.class);
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.responseObjectType).isEqualTo(Response.class);
  }

  @Test public void genericCallbackTypesWithParams() {
    class Example {
      @GET("/foo") void a(@Query("id") String id, Callback<Response> c) {
      }
    }

    Method method = TestingUtils.onlyMethod(Example.class);
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.responseObjectType).isEqualTo(Response.class);
  }

  @Test public void wildcardGenericCallbackTypes() {
    class Example {
      @GET("/foo") void a(Callback<? extends Response> c) {
      }
    }

    Method method = TestingUtils.onlyMethod(Example.class);
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.responseObjectType).isEqualTo(Response.class);
  }

  @Test public void genericCallbackWithGenericType() {
    class Example {
      @GET("/foo") void a(Callback<List<String>> c) {
      }
    }

    Method method = TestingUtils.onlyMethod(Example.class);
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

    Method method = TestingUtils.onlyMethod(Example.class);
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

    Method method = TestingUtils.onlyMethod(Example.class);
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

    Method method = TestingUtils.onlyMethod(Example.class);
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

    Method method = TestingUtils.onlyMethod(Example.class);
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

    Method method = TestingUtils.onlyMethod(Example.class);
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    methodInfo.init();

    assertThat(methodInfo.isStreaming).isTrue();
    assertThat(methodInfo.responseObjectType).isEqualTo(Response.class);
  }

  @Test public void observableResponse() {
    class Example {
      @GET("/foo") Observable<Response> a() {
        return null;
      }
    }

    Method method = TestingUtils.onlyMethod(Example.class);
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

    Method method = TestingUtils.onlyMethod(Example.class);
    RestMethodInfo methodInfo = new RestMethodInfo(method);
    assertThat(methodInfo.isSynchronous).isFalse();
    assertThat(methodInfo.isObservable).isTrue();
    Type expected = new TypeToken<List<String>>() {}.getType();
    assertThat(methodInfo.responseObjectType).isEqualTo(expected);
  }

  private static interface ResponseCallback extends Callback<Response> {
  }

  private static interface MultimapCallback<K, V> extends Callback<Map<? extends K, V[]>> {
  }
}
