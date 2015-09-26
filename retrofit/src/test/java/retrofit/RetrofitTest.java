// Copyright 2013 Square, Inc.
package retrofit;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;

import static com.squareup.okhttp.mockwebserver.SocketPolicy.DISCONNECT_AT_START;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public final class RetrofitTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface CallMethod {
    @GET("/") Call<String> disallowed();
    @POST("/") Call<ResponseBody> disallowed(@Body String body);
    @GET("/") Call<ResponseBody> getResponseBody();
    @GET("/") Call<Void> getVoid();
    @POST("/") Call<ResponseBody> postRequestBody(@Body RequestBody body);
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
  interface VoidService {
    @GET("/") void nope();
  }
  interface Annotated {
    @GET("/") @Foo Call<String> method();
    @POST("/") Call<ResponseBody> parameter(@Foo @Body String param);

    @Retention(RUNTIME)
    @interface Foo {}
  }

  @SuppressWarnings("EqualsBetweenInconvertibleTypes") // We are explicitly testing this behavior.
  @Test public void objectMethodsStillWork() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    CallMethod example = retrofit.create(CallMethod.class);

    assertThat(example.hashCode()).isNotZero();
    assertThat(example.equals(this)).isFalse();
    assertThat(example.toString()).isNotEmpty();
  }

  @Test public void interfaceWithExtendIsNotSupported() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    try {
      retrofit.create(Extending.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("API interfaces must not extend other interfaces.");
    }
  }

  @Test public void voidReturnTypeNotAllowed() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    VoidService service = retrofit.create(VoidService.class);

    try {
      service.nope();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageStartingWith(
          "Service methods cannot return void.\n    for method VoidService.nope");
    }
  }

  @Test public void validateEagerlyFailsAtCreation() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .validateEagerly()
        .build();

    try {
      retrofit.create(VoidService.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageStartingWith(
          "Service methods cannot return void.\n    for method VoidService.nope");
    }
  }

  @Test public void callCallAdapterAddedByDefault() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    CallMethod example = retrofit.create(CallMethod.class);
    assertThat(example.getResponseBody()).isNotNull();
  }

  @Test public void callCallCustomAdapter() {
    final AtomicBoolean factoryCalled = new AtomicBoolean();
    final AtomicBoolean adapterCalled = new AtomicBoolean();
    class MyCallAdapterFactory implements CallAdapter.Factory {
      @Override public CallAdapter<?> get(final Type returnType, Annotation[] annotations,
          Retrofit retrofit) {
        factoryCalled.set(true);
        if (Utils.getRawType(returnType) != Call.class) {
          return null;
        }
        return new CallAdapter<Call<?>>() {
          @Override public Type responseType() {
            return Utils.getSingleParameterUpperBound((ParameterizedType) returnType);
          }

          @Override public <R> Call<R> adapt(Call<R> call) {
            adapterCalled.set(true);
            return call;
          }
        };
      }
    }

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addCallAdapterFactory(new MyCallAdapterFactory())
        .build();
    CallMethod example = retrofit.create(CallMethod.class);
    assertThat(example.getResponseBody()).isNotNull();
    assertThat(factoryCalled.get()).isTrue();
    assertThat(adapterCalled.get()).isTrue();
  }

  @Test public void customCallAdapter() {
    class GreetingCallAdapterFactory implements CallAdapter.Factory {
      @Override public CallAdapter<String> get(Type returnType, Annotation[] annotations,
          Retrofit retrofit) {
        if (Utils.getRawType(returnType) != String.class) {
          return null;
        }
        return new CallAdapter<String>() {
          @Override public Type responseType() {
            return String.class;
          }

          @Override public <R> String adapt(Call<R> call) {
            return "Hi!";
          }
        };
      }
    }

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .addCallAdapterFactory(new GreetingCallAdapterFactory())
        .build();
    StringService example = retrofit.create(StringService.class);
    assertThat(example.get()).isEqualTo("Hi!");
  }

  @Test public void methodAnnotationsPassedToCallAdapter() {
    final AtomicReference<Annotation[]> annotationsRef = new AtomicReference<>();
    class MyCallAdapterFactory implements CallAdapter.Factory {
      @Override public CallAdapter<?> get(Type returnType, Annotation[] annotations,
          Retrofit retrofit) {
        annotationsRef.set(annotations);
        return null;
      }
    }
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .addCallAdapterFactory(new MyCallAdapterFactory())
        .build();
    Annotated annotated = retrofit.create(Annotated.class);
    annotated.method(); // Trigger internal setup.

    Annotation[] annotations = annotationsRef.get();
    assertThat(annotations).hasAtLeastOneElementOfType(Annotated.Foo.class);
  }

  @Test public void customCallAdapterMissingThrows() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    FutureMethod example = retrofit.create(FutureMethod.class);
    try {
      example.method();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Unable to create call adapter for java.util.concurrent.Future<java.lang.String>\n"
              + "    for method FutureMethod.method");
      assertThat(e.getCause()).hasMessage(
          "Could not locate call adapter for java.util.concurrent.Future<java.lang.String>. Tried:\n"
              + " * retrofit.DefaultCallAdapter$1");
    }
  }

  @Test public void methodAnnotationsPassedToConverter() {
    final AtomicReference<Annotation[]> annotationsRef = new AtomicReference<>();
    class MyConverterFactory extends Converter.Factory {
      @Override
      public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
        annotationsRef.set(annotations);
        return new ToStringConverterFactory().fromResponseBody(type, annotations);
      }
    }
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new MyConverterFactory())
        .build();
    Annotated annotated = retrofit.create(Annotated.class);
    annotated.method(); // Trigger internal setup.

    Annotation[] annotations = annotationsRef.get();
    assertThat(annotations).hasAtLeastOneElementOfType(Annotated.Foo.class);
  }

  @Test public void parameterAnnotationsPassedToConverter() {
    final AtomicReference<Annotation[]> annotationsRef = new AtomicReference<>();
    class MyConverterFactory extends Converter.Factory {
      @Override
      public Converter<?, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
        annotationsRef.set(annotations);
        return new ToStringConverterFactory().toRequestBody(type, annotations);
      }
    }
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new MyConverterFactory())
        .build();
    Annotated annotated = retrofit.create(Annotated.class);
    annotated.parameter(null); // Trigger internal setup.

    Annotation[] annotations = annotationsRef.get();
    assertThat(annotations).hasAtLeastOneElementOfType(Annotated.Foo.class);
  }

  @Test public void missingConverterThrowsOnNonRequestBody() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    CallMethod example = retrofit.create(CallMethod.class);
    try {
      example.disallowed("Hi!");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Unable to create @Body converter for class java.lang.String (parameter #1)\n"
              + "    for method CallMethod.disallowed");
      assertThat(e.getCause()).hasMessage(
          "Could not locate RequestBody converter for class java.lang.String. Tried:\n"
              + " * retrofit.BuiltInConverters");
    }
  }

  @Test public void missingConverterThrowsOnNonResponseBody() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    CallMethod example = retrofit.create(CallMethod.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    try {
      example.disallowed();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Unable to create converter for class java.lang.String\n"
          + "    for method CallMethod.disallowed");
      assertThat(e.getCause()).hasMessage(
          "Could not locate ResponseBody converter for class java.lang.String. Tried:\n"
              + " * retrofit.BuiltInConverters");
    }
  }

  @Test public void converterReturningNullThrows() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new Converter.Factory() {})
        .build();
    CallMethod service = retrofit.create(CallMethod.class);

    try {
      service.disallowed();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Unable to create converter for class java.lang.String\n"
          + "    for method CallMethod.disallowed");
      assertThat(e.getCause()).hasMessage(
          "Could not locate ResponseBody converter for class java.lang.String. Tried:\n"
              + " * retrofit.BuiltInConverters\n"
              + " * retrofit.RetrofitTest$1");
    }
  }

  @Test public void requestBodyOutgoingAllowed() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    CallMethod example = retrofit.create(CallMethod.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    Response<ResponseBody> response = example.getResponseBody().execute();
    assertThat(response.body().string()).isEqualTo("Hi");
  }

  @Test public void voidOutgoingAllowed() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    CallMethod example = retrofit.create(CallMethod.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    Response<Void> response = example.getVoid().execute();
    assertThat(response.body()).isNull();
  }

  @Test public void responseBodyIncomingAllowed() throws IOException, InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    CallMethod example = retrofit.create(CallMethod.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "Hey");
    Response<ResponseBody> response = example.postRequestBody(body).execute();
    assertThat(response.body().string()).isEqualTo("Hi");

    assertThat(server.takeRequest().getBody().readUtf8()).isEqualTo("Hey");
  }

  @Test public void unresolvableTypeThrows() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
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
          + "retrofit.Call<java.util.List<java.util.Map<java.lang.String, java.util.Set<T[]>>>>\n"
          + "    for method Unresolvable.crazy");
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
          + "retrofit.Call<? extends com.squareup.okhttp.ResponseBody>\n"
          + "    for method Unresolvable.wildcardUpperBound");
    }
  }

  @Test public void baseUrlRequired() {
    try {
      new Retrofit.Builder().build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Base URL required.");
    }
  }

  @Test public void baseUrlNullThrows() {
    try {
      new Retrofit.Builder().baseUrl((String) null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("baseUrl == null");
    }
    try {
      new Retrofit.Builder().baseUrl((HttpUrl) null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("baseUrl == null");
    }
    try {
      new Retrofit.Builder().baseUrl((BaseUrl) null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("baseUrl == null");
    }
  }

  @Test public void baseUrlInvalidThrows() {
    try {
      new Retrofit.Builder().baseUrl("ftp://foo/bar");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Illegal URL: ftp://foo/bar");
    }
  }

  @Test public void baseUrlStringPropagated() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .build();
    BaseUrl baseUrl = retrofit.baseUrl();
    assertThat(baseUrl).isNotNull();
    assertThat(baseUrl.url().toString()).isEqualTo("http://example.com/");
  }

  @Test public void baseHttpUrlPropagated() {
    HttpUrl url = HttpUrl.parse("http://example.com/");
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(url)
        .build();
    BaseUrl baseUrl = retrofit.baseUrl();
    assertThat(baseUrl).isNotNull();
    assertThat(baseUrl.url()).isSameAs(url);
  }

  @Test public void baseUrlPropagated() {
    BaseUrl baseUrl = mock(BaseUrl.class);
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(baseUrl)
        .build();
    assertThat(retrofit.baseUrl()).isSameAs(baseUrl);
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
        .baseUrl("http://example.com")
        .build();
      assertThat(retrofit.client()).isNotNull();
  }

  @Test public void clientPropagated() {
    OkHttpClient client = new OkHttpClient();
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .client(client)
        .build();
    assertThat(retrofit.client()).isSameAs(client);
  }

  @Test public void converterNullThrows() {
    try {
      new Retrofit.Builder().addConverterFactory(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("converterFactory == null");
    }
  }

  @Test public void converterFactoryDefault() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .build();
    List<Converter.Factory> converterFactories = retrofit.converterFactories();
    assertThat(converterFactories).hasSize(1);
    assertThat(converterFactories.get(0)).isInstanceOf(BuiltInConverters.class);
  }

  @Test public void requestConverterFactoryQueried() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    Converter<?, RequestBody> expectedAdapter = mock(Converter.class);
    Converter.Factory factory = mock(Converter.Factory.class);

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addConverterFactory(factory)
        .build();

    doReturn(expectedAdapter).when(factory).toRequestBody(type, annotations);

    Converter<?, RequestBody> actualAdapter = retrofit.requestConverter(type, annotations);
    assertThat(actualAdapter).isSameAs(expectedAdapter);

    verify(factory).toRequestBody(type, annotations);
    verifyNoMoreInteractions(factory);
  }

  @Test public void requestConverterFactoryNoMatchThrows() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    Converter.Factory factory1 = spy(new Converter.Factory() {
      @Override
      public Converter<?, RequestBody> toRequestBody(Type returnType, Annotation[] annotations) {
        return null;
      }
    });

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addConverterFactory(factory1)
        .build();

    try {
      retrofit.requestConverter(type, annotations);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageStartingWith(
          "Could not locate RequestBody converter for class java.lang.String. Tried:");
    }

    verify(factory1).toRequestBody(type, annotations);
    verifyNoMoreInteractions(factory1);
  }

  @Test public void responseConverterFactoryQueried() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    Converter<ResponseBody, ?> expectedAdapter = mock(Converter.class);
    Converter.Factory factory = mock(Converter.Factory.class);

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addConverterFactory(factory)
        .build();

    doReturn(expectedAdapter).when(factory).fromResponseBody(type, annotations);

    Converter<ResponseBody, ?> actualAdapter = retrofit.responseConverter(type, annotations);
    assertThat(actualAdapter).isSameAs(expectedAdapter);

    verify(factory).fromResponseBody(type, annotations);
    verifyNoMoreInteractions(factory);
  }

  @Test public void responseConverterFactoryNoMatchThrows() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    Converter.Factory factory1 = spy(new Converter.Factory() {
      @Override
      public Converter<ResponseBody, ?> fromResponseBody(Type returnType, Annotation[] annotations) {
        return null;
      }
    });

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addConverterFactory(factory1)
        .build();

    try {
      retrofit.responseConverter(type, annotations);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageStartingWith(
          "Could not locate ResponseBody converter for class java.lang.String. Tried:");
    }

    verify(factory1).fromResponseBody(type, annotations);
    verifyNoMoreInteractions(factory1);
  }

  @Test public void converterFactoryPropagated() {
    Converter.Factory factory = mock(Converter.Factory.class);
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addConverterFactory(factory)
        .build();
    assertThat(retrofit.converterFactories()).contains(factory);
  }

  @Test public void callAdapterFactoryNullThrows() {
    try {
      new Retrofit.Builder().addCallAdapterFactory(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("factory == null");
    }
  }

  @Test public void callAdapterFactoryDefault() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .build();
    assertThat(retrofit.callAdapterFactories()).isNotEmpty();
  }

  @Test public void callAdapterFactoryPropagated() {
    CallAdapter.Factory factory = mock(CallAdapter.Factory.class);
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addCallAdapterFactory(factory)
        .build();
    assertThat(retrofit.callAdapterFactories()).contains(factory);
  }

  @Test public void callAdapterFactoryQueried() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    CallAdapter<?> expectedAdapter = mock(CallAdapter.class);
    CallAdapter.Factory factory = mock(CallAdapter.Factory.class);

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addCallAdapterFactory(factory)
        .build();

    doReturn(expectedAdapter).when(factory).get(type, annotations, retrofit);

    CallAdapter<?> actualAdapter = retrofit.callAdapter(type, annotations);
    assertThat(actualAdapter).isSameAs(expectedAdapter);

    verify(factory).get(type, annotations, retrofit);
    verifyNoMoreInteractions(factory);
  }

  @Test public void callAdapterFactoryQueriedCanDelegate() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    CallAdapter<?> expectedAdapter = mock(CallAdapter.class);
    CallAdapter.Factory factory2 = mock(CallAdapter.Factory.class);
    CallAdapter.Factory factory1 = spy(new CallAdapter.Factory() {
      @Override
      public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        return retrofit.nextCallAdapter(this, returnType, annotations);
      }
    });

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addCallAdapterFactory(factory1)
        .addCallAdapterFactory(factory2)
        .build();

    doReturn(expectedAdapter).when(factory2).get(type, annotations, retrofit);

    CallAdapter<?> actualAdapter = retrofit.callAdapter(type, annotations);
    assertThat(actualAdapter).isSameAs(expectedAdapter);

    verify(factory1).get(type, annotations, retrofit);
    verifyNoMoreInteractions(factory1);
    verify(factory2).get(type, annotations, retrofit);
    verifyNoMoreInteractions(factory2);
  }

  @Test public void callAdapterFactoryQueriedCanDelegateTwiceWithoutRecursion() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    CallAdapter<?> expectedAdapter = mock(CallAdapter.class);
    CallAdapter.Factory factory3 = mock(CallAdapter.Factory.class);
    CallAdapter.Factory factory2 = spy(new CallAdapter.Factory() {
      @Override
      public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        return retrofit.nextCallAdapter(this, returnType, annotations);
      }
    });
    CallAdapter.Factory factory1 = spy(new CallAdapter.Factory() {
      @Override
      public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        return retrofit.nextCallAdapter(this, returnType, annotations);
      }
    });

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addCallAdapterFactory(factory1)
        .addCallAdapterFactory(factory2)
        .addCallAdapterFactory(factory3)
        .build();

    doReturn(expectedAdapter).when(factory3).get(type, annotations, retrofit);

    CallAdapter<?> actualAdapter = retrofit.callAdapter(type, annotations);
    assertThat(actualAdapter).isSameAs(expectedAdapter);

    verify(factory1).get(type, annotations, retrofit);
    verifyNoMoreInteractions(factory1);
    verify(factory2).get(type, annotations, retrofit);
    verifyNoMoreInteractions(factory2);
    verify(factory3).get(type, annotations, retrofit);
    verifyNoMoreInteractions(factory3);
  }

  @Test public void callAdapterFactoryNoMatchThrows() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    CallAdapter.Factory factory = mock(CallAdapter.Factory.class);

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addCallAdapterFactory(factory)
        .build();

    doReturn(null).when(factory).get(type, annotations, retrofit);

    try {
      retrofit.callAdapter(type, annotations);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageStartingWith(
          "Could not locate call adapter for class java.lang.String. Tried:");
    }

    verify(factory).get(type, annotations, retrofit);
    verifyNoMoreInteractions(factory);
  }

  @Test public void callAdapterFactoryDelegateNoMatchThrows() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    CallAdapter.Factory factory1 = spy(new CallAdapter.Factory() {
      @Override
      public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        return retrofit.nextCallAdapter(this, returnType, annotations);
      }
    });

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addCallAdapterFactory(factory1)
        .build();

    try {
      retrofit.callAdapter(type, annotations);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageContaining("Skipped:")
          .hasMessageStartingWith(
              "Could not locate call adapter for class java.lang.String. Tried:");
    }

    verify(factory1).get(type, annotations, retrofit);
    verifyNoMoreInteractions(factory1);
  }

  @Test public void callbackExecutorNullThrows() {
    try {
      new Retrofit.Builder().callbackExecutor(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("callbackExecutor == null");
    }
  }

  @Test public void callbackExecutorNoDefault() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .build();
    assertThat(retrofit.callbackExecutor()).isNull();
  }

  @Test public void callbackExecutorPropagated() {
    Executor executor = mock(Executor.class);
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .callbackExecutor(executor)
        .build();
    assertThat(retrofit.callbackExecutor()).isSameAs(executor);
  }

  @Test public void callbackExecutorUsedForSuccess() throws InterruptedException {
    Executor executor = spy(new Executor() {
      @Override public void execute(Runnable command) {
        command.run();
      }
    });
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .callbackExecutor(executor)
        .build();
    CallMethod service = retrofit.create(CallMethod.class);
    Call<ResponseBody> call = service.getResponseBody();

    server.enqueue(new MockResponse());

    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<ResponseBody>() {
      @Override public void onResponse(Response<ResponseBody> response, Retrofit retrofit) {
        latch.countDown();
      }

      @Override public void onFailure(Throwable t) {
        t.printStackTrace();
      }
    });
    assertTrue(latch.await(2, TimeUnit.SECONDS));

    verify(executor).execute(any(Runnable.class));
    verifyNoMoreInteractions(executor);
  }

  @Test public void callbackExecutorUsedForFailure() throws InterruptedException {
    Executor executor = spy(new Executor() {
      @Override public void execute(Runnable command) {
        command.run();
      }
    });
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .callbackExecutor(executor)
        .build();
    CallMethod service = retrofit.create(CallMethod.class);
    Call<ResponseBody> call = service.getResponseBody();

    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AT_START));

    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<ResponseBody>() {
      @Override public void onResponse(Response<ResponseBody> response, Retrofit retrofit) {
        throw new AssertionError();
      }

      @Override public void onFailure(Throwable t) {
        latch.countDown();
      }
    });
    assertTrue(latch.await(2, TimeUnit.SECONDS));

    verify(executor).execute(any(Runnable.class));
    verifyNoMoreInteractions(executor);
  }
}
