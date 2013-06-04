// Copyright 2013 Square, Inc.
package retrofit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import retrofit.converter.ConversionException;
import retrofit.http.GET;
import retrofit.client.Client;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedString;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static retrofit.Profiler.RequestInformation;
import static retrofit.Utils.SynchronousExecutor;

public class RestAdapterTest {
  private static List<Header> NO_HEADERS = Collections.emptyList();

  private interface Example {
    @GET("/") Object something();
    @GET("/") void something(Callback<Object> callback);
    @GET("/") Response direct();
    @GET("/") void direct(Callback<Response> callback);
  }

  private Client mockClient;
  private Executor mockRequestExecutor;
  private Executor mockCallbackExecutor;
  private Profiler<Object> mockProfiler;
  private Example example;

  @SuppressWarnings("unchecked") // Mock profiler type erasure.
  @Before public void setUp() throws Exception{
    mockClient = mock(Client.class);
    mockRequestExecutor = spy(new SynchronousExecutor());
    mockCallbackExecutor = spy(new SynchronousExecutor());
    mockProfiler = mock(Profiler.class);

    example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setServer("http://example.com")
        .setProfiler(mockProfiler)
        .build()
        .create(Example.class);
  }

  @Test public void objectMethodsStillWork() {
    assertThat(example.hashCode()).isNotZero();
    assertThat(example.equals(this)).isFalse();
    assertThat(example.toString()).isNotEmpty();
  }

  @Test public void profilerObjectPassThrough() throws Exception {
    Object data = new Object();
    when(mockProfiler.beforeCall()).thenReturn(data);
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response(200, "OK", NO_HEADERS, null));

    example.something();

    verify(mockProfiler).beforeCall();
    verify(mockClient).execute(any(Request.class));
    verify(mockProfiler).afterCall(any(RequestInformation.class), anyInt(), eq(200), same(data));
  }

  @Test public void logSuccessfulRequestResponseOnDebugWhenResponseBodyPresent() throws Exception {
    final List<String> logMessages = new ArrayList<String>();
    RestAdapter.Log log = new RestAdapter.Log() {
      public void log(String message) {
        logMessages.add(message);
      }
    };

    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setServer("http://example.com")
        .setProfiler(mockProfiler)
        .setLog(log)
        .setDebug(true)
        .build()
        .create(Example.class);

    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response(200, "OK", NO_HEADERS, new TypedString("{}")));

    example.something();
    assertThat(logMessages).hasSize(5);
    assertThat(logMessages.get(0)).isEqualTo("---> HTTP GET http://example.com/");
    assertThat(logMessages.get(1)).isEqualTo("---> END HTTP (0-byte body)");
    assertThat(logMessages.get(2)).matches("<--- HTTP 200 http://example.com/ \\([0-9]+ms\\)");
    assertThat(logMessages.get(3)).isEqualTo("{}");
    assertThat(logMessages.get(4)).isEqualTo("<--- END HTTP (2-byte body)");
  }

  @Test public void logSuccessfulRequestResponseOnDebugWhenResponseBodyAbsent() throws Exception {
    final List<String> logMessages = new ArrayList<String>();
    RestAdapter.Log log = new RestAdapter.Log() {
      public void log(String message) {
        logMessages.add(message);
      }
    };

    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setServer("http://example.com")
        .setProfiler(mockProfiler)
        .setLog(log)
        .setDebug(true)
        .build()
        .create(Example.class);

    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response(200, "OK", NO_HEADERS, null));

    example.something();
    assertThat(logMessages).hasSize(4);
    assertThat(logMessages.get(0)).isEqualTo("---> HTTP GET http://example.com/");
    assertThat(logMessages.get(1)).isEqualTo("---> END HTTP (0-byte body)");
    assertThat(logMessages.get(2)).matches("<--- HTTP 200 http://example.com/ \\([0-9]+ms\\)");
    assertThat(logMessages.get(3)).isEqualTo("<--- END HTTP (0-byte body)");
  }

  /** Not all servers play nice and add content-type headers to responses. */
  TypedInput inputMissingMimeType = new TypedInput() {

    @Override public String mimeType() {
      return null;
    }

    @Override public long length() {
      return 2;
    }

    @Override public InputStream in() throws IOException {
      return new ByteArrayInputStream("{}".getBytes());
    }
  };

  @Test public void successfulRequestResponseWhenMimeTypeMissing() throws Exception {
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response(200, "OK", NO_HEADERS, inputMissingMimeType));

    example.something();
  }

  @Test public void logSuccessfulRequestResponseOnDebugWhenMimeTypeMissing() throws Exception {
    final List<String> logMessages = new ArrayList<String>();
    RestAdapter.Log log = new RestAdapter.Log() {
      public void log(String message) {
        logMessages.add(message);
      }
    };

    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setServer("http://example.com")
        .setProfiler(mockProfiler)
        .setLog(log)
        .setDebug(true)
        .build()
        .create(Example.class);

    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response(200, "OK", NO_HEADERS, inputMissingMimeType));

    example.something();
    assertThat(logMessages).hasSize(5);
    assertThat(logMessages.get(0)).isEqualTo("---> HTTP GET http://example.com/");
    assertThat(logMessages.get(1)).isEqualTo("---> END HTTP (0-byte body)");
    assertThat(logMessages.get(2)).matches("<--- HTTP 200 http://example.com/ \\([0-9]+ms\\)");
    assertThat(logMessages.get(3)).isEqualTo("{}");
    assertThat(logMessages.get(4)).isEqualTo("<--- END HTTP (2-byte body)");
  }

  @Test public void synchronousDoesNotUseExecutors() throws Exception {
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response(200, "OK", NO_HEADERS, null));

    example.something();

    verifyZeroInteractions(mockRequestExecutor);
    verifyZeroInteractions(mockCallbackExecutor);
  }

  @Test public void asynchronousUsesExecutors() throws Exception {
    Response response = new Response(200, "OK", NO_HEADERS, new TypedString("{}"));
    when(mockClient.execute(any(Request.class))).thenReturn(response);
    Callback<Object> callback = mock(Callback.class);

    example.something(callback);

    verify(mockRequestExecutor).execute(any(CallbackRunnable.class));
    verify(mockCallbackExecutor).execute(any(Runnable.class));
    verify(callback).success(anyString(), same(response));
  }

  @Test public void malformedResponseThrowsConversionException() throws Exception {
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response(200, "OK", NO_HEADERS, new TypedString("{")));

    try {
      example.something();
      fail("RetrofitError expected on malformed response body.");
    } catch (RetrofitError e) {
      assertThat(e.getResponse().getStatus()).isEqualTo(200);
      assertThat(e.getCause()).isInstanceOf(ConversionException.class);
      assertThat(e.getResponse().getBody()).isNull();
    }
  }

  @Test public void errorResponseThrowsHttpError() throws Exception {
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response(500, "Internal Server Error", NO_HEADERS, null));

    try {
      example.something();
      fail("RetrofitError expected on non-2XX response code.");
    } catch (RetrofitError e) {
      assertThat(e.getResponse().getStatus()).isEqualTo(500);
    }
  }

  @Test public void logErrorRequestResponseOnDebugWhenMimeTypeMissing() throws Exception {
    final List<String> logMessages = new ArrayList<String>();
    RestAdapter.Log log = new RestAdapter.Log() {
      public void log(String message) {
        logMessages.add(message);
      }
    };

    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setServer("http://example.com")
        .setProfiler(mockProfiler)
        .setLog(log)
        .setDebug(true)
        .build()
        .create(Example.class);

    Response responseMissingMimeType = //
        new Response(403, "Forbidden", NO_HEADERS, inputMissingMimeType);

    when(mockClient.execute(any(Request.class))).thenReturn(responseMissingMimeType);

    try {
      example.something();
      fail("RetrofitError expected on non-2XX response code.");
    } catch (RetrofitError e) {
      assertThat(e.getResponse().getStatus()).isEqualTo(403);
    }

    assertThat(logMessages.size()).isEqualTo(5);
    assertThat(logMessages.get(0)).isEqualTo("---> HTTP GET http://example.com/");
    assertThat(logMessages.get(1)).isEqualTo("---> END HTTP (0-byte body)");
    assertThat(logMessages.get(2)).matches("<--- HTTP 403 http://example.com/ \\([0-9]+ms\\)");
    assertThat(logMessages.get(3)).isEqualTo("{}");
    assertThat(logMessages.get(4)).isEqualTo("<--- END HTTP (2-byte body)");
  }

  @Test public void logErrorRequestResponseOnDebugWhenResponseBodyAbsent() throws Exception {
    final List<String> logMessages = new ArrayList<String>();
    RestAdapter.Log log = new RestAdapter.Log() {
      public void log(String message) {
        logMessages.add(message);
      }
    };

    Example example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setServer("http://example.com")
        .setProfiler(mockProfiler)
        .setLog(log)
        .setDebug(true)
        .build()
        .create(Example.class);

    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response(500, "Internal Server Error", NO_HEADERS, null));

    try {
      example.something();
      fail("RetrofitError expected on non-2XX response code.");
    } catch (RetrofitError e) {
      assertThat(e.getResponse().getStatus()).isEqualTo(500);
    }

    assertThat(logMessages.size()).isEqualTo(4);
    assertThat(logMessages.get(0)).isEqualTo("---> HTTP GET http://example.com/");
    assertThat(logMessages.get(1)).isEqualTo("---> END HTTP (0-byte body)");
    assertThat(logMessages.get(2)).matches("<--- HTTP 500 http://example.com/ \\([0-9]+ms\\)");
    assertThat(logMessages.get(3)).isEqualTo("<--- END HTTP (0-byte body)");
  }

  @Test public void clientExceptionThrowsNetworkError() throws Exception{
    IOException exception = new IOException("I'm broken.");
    when(mockClient.execute(any(Request.class))).thenThrow(exception);

    try {
      example.something();
      fail("RetrofitError expected when client throws exception.");
    } catch (RetrofitError e) {
      assertThat(e.getCause()).isSameAs(exception);
    }
  }

  @Test public void unexpectedExceptionThrows() {
    RuntimeException exception = new RuntimeException("More breakage.");
    when(mockProfiler.beforeCall()).thenThrow(exception);

    try {
      example.something();
      fail("RetrofitError expected when unexpected exception thrown.");
    } catch (RetrofitError e) {
      assertThat(e.getCause()).isSameAs(exception);
    }
  }

  @Test public void getResponseDirectly() throws Exception {
    Response response = new Response(200, "OK", NO_HEADERS, null);
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(response);
    assertThat(example.direct()).isSameAs(response);
  }

  @Test public void getResponseDirectlyAsync() throws Exception {
    Response response = new Response(200, "OK", NO_HEADERS, null);
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(response);
    Callback<Response> callback = mock(Callback.class);

    example.direct(callback);

    verify(mockRequestExecutor).execute(any(CallbackRunnable.class));
    verify(mockCallbackExecutor).execute(any(Runnable.class));
    verify(callback).success(eq(response), same(response));
  }
}
