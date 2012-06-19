package retrofit.http;

import com.google.gson.Gson;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import retrofit.core.Callback;
import retrofit.core.MainThread;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts Java method calls to Rest calls.
 *
 * @author Bob Lee (bob@squareup.com)
 */
@Singleton public class RestAdapter {
  private static final Logger LOGGER = Logger.getLogger(RestAdapter.class.getName());

  @Inject private Server server;
  @Inject private Provider<HttpClient> httpClientProvider;
  @Inject private Executor executor;
  @Inject private MainThread mainThread;
  @Inject private Headers headers;
  @Inject private Gson gson;
  @Inject(optional = true) private HttpProfiler profiler;

  private ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
    @Override protected SimpleDateFormat initialValue() {
      return new SimpleDateFormat("HH:mm:ss");
    }
  };

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
  public static <T> Module service(final Class<T> type) {
    return new Module() {
      @Override public void configure(Binder binder) {
        binder.bind(type).toProvider(createProvider(type));
      }
    };
  }

  /**
   * Creates the {@link Provider} instances used by {@link #service(Class)}. Can be used by clients that
   * want more control over the implementation of their service interfaces, e.g. to wrap them
   * with caching logic.
   * <p>
   * Before use the provider must be injected via {@link com.google.inject.Injector#injectMembers}.
   */
  public static <T> Provider<T> createProvider(final Class<T> type) {
    return new Provider<T>() {
      @Inject RestAdapter restAdapter;

      @SuppressWarnings("unchecked")
      @Override public T get() {
        RestAdapter.RestHandler handler = restAdapter.new RestHandler();
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
      }
    };
  }

  private class RestHandler implements InvocationHandler {

    @Override public Object invoke(Object proxy, final Method method, final Object[] args) {
      // Construct HTTP request.
      final UiCallback<?> callback =
          UiCallback.create((Callback<?>) args[args.length - 1], mainThread);

      String url = server.apiUrl();
      String startTime = "NULL";
      try {
        // Build the request and headers.
        final HttpUriRequest request = new HttpRequestBuilder(gson).setMethod(method)
            .setArgs(args)
            .setApiUrl(server.apiUrl())
            .setHeaders(headers)
            .build();
        url = request.getURI().toString();

        // The last parameter should be of type Callback<T>. Determine T.
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        final Type resultType = getCallbackParameterType(method, genericParameterTypes);
        LOGGER.fine("Sending " + request.getMethod() + " to " + request.getURI());
        final Date start = new Date();
        startTime = dateFormat.get().format(start);

        final GsonResponseHandler<?> gsonResponseHandler =
            GsonResponseHandler.create(gson, resultType, callback, url, startTime);

        // Optionally wrap the response handler for server call profiling.
        final ResponseHandler<Void> rh = (profiler == null) ? gsonResponseHandler
            : createProfiler(gsonResponseHandler, (HttpProfiler<?>) profiler, getRequestInfo(method, request), start);

        // Execute HTTP request in the background.
        final String finalUrl = url;
        final String finalStartTime = startTime;
        executor.execute(new Runnable() {
          @Override public void run() {
            backgroundInvoke(request, rh, callback, finalUrl, finalStartTime);
          }
        });
      } catch (Throwable t) {
        LOGGER.log(Level.WARNING, t.getMessage() + " from " + url + " at " + startTime, t);
        callback.unexpectedError(t);
      }

      // Methods should return void.
      return null;
    }

    private HttpProfiler.RequestInformation getRequestInfo(Method method, HttpUriRequest request) {
      RequestLine requestLine = RequestLine.fromMethod(method);
      HttpMethodType httpMethod = requestLine.getHttpMethod();
      HttpProfiler.Method profilerMethod = httpMethod.profilerMethod();

      long contentLength = 0;
      String contentType = null;
      if (request instanceof HttpEntityEnclosingRequestBase) {
        HttpEntityEnclosingRequestBase entityReq = (HttpEntityEnclosingRequestBase) request;
        HttpEntity entity = entityReq.getEntity();
        contentLength = entity.getContentLength();
        contentType = entity.getContentType().getValue();
      }

      return new HttpProfiler.RequestInformation(profilerMethod, server.apiUrl(), requestLine.getRelativePath(),
          contentLength, contentType);
    }

    private void backgroundInvoke(HttpUriRequest request, ResponseHandler<Void> rh,
        UiCallback<?> callback, String url, String startTime) {
      try {
        httpClientProvider.get().execute(request, rh);
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, e.getMessage() + " from " + url + " at " + startTime, e);
        callback.networkError();
      } catch (Throwable t) {
        LOGGER.log(Level.WARNING, t.getMessage() + " from " + url + " at " + startTime, t);
        callback.unexpectedError(t);
      }
    }

    /** Wraps a {@code GsonResponseHandler} with a {@code ProfilingResponseHandler}. */
    private <T> ProfilingResponseHandler<T> createProfiler(ResponseHandler<Void> handlerToWrap,
        HttpProfiler<T> profiler, HttpProfiler.RequestInformation requestInfo, Date start) {


      ProfilingResponseHandler<T> responseHandler = new ProfilingResponseHandler<T>(handlerToWrap, profiler,
          requestInfo, start.getTime());
      responseHandler.beforeCall();

      return responseHandler;
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
  private static class ProfilingResponseHandler<T> implements ResponseHandler<Void> {
    private final ResponseHandler<Void> delegate;
    private final HttpProfiler<T> profiler;
    private final HttpProfiler.RequestInformation requestInfo;
    private final long startTime;
    private final AtomicReference<T> beforeCallData = new AtomicReference<T>();

    /** Wraps the delegate response handler. */
    private ProfilingResponseHandler(ResponseHandler<Void> delegate, HttpProfiler<T> profiler,
        HttpProfiler.RequestInformation requestInfo, long startTime) {
      this.delegate = delegate;
      this.profiler = profiler;
      this.requestInfo = requestInfo;
      this.startTime = startTime;
    }

    public void beforeCall() {
      try {
        beforeCallData.set(profiler.beforeCall());
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error occurred in HTTP profiler beforeCall().", e);
      }
    }

    @Override public Void handleResponse(HttpResponse httpResponse) throws IOException {
      // Intercept the response and send data to profiler.
      long elapsedTime = System.currentTimeMillis() - startTime;
      int statusCode = httpResponse.getStatusLine().getStatusCode();

      try {
        profiler.afterCall(requestInfo, elapsedTime, statusCode, beforeCallData.get());
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error occurred in HTTP profiler afterCall().", e);
      }

      // Pass along the response to the normal handler.
      return delegate.handleResponse(httpResponse);
    }
  }
}