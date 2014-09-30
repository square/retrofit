package retrofit;

/**
 * Created by dp on 22/04/14.
 */
// Copyright 2013 Square, Inc.

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import retrofit.client.Client;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.http.JsonRpcMethod;
import retrofit.http.POST;
import retrofit.http.RpcParam;
import retrofit.mime.TypedInput;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JsonRpcTest {
  private static final List<Header> NO_HEADERS = Collections.emptyList();

  /** Not all servers play nice and add content-type headers to responses. */
  private static final TypedInput NO_MIME_BODY = new TypedInput() {
    @Override public String mimeType() {
      return null;
    }

    @Override public long length() {
      return 2;
    }

    @Override public InputStream in() throws IOException {
      return new ByteArrayInputStream("{}".getBytes("UTF-8"));
    }
  };

  private interface Example {
    @JsonRpcMethod("my_test_method")
    Object my_test_method(@RpcParam("param1") String param1, @RpcParam("param2") String param2);

    @JsonRpcMethod("my_test_method")
    void my_test_method_async(@RpcParam("param1") String param1, @RpcParam("param2") String param2, Callback<ResultResponse> data);

    @POST("/test_post")
    Object testPost();
  }

  private Client mockClient;
  private Executor mockRequestExecutor;
  private Executor mockCallbackExecutor;
  private Profiler<Object> mockProfiler;
  private Example example;
  private Gson gson;

  @SuppressWarnings("unchecked") // Mock profiler type erasure.
  @Before
  public void setUp() throws Exception{
    mockClient = mock(Client.class);
    mockRequestExecutor = spy(new Utils.SynchronousExecutor());
    mockCallbackExecutor = spy(new Utils.SynchronousExecutor());
    mockProfiler = mock(Profiler.class);
    gson = new GsonBuilder().create();

    example = new RestAdapter.Builder() //
        .setClient(mockClient)
        .setExecutors(mockRequestExecutor, mockCallbackExecutor)
        .setEndpoint("http://example.com:3344/api/")
        .setProfiler(mockProfiler)
        .setLogLevel(RestAdapter.LogLevel.FULL)
        .build()
        .create(Example.class);
  }

  @Test
  public void makeRpcRequestSync() throws IOException {
    Object data = new Object();
    when(mockProfiler.beforeCall()).thenReturn(data);
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", NO_HEADERS, null));

    example.my_test_method("value1", "value2");

    verify(mockProfiler).beforeCall();
    verify(mockClient).execute(any(Request.class));
    verify(mockProfiler).afterCall(any(Profiler.RequestInformation.class), anyInt(), eq(200), same(data));
  }

  private class ResultResponse {
    String result;

    private ResultResponse(String result) {
      this.result = result;
    }
  }

  @Test
  public void makeRpcRequestAsync() throws IOException {
    final String test_data = "test result";
    final String json = gson.toJson(new ResultResponse(test_data));
    Object data = new Object();
    when(mockProfiler.beforeCall()).thenReturn(data);
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedInput() {
          @Override
          public String mimeType() {
            return "application/json";
          }

          @Override
          public long length() {
            return json.length();
          }

          @Override
          public InputStream in() throws IOException {
            return new ByteArrayInputStream(json.getBytes());
          }
        }));

    example.my_test_method_async("value1", "value2", new Callback<ResultResponse>() {
      @Override
      public void success(ResultResponse o, Response response) {
//        assertThat(o != null);
//        assertThat(o.result == null);
//        assertThat(o.result.equals(test_data));
//        assertThat("http://example.com:3344/api/".equals(response.getUrl()));
      }

      @Override
      public void failure(RetrofitError error) {

      }
    });

    verify(mockProfiler).beforeCall();
    verify(mockClient).execute(any(Request.class));
    verify(mockProfiler).afterCall(any(Profiler.RequestInformation.class), anyInt(), eq(200), same(data));
  }

  @Test
  public void testRestPost() throws IOException {
    final String test_data = "test result";
    final String json = gson.toJson(new ResultResponse(test_data));
    Object data = new Object();
    when(mockProfiler.beforeCall()).thenReturn(data);
    when(mockClient.execute(any(Request.class))) //
        .thenReturn(new Response("http://example.com/", 200, "OK", NO_HEADERS, new TypedInput() {
          @Override
          public String mimeType() {
            return "application/json";
          }

          @Override
          public long length() {
            return json.length();
          }

          @Override
          public InputStream in() throws IOException {
            return new ByteArrayInputStream(json.getBytes());
          }
        }));

    example.testPost();

    verify(mockProfiler).beforeCall();
    verify(mockClient).execute(any(Request.class));
    verify(mockProfiler).afterCall(any(Profiler.RequestInformation.class), anyInt(), eq(200), same(data));
  }
}
