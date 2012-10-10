package retrofit.http;

import com.google.gson.Gson;
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
  public static final String DEBUGGING_DATE_FORMAT = "HH:mm:ss";

  private final Server server;
  private final Provider<HttpClient> httpClientProvider;
  private final Executor executor;
  private final MainThread mainThread;
  private final Headers headers;
  private final Gson gson;
  private final HttpProfiler profiler;

  private final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
    @Override protected SimpleDateFormat initialValue() {
      return new SimpleDateFormat(DEBUGGING_DATE_FORMAT);
    }
  };

  @Inject
  RestAdapter(Server server, Provider<HttpClient> httpClientProvider, Executor executor,
      MainThread mainThread, Headers headers, Gson gson, HttpProfiler profiler) {
    this.server = server;
    this.httpClientProvider = httpClientProvider;
    this.executor = executor;
    this.mainThread = mainThread;
    this.headers = headers;
    this.gson = gson;
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
        Type[] resultTypes = getCallbackParameterTypes(method);
        final Type resultType = resultTypes[resultTypes.length - 1];
        LOGGER.fine("Sending " + request.getMethod() + " to " + request.getURI());
        final Date start = new Date();
        startTime = dateFormat.get().format(start);

        final GsonResponseHandler<?> gsonResponseHandler =
            GsonResponseHandler.create(gson, resultType, callback, url, start, dateFormat);

        // Optionally wrap the response handler for server call profiling.
        final ResponseHandler<Void> rh = (profiler == HttpProfiler.NONE)
            ? gsonResponseHandler
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

        Header entityContentType = entity.getContentType();
        contentType = entityContentType != null ? entityContentType.getValue() : null;
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
  }

  /** Get the callback parameter types. */
  static Type[] getCallbackParameterTypes(Method method) {
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
        return types;
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