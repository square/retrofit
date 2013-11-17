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
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;
import retrofit.mime.TypedString;
import rx.Observable;
import rx.util.functions.Action1;

import static org.junit.Assert.*;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static retrofit.Profiler.RequestInformation;
import static retrofit.RestAdapter.LogLevel.BASIC;
import static retrofit.RestAdapter.LogLevel.FULL;
import static retrofit.RestAdapter.LogLevel.HEADERS;
import static retrofit.Utils.SynchronousExecutor;

/**
 * Example of the Call object behavior. Not a working test.
 */
public class CallTest {

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
		  return new ByteArrayInputStream("{}".getBytes("UTF-8"));
		}
	};

	private interface Api {
		@GET("/")
		Call<String> something();
	}

	private Client mockClient;
	private Executor mockRequestExecutor;
	private Executor mockCallbackExecutor;
	private Api api;

	@SuppressWarnings("unchecked") // Mock profiler type erasure.
	@Before public void setUp() throws Exception {
		mockClient = mock(Client.class);
		mockRequestExecutor = spy(new SynchronousExecutor());
    	mockCallbackExecutor = spy(new SynchronousExecutor());

		api = new RestAdapter.Builder()
			.setClient(mockClient)
			.setExecutors(mockRequestExecutor, mockCallbackExecutor)
			.setServer("http://example.com")
			.build()
			.create(Api.class);
	}

	@Test public void test() throws Exception {
		when(mockClient.execute(any(Request.class))) //
        	.thenReturn(new Response(200, "OK", NO_HEADERS, new TypedString("Hello")));

		Call<String> c = api.something();

		String result = c.execute();

		assertEquals(result, "Hello");
	}

}
