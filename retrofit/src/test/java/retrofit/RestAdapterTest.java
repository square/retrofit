// Copyright 2013 Square, Inc.
package retrofit;

import com.google.gson.JsonParseException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import retrofit.client.MockClient;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Streaming;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;
import retrofit.mime.TypedString;
import rx.Observable;
import rx.functions.Action1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static retrofit.Utils.SynchronousExecutor;

public class RestAdapterTest {
  private static final com.squareup.okhttp.Headers NO_HEADERS = com.squareup.okhttp.Headers.of();
  private static final com.squareup.okhttp.Headers TWO_HEADERS =
      new com.squareup.okhttp.Headers.Builder()
          .add("Content-Type", "application/json")
          .add("Content-Length", "42")
          .build();

  /** Not all servers play nice and add content-type headers to responses. */
  private static final TypedInput NO_MIME_BODY = new TypedInput() {
    @Override public String mimeType() {
      return null;
    }

    @Override public long length() {
      return 2;
    }

    @Override public InputStream in() throws IOException {
      return new ByteArrayInputStream("Hi".getBytes("UTF-8"));
    }
  };

  private interface Example {
    @Headers("Foo: Bar")
    @GET("/") String something();
    @Headers("Foo: Bar")
    @POST("/") Object something(@Body TypedOutput body);
    @GET("/") void something(Callback<String> callback);
    @GET("/") Response direct();
    @GET("/") void direct(Callback<Response> callback);
    @GET("/") @Streaming Response streaming();
    @POST("/") Observable<String> observable(@Body String body);
    @POST("/{x}/{y}") Observable<Response> observable(@Path("x") String x, @Path("y") String y);
  }
  private interface InvalidExample extends Example {
  }

  private final MockClient mockClient = new MockClient();
  private final Executor mockCallbackExecutor = spy(new SynchronousExecutor());
  private final Example example = new RestAdapter.Builder() //
      .setClient(mockClient)
      .setCallbackExecutor(mockCallbackExecutor)
      .setEndpoint("http://example.com")
      .build()
      .create(Example.class);

  @Test public void objectMethodsStillWork() {
    assertThat(example.hashCode()).isNotZero();
    assertThat(example.equals(this)).isFalse();
    assertThat(example.toString()).isNotEmpty();
  }

  @Test public void interfaceWithExtendIsNotSupported() {
    try {
      new RestAdapter.Builder().setEndpoint("http://foo/").build().create(InvalidExample.class);
      fail("Interface inheritance should not be supported.");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Interface definitions must not extend other interfaces.");
    }
  }

  @Test public void successfulRequestResponseWhenMimeTypeMissing() throws Exception {
    mockClient.enqueueResponse(
        new Response("http://example.com/", 200, "OK", NO_HEADERS, NO_MIME_BODY));

    example.something();
  }

  @Test public void asynchronousUsesCalbackExecutor() throws Exception {
    Response response = new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedString("Hey"));
    mockClient.enqueueResponse(response);
    Callback<String> callback = mock(Callback.class);

    example.something(callback);

    verify(mockCallbackExecutor).execute(any(Runnable.class));
    verify(callback).success(eq("Hey"), same(response));
  }

  @Test public void malformedResponseThrowsConversionException() throws Exception {
    mockClient.enqueueResponse(
        new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedString("{")));

    try {
      example.something();
      fail("RetrofitError expected on malformed response body.");
    } catch (RetrofitError e) {
      assertThat(e.getKind()).isEqualTo(RetrofitError.Kind.UNEXPECTED);
      assertThat(e.getResponse().getStatus()).isEqualTo(200);
      assertThat(e.getCause()).isInstanceOf(JsonParseException.class);
      assertThat(e.getResponse().getBody()).isNull();
    }
  }

  @Test public void errorResponseThrowsHttpError() throws Exception {
    mockClient.enqueueResponse(
        new Response("http://example.com/", 500, "Internal Server Error", NO_HEADERS, null));

    try {
      example.something();
      fail("RetrofitError expected on non-2XX response code.");
    } catch (RetrofitError e) {
      assertThat(e.getKind()).isEqualTo(RetrofitError.Kind.HTTP);
      assertThat(e.getResponse().getStatus()).isEqualTo(500);
      assertThat(e.getSuccessType()).isEqualTo(String.class);
    }
  }

  @Test public void clientExceptionThrowsNetworkError() throws Exception {
    IOException exception = new IOException("I'm broken!");
    mockClient.enqueueIOException(exception);

    try {
      example.something();
      fail("RetrofitError expected when client throws exception.");
    } catch (RetrofitError e) {
      assertThat(e.getKind()).isEqualTo(RetrofitError.Kind.NETWORK);
      assertThat(e.getCause()).isSameAs(exception);
    }
  }

  @Test public void bodyTypedInputExceptionThrowsNetworkError() throws Exception {
    TypedInput body = spy(new TypedString("{}"));
    InputStream bodyStream = mock(InputStream.class, new Answer() {
      @Override public Object answer(InvocationOnMock invocation) throws Throwable {
        throw new IOException("I'm broken!");
      }
    });
    doReturn(bodyStream).when(body).in();

    mockClient.enqueueResponse(new Response("http://example.com/", 200, "OK", NO_HEADERS, body));

    try {
      example.something();
      fail("RetrofitError expected on malformed response body.");
    } catch (RetrofitError e) {
      assertThat(e.getKind()).isEqualTo(RetrofitError.Kind.NETWORK);
      assertThat(e.getCause()).isInstanceOf(IOException.class);
      assertThat(e.getCause()).hasMessage("I'm broken!");
    }
  }

  @Test public void unexpectedExceptionThrows() throws IOException {
    RuntimeException exception = new RuntimeException("More breakage.");
    mockClient.enqueueUnexpectedException(exception);

    try {
      example.something();
      fail("RetrofitError expected when unexpected exception thrown.");
    } catch (RetrofitError e) {
      assertThat(e.getKind()).isEqualTo(RetrofitError.Kind.UNEXPECTED);
      assertThat(e.getCause()).isSameAs(exception);
    }
  }

  @Test public void getResponseDirectly() throws Exception {
    Response response = new Response("http://example.com/", 200, "OK", NO_HEADERS, null);
    mockClient.enqueueResponse(response);
    assertThat(example.direct()).isSameAs(response);
  }

  @Test public void streamingResponse() throws Exception {
    final InputStream is = new ByteArrayInputStream("Hey".getBytes("UTF-8"));
    TypedInput in = new TypedInput() {
      @Override public String mimeType() {
        return "text/string";
      }

      @Override public long length() {
        return 3;
      }

      @Override public InputStream in() throws IOException {
        return is;
      }
    };

    mockClient.enqueueResponse(new Response("http://example.com/", 200, "OK", NO_HEADERS, in));

    Response response = example.streaming();
    assertThat(response.getBody().in()).isSameAs(is);
  }

  @Test public void closeInputStream() throws IOException {
    // Set logger and profiler on example to make sure we exercise all the code paths.
    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setCallbackExecutor(mockCallbackExecutor)
        .setEndpoint("http://example.com")
        .build()
        .create(Example.class);

    ByteArrayInputStream is = spy(new ByteArrayInputStream("hello".getBytes()));
    TypedInput typedInput = mock(TypedInput.class);
    when(typedInput.in()).thenReturn(is);
    Response response = new Response("http://example.com/", 200, "OK", NO_HEADERS, typedInput);
    mockClient.enqueueResponse(response);
    example.something();
    verify(is).close();
  }

  @Test public void getResponseDirectlyAsync() throws Exception {
    Response response = new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedString("Hey"));
    mockClient.enqueueResponse(response);
    Callback<Response> callback = mock(Callback.class);

    example.direct(callback);

    verify(mockCallbackExecutor).execute(any(Runnable.class));
    verify(callback).success(eq(response), same(response));
  }

  @Test public void getAsync() throws Exception {
    Response response = new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedString("Hey"));
    mockClient.enqueueResponse(response);
    Callback<String> callback = mock(Callback.class);

    example.something(callback);

    verify(mockCallbackExecutor).execute(any(Runnable.class));

    ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
    verify(callback).success(responseCaptor.capture(), same(response));
    assertThat(responseCaptor.getValue()).isEqualTo("Hey");
  }


  @Test public void errorAsync() throws Exception {
    Response response = new Response("http://example.com/", 500, "Broken!", NO_HEADERS, new TypedString("Hey"));
    mockClient.enqueueResponse(response);
    Callback<String> callback = mock(Callback.class);

    example.something(callback);

    verify(mockCallbackExecutor).execute(any(Runnable.class));

    ArgumentCaptor<RetrofitError> errorCaptor = ArgumentCaptor.forClass(RetrofitError.class);
    verify(callback).failure(errorCaptor.capture());
    RetrofitError error = errorCaptor.getValue();
    assertThat(error.getResponse().getStatus()).isEqualTo(500);
    assertThat(error.getResponse().getReason()).isEqualTo("Broken!");
    assertThat(error.getSuccessType()).isEqualTo(String.class);
    assertThat(error.getBody()).isEqualTo("Hey");
  }

  @Test public void observableCallsOnNext() throws Exception {
    mockClient.enqueueResponse(
        new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedString("hello")));
    Action1<String> action = mock(Action1.class);
    example.observable("Howdy").subscribe(action);
    verify(action).call(eq("hello"));
  }

  @Test public void observableCallsOnError() throws Exception {
    mockClient.enqueueResponse(
        new Response("http://example.com/", 300, "FAIL", NO_HEADERS, new TypedString("bummer")));
    Action1<String> onSuccess = mock(Action1.class);
    Action1<Throwable> onError = mock(Action1.class);
    example.observable("Howdy").subscribe(onSuccess, onError);
    verifyZeroInteractions(onSuccess);

    ArgumentCaptor<RetrofitError> errorCaptor = ArgumentCaptor.forClass(RetrofitError.class);
    verify(onError).call(errorCaptor.capture());
    RetrofitError value = errorCaptor.getValue();
    assertThat(value.getSuccessType()).isEqualTo(String.class);
  }

  @Test public void observableHandlesParams() throws Exception {
    mockClient.enqueueResponse(
        new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedString("hello")));
    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    Action1<Response> action = mock(Action1.class);
    example.observable("X", "Y").subscribe(action);

    Request request = mockClient.takeRequest();
    assertThat(request.getUrl()).contains("/X/Y");

    verify(action).call(responseCaptor.capture());
    Response response = responseCaptor.getValue();
    assertThat(response.getStatus()).isEqualTo(200);
  }
}
