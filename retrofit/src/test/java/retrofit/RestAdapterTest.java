// Copyright 2013 Square, Inc.
package retrofit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import retrofit.client.Client;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.converter.ConversionException;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static retrofit.RestAdapter.LogLevel.BASIC;
import static retrofit.RestAdapter.LogLevel.FULL;
import static retrofit.RestAdapter.LogLevel.HEADERS;
import static retrofit.RestAdapter.LogLevel.HEADERS_AND_ARGS;
import static retrofit.Utils.SynchronousExecutor;

public class RestAdapterTest {
  private static final List<Header> NO_HEADERS = Collections.emptyList();
  private static final List<Header> TWO_HEADERS =
      Arrays.asList(new Header("Content-Type", "application/json"),
          new Header("Content-Length", "42"));

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

  private Client mockClient;
  private Executor mockRequestExecutor;
  private Executor mockCallbackExecutor;
  private Example example;

  @SuppressWarnings("unchecked") // Mock profiler type erasure.
  @Before public void setUp() throws Exception{
    mockClient = mock(Client.class);
    mockRequestExecutor = spy(new SynchronousExecutor());
    mockCallbackExecutor = spy(new SynchronousExecutor());

    example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setEndpoint("http://example.com")
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
      fail("Interface inheritance should not be supported.");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Interface definitions must not extend other interfaces.");
    }
  }

  @Test public void logRequestResponseBasic() throws Exception {
    final List<String> logMessages = new ArrayList<String>();
    RestAdapter.Log log = new RestAdapter.Log() {
      @Override public void log(String message) {
        logMessages.add(message);
      }
    };

    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setEndpoint("http://example.com")
        .setLog(log)
        .setLogLevel(BASIC)
        .build()
        .create(Example.class);

    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", TWO_HEADERS, new TypedString("Hi")));

    example.something();
    assertThat(logMessages).hasSize(2);
    assertThat(logMessages.get(0)).isEqualTo("---> HTTP GET http://example.com/");
    assertThat(logMessages.get(1)).matches("<--- HTTP 200 http://example.com/ \\([0-9]+ms\\)");
  }

  @Test public void logRequestResponseHeaders() throws Exception {
    final List<String> logMessages = new ArrayList<String>();
    RestAdapter.Log log = new RestAdapter.Log() {
      @Override public void log(String message) {
        logMessages.add(message);
      }
    };

    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setEndpoint("http://example.com")
        .setLog(log)
        .setLogLevel(HEADERS)
        .build()
        .create(Example.class);

    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", TWO_HEADERS, new TypedString("Hi")));

    example.something();
    assertThat(logMessages).hasSize(7);
    assertThat(logMessages.get(0)).isEqualTo("---> HTTP GET http://example.com/");
    assertThat(logMessages.get(1)).isEqualTo("Foo: Bar");
    assertThat(logMessages.get(2)).isEqualTo("---> END HTTP (no body)");
    assertThat(logMessages.get(3)).matches("<--- HTTP 200 http://example.com/ \\([0-9]+ms\\)");
    assertThat(logMessages.get(4)).isEqualTo("Content-Type: application/json");
    assertThat(logMessages.get(5)).isEqualTo("Content-Length: 42");
    assertThat(logMessages.get(6)).isEqualTo("<--- END HTTP (2-byte body)");
  }

  @Test public void logRequestResponseHeadersAndArgs() throws Exception {
    final List<String> logMessages = new ArrayList<String>();
    RestAdapter.Log log = new RestAdapter.Log() {
      @Override public void log(String message) {
        logMessages.add(message);
      }
    };

    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setEndpoint("http://example.com")
        .setLog(log)
        .setLogLevel(HEADERS_AND_ARGS)
        .build()
        .create(Example.class);

    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", TWO_HEADERS, new TypedString("Hi")));

    example.something();
    assertThat(logMessages).hasSize(9);
    assertThat(logMessages.get(0)).isEqualTo("---> HTTP GET http://example.com/");
    assertThat(logMessages.get(1)).isEqualTo("Foo: Bar");
    assertThat(logMessages.get(2)).isEqualTo("---> END HTTP (no body)");
    assertThat(logMessages.get(3)).matches("<--- HTTP 200 http://example.com/ \\([0-9]+ms\\)");
    assertThat(logMessages.get(4)).isEqualTo("Content-Type: application/json");
    assertThat(logMessages.get(5)).isEqualTo("Content-Length: 42");
    assertThat(logMessages.get(6)).isEqualTo("<--- END HTTP (2-byte body)");
    assertThat(logMessages.get(7)).isEqualTo("<--- BODY:");
    assertThat(logMessages.get(8)).isEqualTo("Hi");
  }

  @Test public void logSuccessfulRequestResponseFullWhenResponseBodyPresent() throws Exception {
    final List<String> logMessages = new ArrayList<String>();
    RestAdapter.Log log = new RestAdapter.Log() {
      @Override public void log(String message) {
        logMessages.add(message);
      }
    };

    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setEndpoint("http://example.com")
        .setLog(log)
        .setLogLevel(FULL)
        .build()
        .create(Example.class);

    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", TWO_HEADERS, new TypedString("{}")));

    example.something(new TypedString("Hi"));
    assertThat(logMessages).hasSize(13);
    assertThat(logMessages.get(0)).isEqualTo("---> HTTP POST http://example.com/");
    assertThat(logMessages.get(1)).isEqualTo("Foo: Bar");
    assertThat(logMessages.get(2)).isEqualTo("Content-Type: text/plain; charset=UTF-8");
    assertThat(logMessages.get(3)).isEqualTo("Content-Length: 2");
    assertThat(logMessages.get(4)).isEqualTo("");
    assertThat(logMessages.get(5)).isEqualTo("Hi");
    assertThat(logMessages.get(6)).isEqualTo("---> END HTTP (2-byte body)");
    assertThat(logMessages.get(7)).matches("<--- HTTP 200 http://example.com/ \\([0-9]+ms\\)");
    assertThat(logMessages.get(8)).isEqualTo("Content-Type: application/json");
    assertThat(logMessages.get(9)).isEqualTo("Content-Length: 42");
    assertThat(logMessages.get(10)).isEqualTo("");
    assertThat(logMessages.get(11)).isEqualTo("{}");
    assertThat(logMessages.get(12)).isEqualTo("<--- END HTTP (2-byte body)");
  }

  @Test public void logSuccessfulRequestResponseHeadersAndArgsWhenResponseBodyPresent() throws Exception {
    final List<String> logMessages = new ArrayList<String>();
    RestAdapter.Log log = new RestAdapter.Log() {
      @Override public void log(String message) {
        logMessages.add(message);
      }
    };

    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setEndpoint("http://example.com")
        .setLog(log)
        .setLogLevel(HEADERS_AND_ARGS)
        .build()
        .create(Example.class);

    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", TWO_HEADERS,
            new TypedString("{}")));

    example.something(new TypedString("Hi"));
    assertThat(logMessages).hasSize(13);
    assertThat(logMessages.get(0)).isEqualTo("---> HTTP POST http://example.com/");
    assertThat(logMessages.get(1)).isEqualTo("Foo: Bar");
    assertThat(logMessages.get(2)).isEqualTo("Content-Type: text/plain; charset=UTF-8");
    assertThat(logMessages.get(3)).isEqualTo("Content-Length: 2");
    assertThat(logMessages.get(4)).isEqualTo("---> REQUEST:");
    assertThat(logMessages.get(5)).isEqualTo("#0: TypedString[Hi]");
    assertThat(logMessages.get(6)).isEqualTo("---> END HTTP (2-byte body)");
    assertThat(logMessages.get(7)).matches("<--- HTTP 200 http://example.com/ \\([0-9]+ms\\)");
    assertThat(logMessages.get(8)).isEqualTo("Content-Type: application/json");
    assertThat(logMessages.get(9)).isEqualTo("Content-Length: 42");
    assertThat(logMessages.get(10)).isEqualTo("<--- END HTTP (2-byte body)");
    assertThat(logMessages.get(11)).isEqualTo("<--- BODY:");
    assertThat(logMessages.get(12)).isEqualTo("{}");
  }

  @Test public void logSuccessfulRequestResponseFullWhenResponseBodyAbsent() throws Exception {
    final List<String> logMessages = new ArrayList<String>();
    RestAdapter.Log log = new RestAdapter.Log() {
      @Override public void log(String message) {
        logMessages.add(message);
      }
    };

    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setEndpoint("http://example.com")
        .setLog(log)
        .setLogLevel(FULL)
        .build()
        .create(Example.class);

    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", TWO_HEADERS, null));

    example.something();
    assertThat(logMessages).hasSize(7);
    assertThat(logMessages.get(0)).isEqualTo("---> HTTP GET http://example.com/");
    assertThat(logMessages.get(1)).isEqualTo("Foo: Bar");
    assertThat(logMessages.get(2)).isEqualTo("---> END HTTP (no body)");
    assertThat(logMessages.get(3)).matches("<--- HTTP 200 http://example.com/ \\([0-9]+ms\\)");
    assertThat(logMessages.get(4)).isEqualTo("Content-Type: application/json");
    assertThat(logMessages.get(5)).isEqualTo("Content-Length: 42");
    assertThat(logMessages.get(6)).isEqualTo("<--- END HTTP (0-byte body)");
  }

  @Test public void logSuccessfulRequestResponseHeadersAndArgsWhenResponseBodyAbsent() throws Exception {
    final List<String> logMessages = new ArrayList<String>();
    RestAdapter.Log log = new RestAdapter.Log() {
      @Override public void log(String message) {
        logMessages.add(message);
      }
    };

    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setEndpoint("http://example.com")
        .setLog(log)
        .setLogLevel(HEADERS_AND_ARGS)
        .build()
        .create(Example.class);

    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", TWO_HEADERS, null));

    example.something(new TypedString("Hi"));
    assertThat(logMessages).hasSize(11);
    assertThat(logMessages.get(0)).isEqualTo("---> HTTP POST http://example.com/");
    assertThat(logMessages.get(1)).isEqualTo("Foo: Bar");
    assertThat(logMessages.get(2)).isEqualTo("Content-Type: text/plain; charset=UTF-8");
    assertThat(logMessages.get(3)).isEqualTo("Content-Length: 2");
    assertThat(logMessages.get(4)).isEqualTo("---> REQUEST:");
    assertThat(logMessages.get(5)).isEqualTo("#0: TypedString[Hi]");
    assertThat(logMessages.get(6)).isEqualTo("---> END HTTP (2-byte body)");
    assertThat(logMessages.get(7)).matches("<--- HTTP 200 http://example.com/ \\([0-9]+ms\\)");
    assertThat(logMessages.get(8)).isEqualTo("Content-Type: application/json");
    assertThat(logMessages.get(9)).isEqualTo("Content-Length: 42");
    assertThat(logMessages.get(10)).isEqualTo("<--- END HTTP (0-byte body)");
  }

  @Test public void successfulRequestResponseWhenMimeTypeMissing() throws Exception {
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", NO_HEADERS, NO_MIME_BODY));

    example.something();
  }

  @Test public void logSuccessfulRequestResponseFullWhenMimeTypeMissing() throws Exception {
    final List<String> logMessages = new ArrayList<String>();
    RestAdapter.Log log = new RestAdapter.Log() {
      @Override public void log(String message) {
        logMessages.add(message);
      }
    };

    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setEndpoint("http://example.com")
        .setLog(log)
        .setLogLevel(FULL)
        .build()
        .create(Example.class);

    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", TWO_HEADERS, NO_MIME_BODY));

    example.something();
    assertThat(logMessages).hasSize(9);
    assertThat(logMessages.get(0)).isEqualTo("---> HTTP GET http://example.com/");
    assertThat(logMessages.get(1)).isEqualTo("Foo: Bar");
    assertThat(logMessages.get(2)).isEqualTo("---> END HTTP (no body)");
    assertThat(logMessages.get(3)).matches("<--- HTTP 200 http://example.com/ \\([0-9]+ms\\)");
    assertThat(logMessages.get(4)).isEqualTo("Content-Type: application/json");
    assertThat(logMessages.get(5)).isEqualTo("Content-Length: 42");
    assertThat(logMessages.get(6)).isEqualTo("");
    assertThat(logMessages.get(7)).isEqualTo("Hi");
    assertThat(logMessages.get(8)).isEqualTo("<--- END HTTP (2-byte body)");
  }

  @Test public void synchronousDoesNotUseExecutors() throws Exception {
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedString("Hi")));

    example.something();

    verifyZeroInteractions(mockRequestExecutor);
    verifyZeroInteractions(mockCallbackExecutor);
  }

  @Test public void asynchronousUsesExecutors() throws Exception {
    Response response = new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedString("Hey"));
    when(mockClient.execute(any(Request.class))).thenReturn(response);
    Callback<String> callback = mock(Callback.class);

    example.something(callback);

    verify(mockRequestExecutor).execute(any(CallbackRunnable.class));
    verify(mockCallbackExecutor).execute(any(Runnable.class));
    verify(callback).success(eq("Hey"), same(response));
  }

  @Test public void malformedResponseThrowsConversionException() throws Exception {
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedString("{")));

    try {
      example.something();
      fail("RetrofitError expected on malformed response body.");
    } catch (RetrofitError e) {
      assertThat(e.getKind()).isEqualTo(RetrofitError.Kind.CONVERSION);
      assertThat(e.getResponse().getStatus()).isEqualTo(200);
      assertThat(e.getCause()).isInstanceOf(ConversionException.class);
      assertThat(e.getResponse().getBody()).isNull();
    }
  }

  @Test public void errorResponseThrowsHttpError() throws Exception {
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 500, "Internal Server Error", NO_HEADERS, null));

    try {
      example.something();
      fail("RetrofitError expected on non-2XX response code.");
    } catch (RetrofitError e) {
      assertThat(e.getKind()).isEqualTo(RetrofitError.Kind.HTTP);
      assertThat(e.getResponse().getStatus()).isEqualTo(500);
      assertThat(e.getSuccessType()).isEqualTo(String.class);
    }
  }

  @Test public void logErrorRequestResponseFullWhenMimeTypeMissing() throws Exception {
    final List<String> logMessages = new ArrayList<String>();
    RestAdapter.Log log = new RestAdapter.Log() {
      @Override public void log(String message) {
        logMessages.add(message);
      }
    };

    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setEndpoint("http://example.com")
        .setLog(log)
        .setLogLevel(FULL)
        .build()
        .create(Example.class);

    Response responseMissingMimeType = //
        new Response("http://example.com/", 403, "Forbidden", TWO_HEADERS, NO_MIME_BODY);

    when(mockClient.execute(any(Request.class))).thenReturn(responseMissingMimeType);

    try {
      example.something();
      fail("RetrofitError expected on non-2XX response code.");
    } catch (RetrofitError e) {
      assertThat(e.getResponse().getStatus()).isEqualTo(403);
    }

    assertThat(logMessages).hasSize(9);
    assertThat(logMessages.get(0)).isEqualTo("---> HTTP GET http://example.com/");
    assertThat(logMessages.get(1)).isEqualTo("Foo: Bar");
    assertThat(logMessages.get(2)).isEqualTo("---> END HTTP (no body)");
    assertThat(logMessages.get(3)).matches("<--- HTTP 403 http://example.com/ \\([0-9]+ms\\)");
    assertThat(logMessages.get(4)).isEqualTo("Content-Type: application/json");
    assertThat(logMessages.get(5)).isEqualTo("Content-Length: 42");
    assertThat(logMessages.get(6)).isEqualTo("");
    assertThat(logMessages.get(7)).isEqualTo("Hi");
    assertThat(logMessages.get(8)).isEqualTo("<--- END HTTP (2-byte body)");
  }

  @Test public void logErrorRequestResponseFullWhenResponseBodyAbsent() throws Exception {
    final List<String> logMessages = new ArrayList<String>();
    RestAdapter.Log log = new RestAdapter.Log() {
      @Override public void log(String message) {
        logMessages.add(message);
      }
    };

    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setEndpoint("http://example.com")
        .setLog(log)
        .setLogLevel(FULL)
        .build()
        .create(Example.class);

    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 500, "Internal Server Error", TWO_HEADERS, null));

    try {
      example.something();
      fail("RetrofitError expected on non-2XX response code.");
    } catch (RetrofitError e) {
      assertThat(e.getResponse().getStatus()).isEqualTo(500);
    }

    assertThat(logMessages).hasSize(7);
    assertThat(logMessages.get(0)).isEqualTo("---> HTTP GET http://example.com/");
    assertThat(logMessages.get(1)).isEqualTo("Foo: Bar");
    assertThat(logMessages.get(2)).isEqualTo("---> END HTTP (no body)");
    assertThat(logMessages.get(3)).matches("<--- HTTP 500 http://example.com/ \\([0-9]+ms\\)");
    assertThat(logMessages.get(4)).isEqualTo("Content-Type: application/json");
    assertThat(logMessages.get(5)).isEqualTo("Content-Length: 42");
    assertThat(logMessages.get(6)).isEqualTo("<--- END HTTP (0-byte body)");
  }

  @Test public void clientExceptionThrowsNetworkError() throws Exception {
    IOException exception = new IOException("I'm broken!");
    when(mockClient.execute(any(Request.class))).thenThrow(exception);

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

    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", NO_HEADERS, body));

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
    when(mockClient.execute(any(Request.class))).thenThrow(exception);

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
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(response);
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

    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", NO_HEADERS, in));

    Response response = example.streaming();
    assertThat(response.getBody().in()).isSameAs(is);
  }

  @Test public void closeInputStream() throws IOException {
    // Set logger and profiler on example to make sure we exercise all the code paths.
    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setEndpoint("http://example.com")
        .setLog(RestAdapter.Log.NONE)
        .setLogLevel(FULL)
        .build()
        .create(Example.class);

    ByteArrayInputStream is = spy(new ByteArrayInputStream("hello".getBytes()));
    TypedInput typedInput = mock(TypedInput.class);
    when(typedInput.in()).thenReturn(is);
    Response response = new Response("http://example.com/", 200, "OK", NO_HEADERS, typedInput);
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(response);
    example.something();
    verify(is).close();
  }

  @Test public void getResponseDirectlyAsync() throws Exception {
    Response response = new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedString("Hey"));
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(response);
    Callback<Response> callback = mock(Callback.class);

    example.direct(callback);

    verify(mockRequestExecutor).execute(any(CallbackRunnable.class));
    verify(mockCallbackExecutor).execute(any(Runnable.class));
    verify(callback).success(eq(response), same(response));
  }

  @Test public void getAsync() throws Exception {
    Response response = new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedString("Hey"));
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(response);
    Callback<String> callback = mock(Callback.class);

    example.something(callback);

    verify(mockRequestExecutor).execute(any(CallbackRunnable.class));
    verify(mockCallbackExecutor).execute(any(Runnable.class));

    ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
    verify(callback).success(responseCaptor.capture(), same(response));
    assertThat(responseCaptor.getValue()).isEqualTo("Hey");
  }


  @Test public void errorAsync() throws Exception {
    Response response = new Response("http://example.com/", 500, "Broken!", NO_HEADERS, new TypedString("Hey"));
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(response);
    Callback<String> callback = mock(Callback.class);

    example.something(callback);

    verify(mockRequestExecutor).execute(any(CallbackRunnable.class));
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
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedString("hello")));
    Action1<String> action = mock(Action1.class);
    example.observable("Howdy").subscribe(action);
    verify(action).call(eq("hello"));
  }

  @Test public void observableCallsOnError() throws Exception {
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 300, "FAIL", NO_HEADERS, new TypedString("bummer")));
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
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    when(mockClient.execute(requestCaptor.capture())) //
        .thenReturn(new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedString("hello")));
    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    Action1<Response> action = mock(Action1.class);
    example.observable("X", "Y").subscribe(action);

    Request request = requestCaptor.getValue();
    assertThat(request.getUrl()).contains("/X/Y");

    verify(action).call(responseCaptor.capture());
    Response response = responseCaptor.getValue();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test public void observableUsesHttpExecutor() throws IOException {
    Response response = new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedString("hello"));
    when(mockClient.execute(any(Request.class))).thenReturn(response);

    example.observable("Howdy").subscribe(mock(Action1.class));

    verify(mockRequestExecutor, atLeastOnce()).execute(any(Runnable.class));
    verifyZeroInteractions(mockCallbackExecutor);
  }
}
