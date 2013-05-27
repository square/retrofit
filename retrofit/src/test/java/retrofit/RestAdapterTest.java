// Copyright 2013 Square, Inc.
package retrofit;

import java.io.IOException;
import java.net.URI;
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
import retrofit.http.POST;
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

  @Test public void synchronousDoesNotUseExecutors() throws Exception {
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response(200, "OK", NO_HEADERS, null));

    example.something();

    verifyZeroInteractions(mockRequestExecutor);
    verifyZeroInteractions(mockCallbackExecutor);
  }

  @Test public void asynchronousUsesExecutors() throws Exception {
    Response response = new Response(200, "OK", NO_HEADERS, new TypedString("{}"));
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(response);
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

  private interface Rest {
    @POST("/") URI create();
    // TODO: it will probably break things to force trailing slash
    @GET("/") Object get(URI id);
    // TODO: should be possible to specify void return type
    @POST("/kick") Void kick(URI id);
  }

  @Test public void locationHeaderWhenNoBody() throws Exception {
    Header location = new Header("location", "http://example.com/resource/1");
    Response response = //
        new Response(201, "Created", Collections.singletonList(location), null);
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(response);

    Rest rest = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setServer("http://example.com")
        .setProfiler(mockProfiler)
        .build()
        .create(Rest.class);

    assertThat(rest.create()).isEqualTo(URI.create(location.getValue()));
  }

  @Test public void overrideServerURL() throws Exception {
    final URI override = URI.create("http://example.com/resource/1");

    mockClient = new Client(){
      public Response execute(Request request) throws IOException {
        //TODO: it will probably break things to force trailing slash
        assertThat(request.getUrl()).isEqualTo(override.toString() + "/");
        return new Response(200, "OK", NO_HEADERS, new TypedString("{}"));
      }
    };

    Rest rest = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setServer("http://example.com")
        .setProfiler(mockProfiler)
        .build()
        .create(Rest.class);

    rest.get(override);
  }

  @Test public void overrideServerURLWithPath() throws Exception {
    final URI override = URI.create("http://example.com/resource/1");

    mockClient = new Client(){
      public Response execute(Request request) throws IOException {
        assertThat(request.getUrl()).isEqualTo(override.toString() + "/kick");
        return new Response(200, "OK", NO_HEADERS, new TypedString("{}"));
      }
    };

    Rest rest = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setServer("http://example.com")
        .setProfiler(mockProfiler)
        .build()
        .create(Rest.class);

    rest.kick(override);
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
