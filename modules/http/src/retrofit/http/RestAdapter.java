package retrofit.http;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import retrofit.core.Callback;
import retrofit.core.MainThread;
import retrofit.internal.gson.Gson;

/**
 * Converts Java method calls to Rest calls.
 *
 * @author Bob Lee (bob@squareup.com)
 */
@Singleton public class RestAdapter {
  private static final Logger logger = Logger.getLogger(RestAdapter.class.getName());

  @Inject private Server server;
  @Inject private Provider<HttpClient> httpClientProvider;
  @Inject private Executor executor;
  @Inject private MainThread mainThread;
  @Inject private Headers headers;
  @Inject private Gson gson;
  @Inject(optional = true) private HttpProfiler profiler;

  /**
   * Adapts a Java interface to a REST API. HTTP requests happen in a background thread. Callbacks
   * happen in the UI thread.
   *
   * <p>Gets the relative path for a given method from a {@link GET}, {@link POST}, {@link PUT}, or
   * {@link DELETE} annotation on the method. Gets the names of URL parameters from {@link
   * com.google.inject.name.Named} annotations on the method parameters.
   *
   * <p>The last method parameter should be of type {@link Callback}. The JSON HTTP response will be
   * converted to the callback's parameter type using GSON. If the callback parameter type uses a
   * wildcard, the lower bound will be used as the conversion type.
   *
   * <p>For example:
   *
   * <pre>
   *   public interface MyApi {
   *     &#64;POST("go") public void go(@Named("a") String a, @Named("b") int b,
   *         Callback&lt;? super MyResult> callback);
   *   }
   * </pre>
   *
   * @param type to implement
   */
  @SuppressWarnings("unchecked")
  public static <T> Module service(final Class<T> type) {
    return new Module() {
      @Override public void configure(Binder binder) {
        binder.bind(type).toProvider(new Provider<T>() {
          @Inject RestAdapter restAdapter;

          @Override public T get() {
            RestAdapter.RestHandler handler = restAdapter.new RestHandler();
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                handler);
          }
        });
      }
    };
  }

  private class RestHandler implements InvocationHandler {

    @Override public Object invoke(Object proxy, final Method method, final Object[] args) {
      // Construct HTTP request.
      final UiCallback<?> callback =
          UiCallback.create((Callback<?>) args[args.length - 1], mainThread);

      try {
        // Build the request and headers.
        final HttpUriRequest request = new HttpRequestBuilder(gson).setMethod(method)
            .setArgs(args)
            .setApiUrl(server.apiUrl())
            .setHeaders(headers)
            .build();

        // The last parameter should be of type Callback<T>. Determine T.
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        final Type resultType = getCallbackParameterType(method, genericParameterTypes);
        logger.fine("Sending " + request.getMethod() + " to " + request.getURI());

        final GsonResponseHandler<?> gsonResponseHandler =
            GsonResponseHandler.create(gson, resultType, callback);

        // Optionally wrap the response handler for server call profiling.
        final ResponseHandler<Void> rh = (profiler == null) ? gsonResponseHandler
            : createProfiler(gsonResponseHandler, profiler, method, server.apiUrl());

        // Execute HTTP request in the background.
        executor.execute(new Runnable() {
          @Override public void run() {
            backgroundInvoke(request, rh, callback);
          }
        });

      } catch (Throwable t) {
        logger.log(Level.WARNING, t.getMessage() + " from " + server.apiUrl(), t);
        callback.unexpectedError(t);
      }

      // Methods should return void.
      return null;
    }

    private void backgroundInvoke(HttpUriRequest request, ResponseHandler<Void> rh, UiCallback<?> callback) {
      try {
        httpClientProvider.get().execute(request, rh);
      } catch (IOException e) {
        logger.log(Level.WARNING, e.getMessage() + " from " + server.apiUrl(), e);
        callback.networkError();
      } catch (Throwable t) {
        logger.log(Level.WARNING, t.getMessage() + " from " + server.apiUrl(), t);
        callback.unexpectedError(t);
      }
    }

    /** Wraps a {@code GsonResponseHandler} with a {@code ProfilingResponseHandler}. */
    private ProfilingResponseHandler createProfiler(ResponseHandler<Void> handlerToWrap,
        HttpProfiler profiler, Method method, String apiUrl) {
      RequestLine requestLine = RequestLine.fromMethod(method);

      HttpMethodType httpMethod = requestLine.getHttpMethod();
      HttpProfiler.Method profilerMethod = httpMethod.profilerMethod();

      return new ProfilingResponseHandler(handlerToWrap, profiler, profilerMethod, apiUrl,
          requestLine.getRelativePath());
    }

    private static final String NOT_CALLBACK =
        "Last parameter of %s must be" + " of type Callback<X> or Callback<? super X>.";

    /** Gets the callback parameter type. */
    private Type getCallbackParameterType(Method method, Type[] parameterTypes) {
      Type lastType = parameterTypes[parameterTypes.length - 1];

      /*
       * Note: When more than one parameter is present, we seem to get
       * ParameterizedType Callback<? extends Object> instead of
       * WilcardType Callback<? super Foo> like we expected. File a bug.
       */
      if (lastType instanceof ParameterizedType) {
        ParameterizedType pType = (ParameterizedType) lastType;
        if (pType.getRawType() != Callback.class) {
          throw notCallback(method);
        }
        return pType.getActualTypeArguments()[0];
      }

      if (lastType instanceof WildcardType) {
        // Example: ? super SimpleResponse
        // Use the lower bound.
        WildcardType wildcardType = (WildcardType) lastType;
        Type[] lowerBounds = wildcardType.getLowerBounds();
        if (lowerBounds.length != 1) {
          throw notCallback(method);
        }
        return lowerBounds[0];
      }

      throw notCallback(method);
    }

    private IllegalArgumentException notCallback(Method method) {
      return new IllegalArgumentException(String.format(NOT_CALLBACK, method));
    }
  }

  /** Sends server call times and response status codes to {@link HttpProfiler}. */
  private static class ProfilingResponseHandler implements ResponseHandler<Void> {
    private final ResponseHandler<Void> delegate;
    private final HttpProfiler profiler;
    private final String apiUrl;
    private final String relativePath;
    private final HttpProfiler.Method method;
    private final long startTime = System.currentTimeMillis();

    /** Wraps the delegate response handler. */
    private ProfilingResponseHandler(ResponseHandler<Void> delegate, HttpProfiler profiler,
        HttpProfiler.Method method, String apiUrl, String relativePath) {
      this.delegate = delegate;
      this.profiler = profiler;
      this.method = method;
      this.apiUrl = apiUrl;
      this.relativePath = relativePath;
    }

    @Override public Void handleResponse(HttpResponse httpResponse) throws IOException {
      // Intercept the response and send data to profiler.
      long elapsedTime = System.currentTimeMillis() - startTime;
      int statusCode = httpResponse.getStatusLine().getStatusCode();
      profiler.called(method, apiUrl, relativePath, elapsedTime, statusCode);

      // Pass along the response to the normal handler.
      return delegate.handleResponse(httpResponse);
    }
  }
}