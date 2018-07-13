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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.helpers.DelegatingCallAdapterFactory;
import retrofit2.helpers.NonMatchingCallAdapterFactory;
import retrofit2.helpers.NonMatchingConverterFactory;
import retrofit2.helpers.ToStringConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

public final class RetrofitTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface CallMethod {
    @GET("/") Call<String> disallowed();
    @POST("/") Call<ResponseBody> disallowed(@Body String body);

    @GET("/") Call<retrofit2.Response> badType1();
    @GET("/") Call<okhttp3.Response> badType2();

    @GET("/") Call<ResponseBody> getResponseBody();
    @GET("/") Call<Void> getVoid();
    @POST("/") Call<ResponseBody> postRequestBody(@Body RequestBody body);
    @GET("/") Call<ResponseBody> queryString(@Query("foo") String foo);
    @GET("/") Call<ResponseBody> queryObject(@Query("foo") Object foo);
  }
  interface FutureMethod {
    @GET("/") Future<String> method();
  }
  interface Extending extends CallMethod {
  }
  interface StringService {
    @GET("/") String get();
  }
  interface UnresolvableResponseType {
    @GET("/") <T> Call<T> typeVariable();
    @GET("/") <T extends ResponseBody> Call<T> typeVariableUpperBound();
    @GET("/") <T> Call<List<Map<String, Set<T[]>>>> crazy();
    @GET("/") Call<?> wildcard();
    @GET("/") Call<? extends ResponseBody> wildcardUpperBound();
  }
  interface UnresolvableParameterType {
    @POST("/") <T> Call<ResponseBody> typeVariable(@Body T body);
    @POST("/") <T extends RequestBody> Call<ResponseBody> typeVariableUpperBound(@Body T body);
    @POST("/") <T> Call<ResponseBody> crazy(@Body List<Map<String, Set<T[]>>> body);
    @POST("/") Call<ResponseBody> wildcard(@Body List<?> body);
    @POST("/") Call<ResponseBody> wildcardUpperBound(@Body List<? extends RequestBody> body);
  }
  interface VoidService {
    @GET("/") void nope();
  }
  interface Annotated {
    @GET("/") @Foo Call<String> method();
    @POST("/") Call<ResponseBody> bodyParameter(@Foo @Body String param);
    @GET("/") Call<ResponseBody> queryParameter(@Foo @Query("foo") Object foo);

    @Retention(RUNTIME)
    @interface Foo {}
  }
  interface MutableParameters {
    @GET("/") Call<String> method(@Query("i") AtomicInteger value);
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

  @Test public void cloneSharesStatefulInstances() {
    CallAdapter.Factory callAdapter = mock(CallAdapter.Factory.class);
    Converter.Factory converter = mock(Converter.Factory.class);
    HttpUrl baseUrl = server.url("/");
    Executor executor = mock(Executor.class);
    okhttp3.Call.Factory callFactory = mock(okhttp3.Call.Factory.class);

    Retrofit one = new Retrofit.Builder()
        .addCallAdapterFactory(callAdapter)
        .addConverterFactory(converter)
        .baseUrl(baseUrl)
        .callbackExecutor(executor)
        .callFactory(callFactory)
        .build();

    CallAdapter.Factory callAdapter2 = mock(CallAdapter.Factory.class);
    Converter.Factory converter2 = mock(Converter.Factory.class);
    Retrofit two = one.newBuilder()
        .addCallAdapterFactory(callAdapter2)
        .addConverterFactory(converter2)
        .build();
    assertEquals(one.callAdapterFactories().size() + 1, two.callAdapterFactories().size());
    assertThat(two.callAdapterFactories()).contains(callAdapter, callAdapter2);
    assertEquals(one.converterFactories().size() + 1, two.converterFactories().size());
    assertThat(two.converterFactories()).contains(converter, converter2);
    assertSame(baseUrl, two.baseUrl());
    assertSame(executor, two.callbackExecutor());
    assertSame(callFactory, two.callFactory());
  }

  @Test public void builtInConvertersAbsentInCloneBuilder() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();

    assertEquals(0, retrofit.newBuilder().converterFactories().size());
  }

  @Test public void responseTypeCannotBeRetrofitResponse() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    CallMethod service = retrofit.create(CallMethod.class);
    try {
      service.badType1();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "'retrofit2.Response' is not a valid response body type. Did you mean ResponseBody?\n"
              + "    for method CallMethod.badType1");
    }
  }

  @Test public void responseTypeCannotBeOkHttpResponse() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    CallMethod service = retrofit.create(CallMethod.class);
    try {
      service.badType2();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "'okhttp3.Response' is not a valid response body type. Did you mean ResponseBody?\n"
              + "    for method CallMethod.badType2");
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

  @Test public void validateEagerlyDisabledByDefault() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();

    // Should not throw exception about incorrect configuration of the VoidService
    retrofit.create(VoidService.class);
  }

  @Test public void validateEagerlyDisabledByUser() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .validateEagerly(false)
        .build();

    // Should not throw exception about incorrect configuration of the VoidService
    retrofit.create(VoidService.class);
  }

  @Test public void validateEagerlyFailsAtCreation() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .validateEagerly(true)
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
    class MyCallAdapterFactory extends CallAdapter.Factory {
      @Override public CallAdapter<?, ?> get(final Type returnType, Annotation[] annotations,
          Retrofit retrofit) {
        factoryCalled.set(true);
        if (getRawType(returnType) != Call.class) {
          return null;
        }
        return new CallAdapter<Object, Call<?>>() {
          @Override public Type responseType() {
            return getParameterUpperBound(0, (ParameterizedType) returnType);
          }

          @Override public Call<Object> adapt(Call<Object> call) {
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
    class GreetingCallAdapterFactory extends CallAdapter.Factory {
      @Override public CallAdapter<Object, String> get(Type returnType, Annotation[] annotations,
          Retrofit retrofit) {
        if (getRawType(returnType) != String.class) {
          return null;
        }
        return new CallAdapter<Object, String>() {
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
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .addCallAdapterFactory(new GreetingCallAdapterFactory())
        .build();
    StringService example = retrofit.create(StringService.class);
    assertThat(example.get()).isEqualTo("Hi!");
  }

  @Test public void methodAnnotationsPassedToCallAdapter() {
    final AtomicReference<Annotation[]> annotationsRef = new AtomicReference<>();
    class MyCallAdapterFactory extends CallAdapter.Factory {
      @Override public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations,
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
      assertThat(e).hasMessage(""
          + "Unable to create call adapter for java.util.concurrent.Future<java.lang.String>\n"
          + "    for method FutureMethod.method");
      assertThat(e.getCause()).hasMessage(""
          + "Could not locate call adapter for java.util.concurrent.Future<java.lang.String>.\n"
          + "  Tried:\n"
          + "   * retrofit2.DefaultCallAdapterFactory");
    }
  }

  @Test public void methodAnnotationsPassedToResponseBodyConverter() {
    final AtomicReference<Annotation[]> annotationsRef = new AtomicReference<>();
    class MyConverterFactory extends Converter.Factory {
      @Override
      public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
          Retrofit retrofit) {
        annotationsRef.set(annotations);
        return new ToStringConverterFactory().responseBodyConverter(type, annotations, retrofit);
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

  @Test public void methodAndParameterAnnotationsPassedToRequestBodyConverter() {
    final AtomicReference<Annotation[]> parameterAnnotationsRef = new AtomicReference<>();
    final AtomicReference<Annotation[]> methodAnnotationsRef = new AtomicReference<>();

    class MyConverterFactory extends Converter.Factory {
      @Override
      public Converter<?, RequestBody> requestBodyConverter(Type type,
          Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        parameterAnnotationsRef.set(parameterAnnotations);
        methodAnnotationsRef.set(methodAnnotations);
        return new ToStringConverterFactory().requestBodyConverter(type, parameterAnnotations,
            methodAnnotations, retrofit);
      }
    }
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new MyConverterFactory())
        .build();
    Annotated annotated = retrofit.create(Annotated.class);
    annotated.bodyParameter(null); // Trigger internal setup.

    assertThat(parameterAnnotationsRef.get()).hasAtLeastOneElementOfType(Annotated.Foo.class);
    assertThat(methodAnnotationsRef.get()).hasAtLeastOneElementOfType(POST.class);
  }

  @Test public void parameterAnnotationsPassedToStringConverter() {
    final AtomicReference<Annotation[]> annotationsRef = new AtomicReference<>();
    class MyConverterFactory extends Converter.Factory {
      @Override public Converter<?, String> stringConverter(Type type, Annotation[] annotations,
          Retrofit retrofit) {
        annotationsRef.set(annotations);

        return new Converter<Object, String>() {
          @Override public String convert(Object value) throws IOException {
            return String.valueOf(value);
          }
        };
      }
    }
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new MyConverterFactory())
        .build();
    Annotated annotated = retrofit.create(Annotated.class);
    annotated.queryParameter(null); // Trigger internal setup.

    Annotation[] annotations = annotationsRef.get();
    assertThat(annotations).hasAtLeastOneElementOfType(Annotated.Foo.class);
  }

  @Test public void stringConverterCalledForString() {
    final AtomicBoolean factoryCalled = new AtomicBoolean();
    class MyConverterFactory extends Converter.Factory {
      @Override public Converter<?, String> stringConverter(Type type, Annotation[] annotations,
          Retrofit retrofit) {
        factoryCalled.set(true);
        return null;
      }
    }
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new MyConverterFactory())
        .build();
    CallMethod service = retrofit.create(CallMethod.class);
    Call<ResponseBody> call = service.queryString(null);
    assertThat(call).isNotNull();
    assertThat(factoryCalled.get()).isTrue();
  }

  @Test public void stringConverterReturningNullResultsInDefault() {
    final AtomicBoolean factoryCalled = new AtomicBoolean();
    class MyConverterFactory extends Converter.Factory {
      @Override public Converter<?, String> stringConverter(Type type, Annotation[] annotations,
          Retrofit retrofit) {
        factoryCalled.set(true);
        return null;
      }
    }
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new MyConverterFactory())
        .build();
    CallMethod service = retrofit.create(CallMethod.class);
    Call<ResponseBody> call = service.queryObject(null);
    assertThat(call).isNotNull();
    assertThat(factoryCalled.get()).isTrue();
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
      assertThat(e).hasMessage(""
          + "Unable to create @Body converter for class java.lang.String (parameter #1)\n"
          + "    for method CallMethod.disallowed");
      assertThat(e.getCause()).hasMessage(""
          + "Could not locate RequestBody converter for class java.lang.String.\n"
          + "  Tried:\n"
          + "   * retrofit2.BuiltInConverters");
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
      assertThat(e).hasMessage(""
          + "Unable to create converter for class java.lang.String\n"
          + "    for method CallMethod.disallowed");
      assertThat(e.getCause()).hasMessage(""
          + "Could not locate ResponseBody converter for class java.lang.String.\n"
          + "  Tried:\n"
          + "   * retrofit2.BuiltInConverters");
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

  @Test public void voidResponsesArePooled() throws Exception {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    CallMethod example = retrofit.create(CallMethod.class);

    server.enqueue(new MockResponse().setBody("abc"));
    server.enqueue(new MockResponse().setBody("def"));

    example.getVoid().execute();
    example.getVoid().execute();

    assertThat(server.takeRequest().getSequenceNumber()).isEqualTo(0);
    assertThat(server.takeRequest().getSequenceNumber()).isEqualTo(1);
  }

  @Test public void responseBodyIncomingAllowed() throws IOException, InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    CallMethod example = retrofit.create(CallMethod.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    RequestBody body = RequestBody.create(MediaType.get("text/plain"), "Hey");
    Response<ResponseBody> response = example.postRequestBody(body).execute();
    assertThat(response.body().string()).isEqualTo("Hi");

    assertThat(server.takeRequest().getBody().readUtf8()).isEqualTo("Hey");
  }

  @Test public void unresolvableResponseTypeThrows() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    UnresolvableResponseType example = retrofit.create(UnresolvableResponseType.class);

    try {
      example.typeVariable();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Method return type must not include a type variable or wildcard: "
          + "retrofit2.Call<T>\n    for method UnresolvableResponseType.typeVariable");
    }
    try {
      example.typeVariableUpperBound();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Method return type must not include a type variable or wildcard: "
          + "retrofit2.Call<T>\n    for method UnresolvableResponseType.typeVariableUpperBound");
    }
    try {
      example.crazy();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Method return type must not include a type variable or wildcard: "
          + "retrofit2.Call<java.util.List<java.util.Map<java.lang.String, java.util.Set<T[]>>>>\n"
          + "    for method UnresolvableResponseType.crazy");
    }
    try {
      example.wildcard();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Method return type must not include a type variable or wildcard: "
          + "retrofit2.Call<?>\n    for method UnresolvableResponseType.wildcard");
    }
    try {
      example.wildcardUpperBound();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Method return type must not include a type variable or wildcard: "
          + "retrofit2.Call<? extends okhttp3.ResponseBody>\n"
          + "    for method UnresolvableResponseType.wildcardUpperBound");
    }
  }

  @Test public void unresolvableParameterTypeThrows() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    UnresolvableParameterType example = retrofit.create(UnresolvableParameterType.class);

    try {
      example.typeVariable(null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Parameter type must not include a type variable or wildcard: "
          + "T (parameter #1)\n    for method UnresolvableParameterType.typeVariable");
    }
    try {
      example.typeVariableUpperBound(null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Parameter type must not include a type variable or wildcard: "
          + "T (parameter #1)\n    for method UnresolvableParameterType.typeVariableUpperBound");
    }
    try {
      example.crazy(null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Parameter type must not include a type variable or wildcard: "
          + "java.util.List<java.util.Map<java.lang.String, java.util.Set<T[]>>> (parameter #1)\n"
          + "    for method UnresolvableParameterType.crazy");
    }
    try {
      example.wildcard(null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Parameter type must not include a type variable or wildcard: "
          + "java.util.List<?> (parameter #1)\n    for method UnresolvableParameterType.wildcard");
    }
    try {
      example.wildcardUpperBound(null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Parameter type must not include a type variable or wildcard: "
          + "java.util.List<? extends okhttp3.RequestBody> (parameter #1)\n"
          + "    for method UnresolvableParameterType.wildcardUpperBound");
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
  }

  @Test public void baseUrlInvalidThrows() {
    try {
      new Retrofit.Builder().baseUrl("ftp://foo/bar");
      fail();
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test public void baseUrlNoTrailingSlashThrows() {
    try {
      new Retrofit.Builder().baseUrl("http://example.com/api");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("baseUrl must end in /: http://example.com/api");
    }
    HttpUrl parsed = HttpUrl.get("http://example.com/api");
    try {
      new Retrofit.Builder().baseUrl(parsed);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("baseUrl must end in /: http://example.com/api");
    }
  }

  @Test public void baseUrlStringPropagated() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .build();
    HttpUrl baseUrl = retrofit.baseUrl();
    assertThat(baseUrl).isEqualTo(HttpUrl.get("http://example.com/"));
  }

  @Test public void baseHttpUrlPropagated() {
    HttpUrl url = HttpUrl.get("http://example.com/");
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(url)
        .build();
    assertThat(retrofit.baseUrl()).isSameAs(url);
  }

  @Test public void clientNullThrows() {
    try {
      new Retrofit.Builder().client(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("client == null");
    }
  }

  @Test public void callFactoryDefault() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com")
        .build();
    assertThat(retrofit.callFactory()).isNotNull();
  }

  @Test public void callFactoryPropagated() {
    okhttp3.Call.Factory callFactory = mock(okhttp3.Call.Factory.class);
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .callFactory(callFactory)
        .build();
    assertThat(retrofit.callFactory()).isSameAs(callFactory);
  }

  @Test public void callFactoryClientPropagated() {
    OkHttpClient client = new OkHttpClient();
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .client(client)
        .build();
    assertThat(retrofit.callFactory()).isSameAs(client);
  }

  @Test public void callFactoryUsed() throws IOException {
    okhttp3.Call.Factory callFactory = spy(new okhttp3.Call.Factory() {
      @Override public okhttp3.Call newCall(Request request) {
        return new OkHttpClient().newCall(request);
      }
    });
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .callFactory(callFactory)
        .build();

    server.enqueue(new MockResponse());

    CallMethod service = retrofit.create(CallMethod.class);
    service.getResponseBody().execute();
    verify(callFactory).newCall(any(Request.class));
    verifyNoMoreInteractions(callFactory);
  }

  @Test public void callFactoryReturningNullThrows() throws IOException {
    okhttp3.Call.Factory callFactory = new okhttp3.Call.Factory() {
      @Override public okhttp3.Call newCall(Request request) {
        return null;
      }
    };
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .callFactory(callFactory)
        .build();

    server.enqueue(new MockResponse());

    CallMethod service = retrofit.create(CallMethod.class);
    Call<ResponseBody> call = service.getResponseBody();
    try {
      call.execute();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("Call.Factory returned null.");
    }
  }

  @Test public void callFactoryThrowingPropagates() {
    final RuntimeException cause = new RuntimeException("Broken!");
    okhttp3.Call.Factory callFactory = new okhttp3.Call.Factory() {
      @Override public okhttp3.Call newCall(Request request) {
        throw cause;
      }
    };
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .callFactory(callFactory)
        .build();

    server.enqueue(new MockResponse());

    CallMethod service = retrofit.create(CallMethod.class);
    Call<ResponseBody> call = service.getResponseBody();
    try {
      call.execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isSameAs(cause);
    }
  }

  @Test public void converterNullThrows() {
    try {
      new Retrofit.Builder().addConverterFactory(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("factory == null");
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

  @Test public void builtInConvertersFirstInClone() {
    Converter<ResponseBody, Void> converter = mock(Converter.class);
    Converter.Factory factory = mock(Converter.Factory.class);
    Annotation[] annotations = new Annotation[0];

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addConverterFactory(factory)
        .build();

    doReturn(converter).when(factory).responseBodyConverter(Void.class, annotations, retrofit);

    retrofit.newBuilder().build().responseBodyConverter(Void.class, annotations);

    verifyZeroInteractions(factory);
  }

  @Test public void requestConverterFactoryQueried() {
    Type type = String.class;
    Annotation[] parameterAnnotations = new Annotation[0];
    Annotation[] methodAnnotations = new Annotation[1];

    Converter<?, RequestBody> expectedAdapter = mock(Converter.class);
    Converter.Factory factory = mock(Converter.Factory.class);

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addConverterFactory(factory)
        .build();

    doReturn(expectedAdapter).when(factory).requestBodyConverter(type, parameterAnnotations,
        methodAnnotations, retrofit);

    Converter<?, RequestBody> actualAdapter = retrofit.requestBodyConverter(type,
        parameterAnnotations, methodAnnotations);
    assertThat(actualAdapter).isSameAs(expectedAdapter);

    verify(factory).requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit);
    verifyNoMoreInteractions(factory);
  }

  @Test public void requestConverterFactoryNoMatchThrows() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    NonMatchingConverterFactory nonMatchingFactory = new NonMatchingConverterFactory();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addConverterFactory(nonMatchingFactory)
        .build();

    try {
      retrofit.requestBodyConverter(type, annotations, annotations);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(""
          + "Could not locate RequestBody converter for class java.lang.String.\n"
          + "  Tried:\n"
          + "   * retrofit2.BuiltInConverters\n"
          + "   * retrofit2.helpers.NonMatchingConverterFactory");
    }

    assertThat(nonMatchingFactory.called).isTrue();
  }

  @Test public void requestConverterFactorySkippedNoMatchThrows() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    NonMatchingConverterFactory nonMatchingFactory1 = new NonMatchingConverterFactory();
    NonMatchingConverterFactory nonMatchingFactory2 = new NonMatchingConverterFactory();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addConverterFactory(nonMatchingFactory1)
        .addConverterFactory(nonMatchingFactory2)
        .build();

    try {
      retrofit.nextRequestBodyConverter(nonMatchingFactory1, type, annotations, annotations);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(""
          + "Could not locate RequestBody converter for class java.lang.String.\n"
          + "  Skipped:\n"
          + "   * retrofit2.BuiltInConverters\n"
          + "   * retrofit2.helpers.NonMatchingConverterFactory\n"
          + "  Tried:\n"
          + "   * retrofit2.helpers.NonMatchingConverterFactory");
    }

    assertThat(nonMatchingFactory1.called).isFalse();
    assertThat(nonMatchingFactory2.called).isTrue();
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

    doReturn(expectedAdapter).when(factory).responseBodyConverter(type, annotations, retrofit);

    Converter<ResponseBody, ?> actualAdapter = retrofit.responseBodyConverter(type, annotations);
    assertThat(actualAdapter).isSameAs(expectedAdapter);

    verify(factory).responseBodyConverter(type, annotations, retrofit);
    verifyNoMoreInteractions(factory);
  }

  @Test public void responseConverterFactoryNoMatchThrows() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    NonMatchingConverterFactory nonMatchingFactory = new NonMatchingConverterFactory();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addConverterFactory(nonMatchingFactory)
        .build();

    try {
      retrofit.responseBodyConverter(type, annotations);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(""
          + "Could not locate ResponseBody converter for class java.lang.String.\n"
          + "  Tried:\n"
          + "   * retrofit2.BuiltInConverters\n"
          + "   * retrofit2.helpers.NonMatchingConverterFactory");
    }

    assertThat(nonMatchingFactory.called).isTrue();
  }

  @Test public void responseConverterFactorySkippedNoMatchThrows() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    NonMatchingConverterFactory nonMatchingFactory1 = new NonMatchingConverterFactory();
    NonMatchingConverterFactory nonMatchingFactory2 = new NonMatchingConverterFactory();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addConverterFactory(nonMatchingFactory1)
        .addConverterFactory(nonMatchingFactory2)
        .build();

    try {
      retrofit.nextResponseBodyConverter(nonMatchingFactory1, type, annotations);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(""
          + "Could not locate ResponseBody converter for class java.lang.String.\n"
          + "  Skipped:\n"
          + "   * retrofit2.BuiltInConverters\n"
          + "   * retrofit2.helpers.NonMatchingConverterFactory\n"
          + "  Tried:\n"
          + "   * retrofit2.helpers.NonMatchingConverterFactory");
    }

    assertThat(nonMatchingFactory1.called).isFalse();
    assertThat(nonMatchingFactory2.called).isTrue();
  }

  @Test public void stringConverterFactoryQueried() {
    Type type = Object.class;
    Annotation[] annotations = new Annotation[0];

    Converter<?, String> expectedConverter = mock(Converter.class);
    Converter.Factory factory = mock(Converter.Factory.class);

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addConverterFactory(factory)
        .build();

    doReturn(expectedConverter).when(factory).stringConverter(type, annotations, retrofit);

    Converter<?, String> actualConverter = retrofit.stringConverter(type, annotations);
    assertThat(actualConverter).isSameAs(expectedConverter);

    verify(factory).stringConverter(type, annotations, retrofit);
    verifyNoMoreInteractions(factory);
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

    CallAdapter<?, ?> expectedAdapter = mock(CallAdapter.class);
    CallAdapter.Factory factory = mock(CallAdapter.Factory.class);

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addCallAdapterFactory(factory)
        .build();

    doReturn(expectedAdapter).when(factory).get(type, annotations, retrofit);

    CallAdapter<?, ?> actualAdapter = retrofit.callAdapter(type, annotations);
    assertThat(actualAdapter).isSameAs(expectedAdapter);

    verify(factory).get(type, annotations, retrofit);
    verifyNoMoreInteractions(factory);
  }

  @Test public void callAdapterFactoryQueriedCanDelegate() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    CallAdapter<?, ?> expectedAdapter = mock(CallAdapter.class);
    CallAdapter.Factory factory2 = mock(CallAdapter.Factory.class);
    CallAdapter.Factory factory1 = spy(new CallAdapter.Factory() {
      @Override
      public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        return retrofit.nextCallAdapter(this, returnType, annotations);
      }
    });

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addCallAdapterFactory(factory1)
        .addCallAdapterFactory(factory2)
        .build();

    doReturn(expectedAdapter).when(factory2).get(type, annotations, retrofit);

    CallAdapter<?, ?> actualAdapter = retrofit.callAdapter(type, annotations);
    assertThat(actualAdapter).isSameAs(expectedAdapter);

    verify(factory1).get(type, annotations, retrofit);
    verifyNoMoreInteractions(factory1);
    verify(factory2).get(type, annotations, retrofit);
    verifyNoMoreInteractions(factory2);
  }

  @Test public void callAdapterFactoryQueriedCanDelegateTwiceWithoutRecursion() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    CallAdapter<?, ?> expectedAdapter = mock(CallAdapter.class);
    CallAdapter.Factory factory3 = mock(CallAdapter.Factory.class);
    CallAdapter.Factory factory2 = spy(new CallAdapter.Factory() {
      @Override
      public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        return retrofit.nextCallAdapter(this, returnType, annotations);
      }
    });
    CallAdapter.Factory factory1 = spy(new CallAdapter.Factory() {
      @Override
      public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
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

    CallAdapter<?, ?> actualAdapter = retrofit.callAdapter(type, annotations);
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

    NonMatchingCallAdapterFactory nonMatchingFactory = new NonMatchingCallAdapterFactory();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addCallAdapterFactory(nonMatchingFactory)
        .build();

    try {
      retrofit.callAdapter(type, annotations);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(""
          + "Could not locate call adapter for class java.lang.String.\n"
          + "  Tried:\n"
          + "   * retrofit2.helpers.NonMatchingCallAdapterFactory\n"
          + "   * retrofit2.DefaultCallAdapterFactory");
    }

    assertThat(nonMatchingFactory.called).isTrue();
  }

  @Test public void callAdapterFactoryDelegateNoMatchThrows() {
    Type type = String.class;
    Annotation[] annotations = new Annotation[0];

    DelegatingCallAdapterFactory delegatingFactory1 = new DelegatingCallAdapterFactory();
    DelegatingCallAdapterFactory delegatingFactory2 = new DelegatingCallAdapterFactory();
    NonMatchingCallAdapterFactory nonMatchingFactory = new NonMatchingCallAdapterFactory();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addCallAdapterFactory(delegatingFactory1)
        .addCallAdapterFactory(delegatingFactory2)
        .addCallAdapterFactory(nonMatchingFactory)
        .build();

    try {
      retrofit.callAdapter(type, annotations);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(""
          + "Could not locate call adapter for class java.lang.String.\n"
          + "  Skipped:\n"
          + "   * retrofit2.helpers.DelegatingCallAdapterFactory\n"
          + "   * retrofit2.helpers.DelegatingCallAdapterFactory\n"
          + "  Tried:\n"
          + "   * retrofit2.helpers.NonMatchingCallAdapterFactory\n"
          + "   * retrofit2.DefaultCallAdapterFactory");
    }

    assertThat(delegatingFactory1.called).isTrue();
    assertThat(delegatingFactory2.called).isTrue();
    assertThat(nonMatchingFactory.called).isTrue();
  }

  @Test public void platformAwareAdapterAbsentInCloneBuilder() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();

    assertEquals(0, retrofit.newBuilder().callAdapterFactories().size());
  }

  @Test public void callbackExecutorNullThrows() {
    try {
      new Retrofit.Builder().callbackExecutor(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("executor == null");
    }
  }

  @Test public void callbackExecutorPropagatesDefaultJvm() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .build();
    assertThat(retrofit.callbackExecutor()).isNull();
  }

  @Test public void callbackExecutorPropagatesDefaultAndroid() {
    final Executor executor = Executors.newSingleThreadExecutor();
    Platform platform = new Platform() {
      @Override Executor defaultCallbackExecutor() {
        return executor;
      }
    };
    Retrofit retrofit = new Retrofit.Builder(platform)
        .baseUrl("http://example.com/")
        .build();
    assertThat(retrofit.callbackExecutor()).isSameAs(executor);
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
      @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        latch.countDown();
      }

      @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
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
      @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        throw new AssertionError();
      }

      @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
        latch.countDown();
      }
    });
    assertTrue(latch.await(2, TimeUnit.SECONDS));

    verify(executor).execute(any(Runnable.class));
    verifyNoMoreInteractions(executor);
  }

  /** Confirm that Retrofit encodes parameters when the call is executed, and not earlier. */
  @Test public void argumentCapture() throws Exception {
    AtomicInteger i = new AtomicInteger();

    server.enqueue(new MockResponse().setBody("a"));
    server.enqueue(new MockResponse().setBody("b"));

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    MutableParameters mutableParameters = retrofit.create(MutableParameters.class);

    i.set(100);
    Call<String> call1 = mutableParameters.method(i);

    i.set(101);
    Response<String> response1 = call1.execute();

    i.set(102);
    assertEquals("a", response1.body());
    assertEquals("/?i=101", server.takeRequest().getPath());

    i.set(200);
    Call<String> call2 = call1.clone();

    i.set(201);
    Response<String> response2 = call2.execute();

    i.set(202);
    assertEquals("b", response2.body());

    assertEquals("/?i=201", server.takeRequest().getPath());
  }
}
