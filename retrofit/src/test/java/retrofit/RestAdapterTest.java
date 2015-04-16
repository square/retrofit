// Copyright 2013 Square, Inc.
package retrofit;

import com.google.gson.JsonParseException;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.rule.MockWebServerRule;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit.RequestInterceptor;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Streaming;
import rx.Observable;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import static com.squareup.okhttp.mockwebserver.SocketPolicy.DISCONNECT_AT_START;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static retrofit.Utils.SynchronousExecutor;

public class RestAdapterTest {
  private interface Example {
    @Headers("Foo: Bar")
    @GET("/") String something();
    @Headers("Foo: Bar")
    @GET("/") void something(Callback<String> callback);
    @GET("/") Response direct();
    @GET("/") void direct(Callback<Response> callback);
    @GET("/") @Streaming Response streaming();
    @POST("/") Observable<String> observable(@Body String body);
  }
  private interface InvalidExample extends Example {
  }

  @Rule public final MockWebServerRule server = new MockWebServerRule();

  private Example example;

  @Before public void setUp() {
    OkHttpClient client = new OkHttpClient();

    example = new RestAdapter.Builder() //
        .setClient(client)
        .setCallbackExecutor(new SynchronousExecutor())
        .setEndpoint(server.getUrl("/").toString())
        .build()
        .create(Example.class);
  }

  @Test public void objectMethodsStillWork() {
    assertThat(example.hashCode()).isNotZero();
    assertThat(example.equals(this)).isFalse();
    assertThat(example.toString()).isNotEmpty();
  }

  @Test public void interfaceWithExtendIsNotSupported() {
    try {
      new RestAdapter.Builder().setEndpoint("http://foo/").build().create(InvalidExample.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Interface definitions must not extend other interfaces.");
    }
  }

  @Test public void successfulRequestResponseWhenMimeTypeMissing() throws Exception {
    server.enqueue(new MockResponse().setBody("Hi").removeHeader("Content-Type"));
    String string = example.something();
    assertThat(string).isEqualTo("Hi");
  }

  @Test public void malformedResponseThrowsConversionException() throws Exception {
    server.enqueue(new MockResponse().setBody("{"));
    try {
      example.something();
      fail();
    } catch (RetrofitError e) {
      assertThat(e.getKind()).isEqualTo(RetrofitError.Kind.UNEXPECTED);
      assertThat(e.getResponse().code()).isEqualTo(200);
      assertThat(e.getCause()).isInstanceOf(JsonParseException.class);
      assertThat(e.getResponse().body()).isNull();
    }
  }

  @Test public void errorResponseThrowsHttpError() throws Exception {
    server.enqueue(new MockResponse().setStatus("HTTP/1.1 500 Broken"));

    try {
      example.something();
      fail();
    } catch (RetrofitError e) {
      assertThat(e.getKind()).isEqualTo(RetrofitError.Kind.HTTP);
      assertThat(e.getResponse().code()).isEqualTo(500);
      assertThat(e.getSuccessType()).isEqualTo(String.class);
    }
  }

  @Test public void clientExceptionThrowsNetworkError() throws Exception {
    server.enqueue(new MockResponse().setBody("Hi").setSocketPolicy(DISCONNECT_AT_START));

    try {
      example.something();
      fail();
    } catch (RetrofitError e) {
      assertThat(e.getKind()).isEqualTo(RetrofitError.Kind.NETWORK);
    }
  }

  @Test public void getResponseDirectly() throws Exception {
    server.enqueue(new MockResponse().setBody("Hey"));
    Response response = example.direct();
    assertThat(response.body().string()).isEqualTo("Hey");
  }

  @Test public void streamingResponse() throws Exception {
    server.enqueue(new MockResponse().setBody("Hey").setBodyDelay(500, MILLISECONDS));
    Response response = example.streaming();
    long startNs = System.nanoTime();
    response.body().string();
    long tookNs = System.nanoTime() - startNs;
    assertThat(tookNs).isGreaterThanOrEqualTo(500);
  }

  @Test public void getResponseDirectlyAsync() throws Exception {
    server.enqueue(new MockResponse().setBody("Hey"));

    final AtomicReference<Response> responseRef = new AtomicReference<Response>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.direct(new Callback<Response>() {
      @Override public void success(Response response, Response response2) {
        responseRef.set(response);
        latch.countDown();
      }

      @Override public void failure(RetrofitError error) {
        throw new AssertionError();
      }
    });
    assertTrue(latch.await(1, TimeUnit.SECONDS));

    assertThat(responseRef.get().body().string()).isEqualTo("Hey");
  }

  @Test public void getAsync() throws Exception {
    server.enqueue(new MockResponse().setBody("Hey"));

    final AtomicReference<String> bodyRef = new AtomicReference<String>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.something(new Callback<String>() {
      @Override public void success(String body, Response response) {
        bodyRef.set(body);
        latch.countDown();
      }

      @Override public void failure(RetrofitError error) {
        throw new AssertionError();
      }
    });
    assertTrue(latch.await(1, TimeUnit.SECONDS));

    assertThat(bodyRef.get()).isEqualTo("Hey");
  }

  @Test public void errorAsync() throws Exception {
    server.enqueue(new MockResponse().setStatus("HTTP/1.1 500 Broken!").setBody("Hey"));

    final AtomicReference<RetrofitError> errorRef = new AtomicReference<RetrofitError>();
    final CountDownLatch latch = new CountDownLatch(1);
    example.something(new Callback<String>() {
      @Override public void success(String s, Response response) {
        throw new AssertionError();
      }

      @Override public void failure(RetrofitError error) {
        errorRef.set(error);
        latch.countDown();
      }
    });
    assertTrue(latch.await(1, TimeUnit.SECONDS));

    RetrofitError error = errorRef.get();
    assertThat(error.getResponse().code()).isEqualTo(500);
    assertThat(error.getResponse().message()).isEqualTo("Broken!");
    assertThat(error.getSuccessType()).isEqualTo(String.class);
    assertThat(error.getBody()).isEqualTo("Hey");
  }

  @Test public void observableCallsOnNext() throws Exception {
    server.enqueue(new MockResponse().setBody("hello"));

    final TestSubscriber subscriber = new TestSubscriber();
    example.observable("Howdy").subscribe(subscriber);

    subscriber.awaitTerminalEvent(1, TimeUnit.SECONDS);
    assertThat(subscriber.getOnNextEvents()).containsExactly("hello");
    subscriber.assertNoErrors();
  }

  @Test public void observableCallsOnError() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500));

    final TestSubscriber subscriber = new TestSubscriber();
    example.observable("Howdy").subscribe(subscriber);

    subscriber.awaitTerminalEvent(1, TimeUnit.SECONDS);

    RetrofitError error = (RetrofitError) subscriber.getOnErrorEvents().get(0);
    assertThat(error.getResponse().code()).isEqualTo(500);
    assertThat(error.getSuccessType()).isEqualTo(String.class);
    assertThat(subscriber.getOnErrorEvents()).hasSize(1);
  }

  @Test public void observableDefersInterceptionUntilSubscription() throws Exception {
    server.enqueue(new MockResponse().setBody("hello"));

    final TestSubscriber subscriber = new TestSubscriber();
    final Thread testThread = Thread.currentThread();

    final Example interceptorExample = new RestAdapter.Builder()
        .setEndpoint(server.getUrl("/").toString())
        .setRequestInterceptor(new RequestInterceptor() {
          @Override
          public void intercept(RequestInterceptor.RequestFacade request) {
            assertThat(Thread.currentThread()).isNotEqualTo(testThread);
            request.addHeader("Nyan", "Cat");
          }
        })
        .build()
        .create(Example.class);

    final Observable<String> observable = interceptorExample.observable("Howdy")
        .subscribeOn(Schedulers.newThread());

    observable.subscribe(subscriber);
    subscriber.awaitTerminalEvent(1, TimeUnit.SECONDS);

    assertThat(subscriber.getOnNextEvents()).containsExactly("hello");
    assertThat(server.takeRequest().getHeader("Nyan")).isEqualTo("Cat");
  }

  @Test public void observableInterceptionDefaultsToCurrentThread() throws Exception {
    server.enqueue(new MockResponse().setBody("hello"));

    final TestSubscriber subscriber = new TestSubscriber();
    final Thread testThread = Thread.currentThread();

    final Example interceptorExample = new RestAdapter.Builder()
        .setEndpoint(server.getUrl("/").toString())
        .setRequestInterceptor(new RequestInterceptor() {
          @Override
          public void intercept(RequestInterceptor.RequestFacade request) {
            assertThat(Thread.currentThread()).isEqualTo(testThread);
            request.addHeader("Nyan", "Cat");
          }
        })
        .build()
        .create(Example.class);

    final Observable<String> observable = interceptorExample.observable("Howdy");

    observable.subscribe(subscriber);
    subscriber.awaitTerminalEvent(1, TimeUnit.SECONDS);

    assertThat(subscriber.getOnNextEvents()).containsExactly("hello");
    assertThat(server.takeRequest().getHeader("Nyan")).isEqualTo("Cat");
  }
}
