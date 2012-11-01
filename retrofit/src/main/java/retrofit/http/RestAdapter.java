package retrofit.http;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import retrofit.http.Callback.ServerError;
import retrofit.http.HttpProfiler.RequestInformation;
import retrofit.http.RestException.ClientHttpException;
import retrofit.http.RestException.HttpException;
import retrofit.http.RestException.NetworkException;
import retrofit.http.RestException.ServerHttpException;
import retrofit.http.RestException.UnauthorizedHttpException;
import retrofit.http.RestException.UnexpectedException;

import javax.inject.Provider;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

/**
 * Converts Java method calls to Rest calls.
 *
 * @author Bob Lee (bob@squareup.com)
 * @author Jake Wharton (jw@squareup.com)
 */
public class RestAdapter {
  private static final Logger LOGGER = Logger.getLogger(RestAdapter.class.getName());
  private static final int LOG_CHUNK_SIZE = 4000;

  private final Server server;
  private final Provider<HttpClient> httpClientProvider;
  private final Executor httpExecutor;
  private final Executor callbackExecutor;
  private final Headers headers;
  private final Converter converter;
  private final HttpProfiler profiler;

  private RestAdapter(Server server, Provider<HttpClient> httpClientProvider, Executor httpExecutor,
      Executor callbackExecutor, Headers headers, Converter converter, HttpProfiler profiler) {
    this.server = server;
    this.httpClientProvider = httpClientProvider;
    this.httpExecutor = httpExecutor;
    this.callbackExecutor = callbackExecutor;
    this.headers = headers;
    this.converter = converter;
    this.profiler = profiler;
  }

  /**
   * Adapts a Java interface to a REST API.
   * <p/>
   * The relative path for a given method is obtained from a {@link GET}, {@link POST}, {@link PUT}, or {@link DELETE}
   * annotation on the method. Gets the names of URL parameters from {@link javax.inject.Named} annotations on the
   * method parameters.
   * <p/>
   * HTTP requests happen in one of two ways:
   * <ul>
   *   <li>On the provided HTTP {@link Executor} with callbacks marshaled to the callback {@link Executor}. The last
   *   method parameter should be of type {@link Callback}. The HTTP response will be converted to the callback's
   *   parameter type using the specified {@link Converter}. If the callback parameter type uses a wildcard, the lower
   *   bound will be used as the conversion type.</li>
   *   <li>On the current thread returning the response or throwing a {@link RestException}. The HTTP response will be
   *   converted to the method's return type using the specified {@link Converter}.</li>
   * </ul>
   * <p/>
   * For example:
   * <pre>
   *   public interface MyApi {
   *     &#64;POST("go") // Asynchronous execution.
   *     public void go(@Named("a") String a, @Named("b") int b, Callback&lt;? super MyResult> callback);
   *     &#64;POST("go") // Synchronous execution.
   *     public MyResult go(@Named("a") String a, @Named("b") int b);
   *   }
   * </pre>
   *
   * @param type to implement
   */
  @SuppressWarnings("unchecked")
  public <T> T create(Class<T> type) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, new RestHandler());
  }

  private class RestHandler implements InvocationHandler {
    private final Map<Method, Type> responseTypeCache = new HashMap<Method, Type>();

    @SuppressWarnings("unchecked")
    @Override public Object invoke(Object proxy, final Method method, final Object[] args) {
      if (methodWantsSynchronousInvocation(method)) {
        return invokeRequest(method, args, true);
      } else {
        httpExecutor.execute(new CallbackRunnable(obtainCallback(args), callbackExecutor) {
          @Override public Object obtainResponse() {
            return invokeRequest(method, args, false);
          }
        });
        return null; // Asynchronous methods should have return type of void.
      }
    }

    /**
     * Execute an HTTP request.
     *
     * @return HTTP response object of specified {@code type}.
     * @throws ClientHttpException if HTTP 4XX error occurred.
     * @throws UnauthorizedHttpException if HTTP 401 error occurred.
     * @throws ServerHttpException if HTTP 5XX error occurred.
     * @throws NetworkException if the {@code request} URL was unreachable.
     * @throws UnexpectedException if an unexpected exception was thrown while processing the request.
     */
    private Object invokeRequest(Method method, Object[] args, boolean isSynchronousInvocation) {
      long start = System.nanoTime();
      String url = server.apiUrl();
      try {
        // Build the request and headers.
        final HttpUriRequest request = new HttpRequestBuilder(converter) //
            .setMethod(method, isSynchronousInvocation)
            .setArgs(args)
            .setApiUrl(url)
            .setHeaders(headers)
            .build();
        url = request.getURI().toString();

        // Determine deserialization type by method return type or generic parameter to Callback argument.
        Type type = responseTypeCache.get(method);
        if (type == null) {
          type = getResponseObjectType(method, isSynchronousInvocation);
          responseTypeCache.put(method, type);
        }

        Object profilerObject = null;
        if (profiler != null) {
          profilerObject = profiler.beforeCall();
        }

        LOGGER.fine("Sending " + request.getMethod() + " to " + request.getURI());
        HttpResponse response = httpClientProvider.get().execute(request);
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();

        long elapsedTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        if (profiler != null) {
          RequestInformation requestInfo = getRequestInfo(server, method, request);
          profiler.afterCall(requestInfo, elapsedTime, statusCode, profilerObject);
        }

        HttpEntity entity = response.getEntity();
        byte[] body = null;
        if (entity != null) {
          body = EntityUtils.toByteArray(entity);
        }
        if (LOGGER.isLoggable(Level.FINE)) {
          logResponseBody(url, body, statusCode, elapsedTime);
        }

        try {
          if (statusCode >= 200 && statusCode < 300) { // 2XX == successful request
            return converter.to(body, type);
          } else if (statusCode == SC_UNAUTHORIZED) { // 401 == unauthorized user
            ServerError serverError = (ServerError) converter.to(body, ServerError.class);
            throw new UnauthorizedHttpException(url, statusLine.getReasonPhrase(), serverError);
          } else if (statusCode >= 500) { // 5XX == server error
            ServerError serverError = (ServerError) converter.to(body, ServerError.class);
            throw new ServerHttpException(url, statusCode, statusLine.getReasonPhrase(), serverError);
          } else { // 4XX == client error
            Object clientError = converter.to(body, type);
            throw new ClientHttpException(url, statusCode, statusLine.getReasonPhrase(), clientError);
          }
        } catch (ConversionException e) {
          LOGGER.log(WARNING, e.getMessage() + " from " + url, e);
          throw new ServerHttpException(url, statusCode, statusLine.getReasonPhrase(), e);
        }
      } catch (HttpException e) {
        if (LOGGER.isLoggable(Level.FINE)) {
          LOGGER.fine("Sever returned " + e.getStatus() + ", " + e.getMessage() + ". Body: " + e.getResponse()
              + ". Url: " + e.getUrl());
        }
        throw e; // Allow any rest-related exceptions to pass through.
      } catch (IOException e) {
        LOGGER.log(WARNING, e.getMessage() + " from " + url, e);
        throw new NetworkException(url, e);
      } catch (Throwable t) {
        LOGGER.log(WARNING, t.getMessage() + " from " + url, t);
        throw new UnexpectedException(url, t);
      }
    }
  }

  private static void logResponseBody(String url, byte[] body, int statusCode, long elapsedTime)
      throws UnsupportedEncodingException {
    LOGGER.fine("---- HTTP " + statusCode + " from " + url + " (" + elapsedTime + "ms)");
    String bodyString = new String(body, "UTF-8");
    for (int i = 0; i < body.length; i += LOG_CHUNK_SIZE) {
      int end = Math.min(bodyString.length(), i + LOG_CHUNK_SIZE);
      LOGGER.fine(bodyString.substring(i, end));
    }
    LOGGER.fine("---- END HTTP");
  }

  private static Callback<?> obtainCallback(Object[] args) {
    return (Callback<?>) args[args.length - 1];
  }

  private static HttpProfiler.RequestInformation getRequestInfo(Server server, Method method, HttpUriRequest request) {
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

  /**
   * Build a new {@link RestAdapter}.
   * <p/>
   * Calling the following methods is required before calling {@link #build()}:
   * <ul>
   *   <li>{@link #setServer(Server)}</li>
   *   <li>{@link #setClient(javax.inject.Provider)}</li>
   *   <li>{@link #setConverter(Converter)}</li>
   * </ul>
   * If you are using asynchronous execution (i.e., with {@link Callback Callbacks}) the following is also required:
   * <ul>
   *   <li>{@link #setExecutors(java.util.concurrent.Executor, java.util.concurrent.Executor)}</li>
   * </ul>
   */
  public static class Builder {
    private Server server;
    private Provider<HttpClient> clientProvider;
    private Executor httpExecutor;
    private Executor callbackExecutor;
    private Headers headers;
    private Converter converter;
    private HttpProfiler profiler;

    public Builder setServer(String endpoint) {
      if (endpoint == null) throw new NullPointerException("endpoint");
      return setServer(new Server(endpoint));
    }

    public Builder setServer(Server server) {
      if (server == null) throw new NullPointerException("server");
      this.server = server;
      return this;
    }

    public Builder setClient(final HttpClient client) {
      if (client == null) throw new NullPointerException("client");
      return setClient(new Provider<HttpClient>() {
        @Override public HttpClient get() {
          return client;
        }
      });
    }

    public Builder setClient(Provider<HttpClient> clientProvider) {
      if (clientProvider == null) throw new NullPointerException("clientProvider");
      this.clientProvider = clientProvider;
      return this;
    }

    /**
     * Executors used for asynchronous HTTP client downloads and callbacks.
     *
     * @param httpExecutor Executor on which HTTP client calls will be made.
     * @param callbackExecutor Executor on which any {@link Callback} methods will be invoked. If this argument is
     *                         {@code null} then callback methods will be run on the same thread as the HTTP client.
     */
    public Builder setExecutors(Executor httpExecutor, Executor callbackExecutor) {
      if (httpExecutor == null) throw new NullPointerException("httpExecutor");
      if (callbackExecutor == null) callbackExecutor = new SynchronousExecutor();
      this.httpExecutor = httpExecutor;
      this.callbackExecutor = callbackExecutor;
      return this;
    }

    public Builder setHeaders(Headers headers) {
      if (headers == null) throw new NullPointerException("headers");
      this.headers = headers;
      return this;
    }

    public Builder setConverter(Converter converter) {
      if (converter == null) throw new NullPointerException("converter");
      this.converter = converter;
      return this;
    }

    public Builder setProfiler(HttpProfiler profiler) {
      if (profiler == null) throw new NullPointerException("profiler");
      this.profiler = profiler;
      return this;
    }

    public RestAdapter build() {
      if (server == null || clientProvider == null || converter == null) {
        throw new IllegalArgumentException("Server, client, and converter are required.");
      }
      return new RestAdapter(server, clientProvider, httpExecutor, callbackExecutor, headers, converter, profiler);
    }
  }

  private static class SynchronousExecutor implements Executor {
    @Override public void execute(Runnable runnable) {
      runnable.run();
    }
  }
}