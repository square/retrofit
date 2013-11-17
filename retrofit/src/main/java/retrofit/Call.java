// Copyright 2013 Square, Inc.
package retrofit;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * API 2.0 future request invocation/call object.
 */
public class Call<T> {

	// RestHandler that created this Call<T> and that will carry out the actual
	// request
	private RestAdapter.RestHandler handler;

	// Interceptor to call invokeRequest() with
	private RequestInterceptor interceptor;

	// RestMethodInfo to call invokeRequest() with
	private RestMethodInfo methodInfo;

	// Object[] to call invokeRequest() with
	private Object[] args;

	// Executor for HTTP requests
	private Executor httpExecutor;

	// Scheduled asynchronous execute() call
	private ScheduledFuture<?> future;

	/**
	 * Constructor
	 */
	public Call(RestAdapter.RestHandler handler, RequestInterceptor interceptor,
		RestMethodInfo methodInfo, Object[] args, Executor httpExecutor)
	{
		this.handler = handler;
		this.interceptor = interceptor;
		this.methodInfo = methodInfo;
		this.httpExecutor = httpExecutor;
		this.future = null;
	}

	/**
	 * Execute immediately
	 */
	public T execute() {
		return (T)handler.invokeRequest(interceptor, methodInfo, args);
	}

	/**
	 * Execute with a (API 2.0) callback
	 */
	public void execute(final Callback2<T> callback) {
		// Not handling the interceptor or observable for now. The synchronous
		// version of RestHandler.invoke() does not handle this either.

		future = httpExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				T response;

				try {
					response = (T)handler.invokeRequest(interceptor, methodInfo, args);
				}
				catch (RetrofitError err) {
					callback.failure(err);
					return;
				}

				callback.success(response);
			}
		});
	}
}
