package retrofit2;

import okhttp3.Request;

/**
 * Intercepts a {@link Request.Builder} just before it is built into an actual {@link Request}. Interceptors
 * can be {@linkplain Retrofit.Builder#addRequestBuilderInterceptor(RequestBuilderInterceptor) added}
 * to the {@link Retrofit} instance.
 */
public interface RequestBuilderInterceptor {

    /**
     * Intercepts the {@code requestBuilder} with the provided {@code invocation}. The {@code invocation}
     * is used to access all method annotations, parameter annotations and method arguments. Use this
     * method to provide your own modifications on the {@code requestBuilder} just before it's built into
     * a {@link Request}.
     */
    void intercept(Request.Builder requestBuilder, Invocation invocation);

}
