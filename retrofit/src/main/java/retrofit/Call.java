package retrofit;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * 
 * @param <T>
 */
public class Call<T> {

	private final Method method;
	private final Object[] args;
	private final RequestInterceptor requestInterceptor;
	private final ErrorHandler errorHandler;
	private final Executor httpExecutor;
	private final Executor callbackExecutor;
	private RestMethodInfo methodDetails = null;

	private boolean isRetriable = false;
	private boolean isCanceled = false;

	public Call(Method method, Object[] args,
			RequestInterceptor requestInterceptor, ErrorHandler errorHandler,
			Executor httpExecutor, Executor callbackExecutor) {
		this.method = method;
		this.args = args;
		this.requestInterceptor = requestInterceptor;
		this.errorHandler = errorHandler;
		this.httpExecutor = httpExecutor;
		this.callbackExecutor = callbackExecutor;
	}

	/**
	 * Callers can synchronously call {@code execute()} which will return type
	 * {@code T}. Exceptions will be thrown for any error, network error,
	 * unsuccessful response, or unsuccessful deserialization of the response
	 * body. While these exceptions will likely extend from the same supertype,
	 * it's unclear as to whether that supertype should be checked or unchecked.
	 * 
	 * @return
	 * @throws Throwable
	 */
	public T execute() throws Throwable {
		if (methodDetails == null) {
			methodDetails = new RestMethodInfo(method);
		}

		try {
			// TODO create invokeRequest()
			// return invokeRequest(requestInterceptor, methodDetails, args);
			return null;

		} catch (RetrofitError error) {
			int status = error.getResponse().getStatus();
			if (error.isNetworkError() || (status >= 500 && status < 600)) {
				isRetriable = true;
			}

			Throwable newError = errorHandler.handleError(error);
			if (newError == null) {
				throw new IllegalStateException(
						"Error handler returned null for wrapped exception.",
						error);
			}
			throw newError;
		}
	}

	/**
	 * Callers can supply callbacks to this object for asynchronous notification
	 * of the response. The traditional {@link Callback Callback} of the current
	 * version of Retrofit will be available. One change will be that the error
	 * object passed to {@link Callback.failure failure} will not be the same
	 * exception as would be thrown in synchronous execution but rather
	 * something a bit more transparent to the underlying cause.
	 * 
	 * @param callback
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void execute(Callback<T> callback) throws Throwable {
		if (methodDetails == null) {
			methodDetails = new RestMethodInfo(method);
		}

		if (httpExecutor == null || callbackExecutor == null) {
			throw new IllegalStateException(
					"Asynchronous invocation requires calling setExecutors.");
		}
		// Apply the interceptor synchronously, recording the interception so we
		// can replay it later.
		// This way we still defer argument serialization to the background
		// thread.
		final RequestInterceptorTape interceptorTape = new RequestInterceptorTape();
		requestInterceptor.intercept(interceptorTape);
		httpExecutor.execute(new CallbackRunnable(callback, callbackExecutor) {
			@Override public ResponseWrapper obtainResponse() {
				// TODO create invokeRequest()
				// return (ResponseWrapper) invokeRequest(interceptorTape,
				// methodDetails, args);
				return null;
			}
		});
	}

	/**
	 * Is a no-op after the response has been received. In all other cases the
	 * method will set any callbacks to {@code null} (thus freeing strong
	 * references to the enclosing class if declared anonymously) and render the
	 * request object dead. All future interactions with the request object will
	 * throw an exception. If the request is waiting in the executor its Future
	 * will be cancelled so that it is never invoked.
	 */
	public void cancel() {
		if (isCanceled) {
			return;
		}

		// TODO Implement cancel()

		isCanceled = true;
	}

	/**
	 * Will re-submit the request onto the backing executor without passing
	 * through any of the mutating pipeline described above. Retrying a request
	 * is only available after a network error or 5XX response. Attempting to
	 * retry a request that is currently in flight, after a non-5XX response,
	 * after an unexpected error, or after calling {@code cancel()} will throw
	 * an exception.
	 */
	public void retry() {
		if (!isRetriable) {
			throw new IllegalStateException(
					"Retrying a request is only available after a network error or 5XX response");
		}

		// TODO Implement retry()

	}
}
