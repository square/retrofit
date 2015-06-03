// Copyright 2013 Square, Inc.
package retrofit;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import org.junit.Test;
import retrofit.http.Body;
import retrofit.http.POST;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unused") // Lots of unused parameters for example code.
public final class MethodInfoTest {
  private static CallAdapter.Factory FACTORY =
      new DefaultCallAdapterFactory(Executors.newSingleThreadExecutor());
  private static Converter CONVERTER = new StringConverter();

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
    Set<String> calculated = MethodInfo.parsePathParameters(path);
    assertThat(calculated).hasSize(expected.length);
    if (expected.length > 0) {
      assertThat(calculated).containsExactly(expected);
    }
  }

  static class Dummy {
  }

  @Test public void concreteBodyType() {
    class Example {
      @POST("/foo") Call<Object> a(@Body Dummy body) {
        return null;
      }
    }

    Method method = TestingUtils.onlyMethod(Example.class);
    MethodInfo methodInfo = new MethodInfo(method, FACTORY, CONVERTER);
    assertThat(methodInfo.requestType).isEqualTo(Dummy.class);
  }

  @Test public void genericBodyType() {
    class Example {
      @POST("/foo") Call<Object> a(@Body List<Dummy> body) {
        return null;
      }
    }

    Method method = TestingUtils.onlyMethod(Example.class);
    MethodInfo methodInfo = new MethodInfo(method, FACTORY, CONVERTER);
    Type expected = new TypeToken<List<Dummy>>() {}.getType();
    assertThat(methodInfo.requestType).isEqualTo(expected);
  }

  @Test public void wildcardBodyType() {
    class Example {
      @POST("/foo") Call<Object> a(@Body List<? super String> body) {
        return null;
      }
    }

    Method method = TestingUtils.onlyMethod(Example.class);
    MethodInfo methodInfo = new MethodInfo(method, FACTORY, CONVERTER);
    Type expected = new TypeToken<List<? super String>>() {}.getType();
    assertThat(methodInfo.requestType).isEqualTo(expected);
  }
}
