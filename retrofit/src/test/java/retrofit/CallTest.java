// Copyright 2013 Square, Inc.
package retrofit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;

import retrofit.Utils.SynchronousExecutor;
import retrofit.client.Client;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.mime.TypedString;

public class CallTest {

	private static final List<Header> NO_HEADERS = Collections.emptyList();
	
	private interface Api {
		@GET("/")
		Call<String> something();
	}

	private Client mockClient;
	private Executor mockRequestExecutor;
	private Executor mockCallbackExecutor;
	private Api api;

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

	@Test public void synchronousTest() throws Exception {
		when(mockClient.execute(any(Request.class)))
        	.thenReturn(new Response(200, "OK", NO_HEADERS, new TypedString("Hello")));

		Call<String> c = api.something();

		String result = c.execute();
		assertEquals(result, "Hello");
	}

	@SuppressWarnings("unchecked")
	@Test public void asyncTest() throws Exception {
		when(mockClient.execute(any(Request.class)))
        	.thenReturn(new Response(200, "OK", NO_HEADERS, new TypedString("Hello")));

		Call<String> c = api.something();

		Callback2<String> callback = mock(Callback2.class);
		c.execute(callback);

		verify(callback).success(eq("Hello"));
	}

}
