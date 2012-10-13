package retrofit.http;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
  static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
    @Override protected DateFormat initialValue() {
      return new SimpleDateFormat("HH:mm:ss");
    }
  };

  private final Server server;
  private final Provider<HttpClient> httpClientProvider;
  private final Executor executor;
  private final MainThread mainThread;
  private final Headers headers;
  private final Converter converter;
  private final HttpProfiler profiler;

  @Inject public RestAdapter(Server server, Provider<HttpClient> httpClientProvider, Executor executor,
      MainThread mainThread, Headers headers, Converter converter, HttpProfiler profiler) {
    this.server = server;
    this.httpClientProvider = httpClientProvider;
    this.executor = executor;
    this.mainThread = mainThread;
    this.headers = headers;
    this.converter = converter;
    this.profiler = profiler;
  }

  /**
   * Adapts a Java interface to a REST API. HTTP requests happen in a background thread. Callbacks
   * happen in the UI thread.
   *
   * <p>Gets the relative path for a given method from a {@link GET}, {@link POST}, {@link PUT}, or
   * {@link DELETE} annotation on the method. Gets the names of URL parameters from {@link
   * javax.inject.Named} annotations on the method parameters.
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
  public <T> T create(Class<T> type) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(),
        new Class<?>[] {type}, new RestHandler());
  }

  private class RestHandler implements InvocationHandler {
    private final Map<Method, Type> responseTypeCache = new HashMap<Method, Type>();

    @Override public Object invoke(Object proxy, final Method method, final Object[] args) {
      // Determine whether or not the execution will be synchronous.
      boolean isSynchronousInvocation = methodWantsSynchronousInvocation(method);
      if (isSynchronousInvocation) {
        // TODO support synchronous invocations!
        throw new UnsupportedOperationException("Synchronous invocation not supported.");
      }

      // Construct HTTP request.
      final Callback<?> callback =
          UiCallback.create((Callback<?>) args[args.length - 1], mainThread);

      String url = server.apiUrl();
      String startTime = "NULL";
      try {
        // Build the request and headers.
        final HttpUriRequest request = new HttpRequestBuilder(converter) //
            .setMethod(method)
            .setArgs(args)
            .setApiUrl(server.apiUrl())
            .setHeaders(headers)
            .build();
        url = request.getURI().toString();

        // Determine deserialization type by method return type or generic parameter to Callback argument.
        Type type = responseTypeCache.get(method);
        if (type == null) {
          type = getResponseObjectType(method, isSynchronousInvocation);
          responseTypeCache.put(method, type);
        }

        LOGGER.fine("Sending " + request.getMethod() + " to " + request.getURI());
        final Date start = new Date();
        startTime = DATE_FORMAT.get().format(start);

        ResponseHandler<Void> rh = new CallbackResponseHandler(callback, type, converter, url, start, DATE_FORMAT);

        // Optionally wrap the response handler for server call profiling.
        if (profiler != HttpProfiler.NONE) {
          rh = createProfiler(rh, (HttpProfiler<?>) profiler, getRequestInfo(method, request), start);
        }

        // Execute HTTP request in the background.
        final String finalUrl = url;
        final String finalStartTime = startTime;
        final ResponseHandler<Void> finalResponseHandler = rh;
        executor.execute(new Runnable() {
          @Override public void run() {
            invokeRequest(request, finalResponseHandler, callback, finalUrl, finalStartTime);
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

        Header entityContentType = entity.getContentType();
        contentType = entityContentType != null ? entityContentType.getValue() : null;
      }

      return new HttpProfiler.RequestInformation(profilerMethod, server.apiUrl(), requestLine.getRelativePath(),
          contentLength, contentType);
    }

    private void invokeRequest(HttpUriRequest request, ResponseHandler<Void> rh,
        Callback<?> callback, String url, String startTime) {
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
  }

  /**
   * Determine whether or not execution for a method should be done synchronously.
   *
   * @throws IllegalArgumentException if the supplied {@code method} has both a return type and {@link Callback}
   *     argument or neither of the two.
   */
  static boolean methodWantsSynchronousInvocation(Method method) {
    boolean hasReturnType = method.getReturnType() != void.class;

    Class<?>[] parameterTypes = method.getParameterTypes();
    boolean hasCallback = parameterTypes.length > 0
        && Callback.class.isAssignableFrom(parameterTypes[parameterTypes.length - 1]);

    if ((hasReturnType && hasCallback) || (!hasReturnType && !hasCallback)) {
      throw new IllegalArgumentException("Method must have either a return type or Callback as last argument.");
    }
    return hasReturnType;
  }

  /** Get the callback parameter types. */
  static Type getResponseObjectType(Method method, boolean isSynchronousInvocation) {
    if (isSynchronousInvocation) {
      return method.getGenericReturnType();
    }

    Type[] parameterTypes = method.getGenericParameterTypes();
    Type callbackType = parameterTypes[parameterTypes.length - 1];
    Class<?> callbackClass;
    if (callbackType instanceof Class) {
      callbackClass = (Class<?>) callbackType;
    } else if (callbackType instanceof ParameterizedType) {
      callbackClass = (Class<?>) ((ParameterizedType) callbackType).getRawType();
    } else {
      throw new ClassCastException(
          String.format("Last parameter of %s must be a Class or ParameterizedType", method));
    }
    if (Callback.class.isAssignableFrom(callbackClass)) {
      callbackType = Types.getGenericSupertype(callbackType, callbackClass, Callback.class);
      if (callbackType instanceof ParameterizedType) {
        Type[] types = ((ParameterizedType) callbackType).getActualTypeArguments();
        for (int i = 0; i < types.length; i++) {
          Type type = types[i];
          if (type instanceof WildcardType) {
            types[i] = ((WildcardType) type).getUpperBounds()[0];
          }
        }
        return types[0];
      }
    }
    throw new IllegalArgumentException(
        String.format("Last parameter of %s must be of type Callback<X,Y,Z> or Callback<? super X,..,..>.", method));
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