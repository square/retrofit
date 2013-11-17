package retrofit;

/**
 * 
 * @param <T>
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

	/**
	 * Constructor
	 */
	public Call(RestAdapter.RestHandler handler, RequestInterceptor interceptor,
		RestMethodInfo methodInfo, Object[] args)
	{
		this.handler = handler;
		this.interceptor = interceptor;
		this.methodInfo = methodInfo;
	}

	/**
	 * Execute immediately
	 */
	public T execute() {
		return (T)handler.invokeRequest(interceptor, methodInfo, args);
	}

	public int getthing() {
		return 10;
	}
}
