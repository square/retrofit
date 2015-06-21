// Copyright 2013 Square, Inc.
package retrofit;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
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
import static org.mockito.Mockito.mock;

public final class RetrofitTest {
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
  interface Unresolvable {
    @GET("/") <T> Call<T> typeVariable();
    @GET("/") <T extends ResponseBody> Call<T> typeVariableUpperBound();
    @GET("/") <T> Call<List<Map<String, Set<T[]>>>> crazy();
    @GET("/") Call<?> wildcard();
    @GET("/") Call<? extends ResponseBody> wildcardUpperBound();
  }

  @SuppressWarnings("EqualsBetweenInconvertibleTypes") // We are explicitly testing this behavior.
  @Test public void objectMethodsStillWork() {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    CallMethod example = retrofit.create(CallMethod.class);

    assertThat(example.hashCode()).isNotZero();
    assertThat(example.equals(this)).isFalse();
    assertThat(example.toString()).isNotEmpty();
  }

  @Test public void interfaceWithExtendIsNotSupported() {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    try {
      retrofit.create(Extending.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Interface definitions must not extend other interfaces.");
    }
  }

  @Test public void callReturnTypeAdapterAddedByDefault() {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    CallMethod example = retrofit.create(CallMethod.class);
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

    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .callAdapterFactory(new MyCallAdapterFactory())
        .build();
    CallMethod example = retrofit.create(CallMethod.class);
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

    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .converterFactory(new ToStringConverterFactory())
        .callAdapterFactory(new GreetingCallAdapterFactory())
        .build();
    StringService example = retrofit.create(StringService.class);
    assertThat(example.get()).isEqualTo("Hi!");
  }

  @Test public void customReturnTypeAdapterMissingThrows() {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    FutureMethod example = retrofit.create(FutureMethod.class);
    try {
      example.method();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Call adapter factory 'Default CallAdapterFactory' was unable to handle return type java.util.concurrent.Future<java.lang.String>\n    for method FutureMethod.method");
    }
  }

  @Test public void missingConverterThrowsOnNonRequestBody() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    CallMethod example = retrofit.create(CallMethod.class);
    try {
      example.disallowed("Hi!");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "@Body parameter is class java.lang.String but no converter factory registered. "
              + "Either add a converter factory to the Retrofit instance or use RequestBody. (parameter #1)\n    for method CallMethod.disallowed");
    }
  }

  @Test public void missingConverterThrowsOnNonResponseBody() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    CallMethod example = retrofit.create(CallMethod.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    try {
      example.disallowed();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Method response type is class java.lang.String but no converter factory registered. "
              + "Either add a converter factory to the Retrofit instance or use ResponseBody.\n    for method CallMethod.disallowed");
    }
  }

  @Test public void requestBodyOutgoingAllowed() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    CallMethod example = retrofit.create(CallMethod.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    Response<ResponseBody> response = example.allowed().execute();
    assertThat(response.body().string()).isEqualTo("Hi");
  }

  @Test public void responseBodyIncomingAllowed() throws IOException, InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .build();
    CallMethod example = retrofit.create(CallMethod.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "Hey");
    Response<ResponseBody> response = example.allowed(body).execute();
    assertThat(response.body().string()).isEqualTo("Hi");

    assertThat(server.takeRequest().getBody().readUtf8()).isEqualTo("Hey");
  }

  @Test public void unresolvableTypeThrows() {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(server.getUrl("/").toString())
        .converterFactory(new ToStringConverterFactory())
        .build();
    Unresolvable example = retrofit.create(Unresolvable.class);

    try {
      example.typeVariable();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Method return type must not include a type variable or wildcard: "
          + "retrofit.Call<T>\n    for method Unresolvable.typeVariable");
    }
    try {
      example.typeVariableUpperBound();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Method return type must not include a type variable or wildcard: "
          + "retrofit.Call<T>\n    for method Unresolvable.typeVariableUpperBound");
    }
    try {
      example.crazy();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Method return type must not include a type variable or wildcard: "
          + "retrofit.Call<java.util.List<java.util.Map<java.lang.String, java.util.Set<T[]>>>>\n    for method Unresolvable.crazy");
    }
    try {
      example.wildcard();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Method return type must not include a type variable or wildcard: "
          + "retrofit.Call<?>\n    for method Unresolvable.wildcard");
    }
    try {
      example.wildcardUpperBound();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Method return type must not include a type variable or wildcard: "
          + "retrofit.Call<? extends com.squareup.okhttp.ResponseBody>\n    for method Unresolvable.wildcardUpperBound");
    }
  }

  @Test public void endpointRequired() {
    try {
      new Retrofit.Builder().build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Endpoint required.");
    }
  }

  @Test public void endpointNullThrows() {
    try {
      new Retrofit.Builder().endpoint((String) null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("url == null");
    }
    try {
      new Retrofit.Builder().endpoint((HttpUrl) null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("url == null");
    }
    try {
      new Retrofit.Builder().endpoint((Endpoint) null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("endpoint == null");
    }
  }

  @Test public void endpointInvalidThrows() {
    try {
      new Retrofit.Builder().endpoint("ftp://foo/bar");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Illegal URL: ftp://foo/bar");
    }
  }

  @Test public void endpointStringPropagated() {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint("http://example.com/")
        .build();
    Endpoint endpoint = retrofit.endpoint();
    assertThat(endpoint).isNotNull();
    assertThat(endpoint.url().toString()).isEqualTo("http://example.com/");
  }

  @Test public void endpointHttpUrlPropagated() {
    HttpUrl url = HttpUrl.parse("http://example.com/");
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(url)
        .build();
    Endpoint endpoint = retrofit.endpoint();
    assertThat(endpoint).isNotNull();
    assertThat(endpoint.url()).isSameAs(url);
  }

  @Test public void endpointPropagated() {
    Endpoint endpoint = mock(Endpoint.class);
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint(endpoint)
        .build();
    assertThat(retrofit.endpoint()).isSameAs(endpoint);
  }

  @Test public void clientNullThrows() {
    try {
      new Retrofit.Builder().client(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("client == null");
    }
  }

  @Test public void clientDefault() {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint("http://example.com")
        .build();
      assertThat(retrofit.client()).isNotNull();
  }

  @Test public void clientPropagated() {
    OkHttpClient client = new OkHttpClient();
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint("http://example.com/")
        .client(client)
        .build();
    assertThat(retrofit.client()).isSameAs(client);
  }

  @Test public void converterNullThrows() {
    try {
      new Retrofit.Builder().converterFactory(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("converterFactory == null");
    }
  }

  @Test public void converterNoDefault() {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint("http://example.com/")
        .build();
    assertThat(retrofit.converterFactory()).isNull();
  }

  @Test public void converterFactoryPropagated() {
    Converter.Factory factory = mock(Converter.Factory.class);
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint("http://example.com/")
        .converterFactory(factory)
        .build();
    assertThat(retrofit.converterFactory()).isSameAs(factory);
  }

  @Test public void callAdapterFactoryNullThrows() {
    try {
      new Retrofit.Builder().callAdapterFactory(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("factory == null");
    }
  }

  @Test public void callAdapterFactoryDefault() {
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint("http://example.com/")
        .build();
    assertThat(retrofit.callAdapterFactory()).isInstanceOf(DefaultCallAdapterFactory.class);
  }

  @Test public void callAdapterFactoryPropagated() {
    CallAdapter.Factory factory = mock(CallAdapter.Factory.class);
    Retrofit retrofit = new Retrofit.Builder()
        .endpoint("http://example.com/")
        .callAdapterFactory(factory)
        .build();
    assertThat(retrofit.callAdapterFactory()).isSameAs(factory);
  }
}
