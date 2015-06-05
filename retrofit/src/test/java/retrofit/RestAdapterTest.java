// Copyright 2013 Square, Inc.
package retrofit;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.rule.MockWebServerRule;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Rule;
import org.junit.Test;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class RestAdapterTest {
  @Rule public final MockWebServerRule server = new MockWebServerRule();

  interface CallMethod {
    @GET("/") Call<String> disallowed();
    @POST("/") Call<ResponseBody> disallowed(@Body String body);
    @GET("/") Call<ResponseBody> allowed();
    @POST("/") Call<ResponseBody> allowed(@Body RequestBody body);
  }
  interface FutureMethod {
    @GET("/") Future<String> method();
  }
  interface Extending extends CallMethod {
  }
  interface StringService {
    @GET("/") String get();
  }
  interface BoundsService {
    @GET("/") <T> Call<T> none();
    @GET("/") <T extends ResponseBody> Call<T> upper();
    @GET("/") <T> Call<List<Map<String, Set<T[]>>>> crazy();
  }

  @SuppressWarnings("EqualsBetweenInconvertibleTypes") // We are explicitly testing this behavior.
  @Test public void objectMethodsStillWork() {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    CallMethod example = ra.create(CallMethod.class);

    assertThat(example.hashCode()).isNotZero();
    assertThat(example.equals(this)).isFalse();
    assertThat(example.toString()).isNotEmpty();
  }

  @Test public void interfaceWithExtendIsNotSupported() {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    try {
      ra.create(Extending.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Interface definitions must not extend other interfaces.");
    }
  }

  @Test public void callReturnTypeAdapterAddedByDefault() {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    CallMethod example = ra.create(CallMethod.class);
    assertThat(example.allowed()).isNotNull();
  }

  @Test public void callReturnTypeCustomAdapter() {
    final AtomicBoolean factoryCalled = new AtomicBoolean();
    final AtomicBoolean adapterCalled = new AtomicBoolean();
    class MyCallAdapterFactory implements CallAdapter.Factory {
      @Override public CallAdapter<?> get(final Type returnType) {
        factoryCalled.set(true);
        if (Utils.getRawType(returnType) != Call.class) {
          return null;
        }
        return new CallAdapter<Object>() {
          @Override public Type responseType() {
            return Utils.getSingleParameterUpperBound((ParameterizedType) returnType);
          }

          @Override public Object adapt(Call<Object> call) {
            adapterCalled.set(true);
            return call;
          }
        };
      }
    }

    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .callAdapterFactory(new MyCallAdapterFactory())
        .build();
    CallMethod example = ra.create(CallMethod.class);
    assertThat(example.allowed()).isNotNull();
    assertThat(factoryCalled.get()).isTrue();
    assertThat(adapterCalled.get()).isTrue();
  }

  @Test public void customReturnTypeAdapter() {
    class GreetingCallAdapterFactory implements CallAdapter.Factory {
      @Override public CallAdapter<?> get(Type returnType) {
        if (Utils.getRawType(returnType) != String.class) {
          return null;
        }
        return new CallAdapter<Object>() {
          @Override public Type responseType() {
            return String.class;
          }

          @Override public String adapt(Call<Object> call) {
            return "Hi!";
          }
        };
      }
    }

    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter())
        .callAdapterFactory(new GreetingCallAdapterFactory())
        .build();
    StringService example = ra.create(StringService.class);
    assertThat(example.get()).isEqualTo("Hi!");
  }

  @Test public void customReturnTypeAdapterMissingThrows() {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    FutureMethod example = ra.create(FutureMethod.class);
    try {
      example.method();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("FutureMethod.method: Registered call adapter factory was unable to handle return type java.util.concurrent.Future<java.lang.String>");
    }
  }

  @Test public void missingConverterThrowsOnNonRequestBody() throws IOException {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    CallMethod example = ra.create(CallMethod.class);
    try {
      example.disallowed("Hi!");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "CallMethod.disallowed: @Body parameter is class java.lang.String but no converter registered. "
              + "Either add a converter to the RestAdapter or use RequestBody. (parameter #1)");
    }
  }

  @Test public void missingConverterThrowsOnNonResponseBody() throws IOException {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    CallMethod example = ra.create(CallMethod.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    try {
      example.disallowed();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "CallMethod.disallowed: Method response type is class java.lang.String but no converter registered. "
              + "Either add a converter to the RestAdapter or use ResponseBody.");
    }
  }

  @Test public void requestBodyOutgoingAllowed() throws IOException {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    CallMethod example = ra.create(CallMethod.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    Response<ResponseBody> response = example.allowed().execute();
    assertThat(response.body().string()).isEqualTo("Hi");
  }

  @Test public void responseBodyIncomingAllowed() throws IOException, InterruptedException {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    CallMethod example = ra.create(CallMethod.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "Hey");
    Response<ResponseBody> response = example.allowed(body).execute();
    assertThat(response.body().string()).isEqualTo("Hi");

    assertThat(server.takeRequest().getBody().readUtf8()).isEqualTo("Hey");
  }

  @Test public void typeVariableNoBoundThrows() {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter())
        .build();
    BoundsService example = ra.create(BoundsService.class);

    try {
      example.none();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "BoundsService.none: Method response type must not include a type variable.");
    }
  }

  @Test public void typeVariableUpperBoundThrows() {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter())
        .build();
    BoundsService example = ra.create(BoundsService.class);

    try {
      example.upper();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "BoundsService.upper: Method response type must not include a type variable.");
    }
  }

  @Test public void typeVariableNestedThrows() {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint(server.getUrl("/").toString())
        .converter(new StringConverter())
        .build();
    BoundsService example = ra.create(BoundsService.class);

    try {
      example.crazy();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "BoundsService.crazy: Method response type must not include a type variable.");
    }
  }
}
