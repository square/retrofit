// Copyright 2012 Square, Inc.
package retrofit.http;

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
import javax.inject.Provider;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import retrofit.http.HttpProfiler.RequestInformation;

/**
 * Converts Java method calls to Rest calls.
 *
 * @author Bob Lee (bob@squareup.com)
 * @author Jake Wharton (jw@squareup.com)
 */
public class RestAdapter {
  private static final Logger LOGGER = Logger.getLogger(RestAdapter.class.getName());
  private static final int LOG_CHUNK_SIZE = 4000;
  static final String THREAD_PREFIX = "Retrofit-";

  private final Server server;
  private final Provider<HttpClient> httpClientProvider;
  private final Executor httpExecutor;
  private final Executor callbackExecutor;
  private final Headers requestHeaders;
  private final Converter converter;
  private final HttpProfiler profiler;

  private RestAdapter(Server server, Provider<HttpClient> httpClientProvider, Executor httpExecutor,
      Executor callbackExecutor, Headers requestHeaders, Converter converter,
      HttpProfiler profiler) {
    this.server = server;
    this.httpClientProvider = httpClientProvider;
    this.httpExecutor = httpExecutor;
    this.callbackExecutor = callbackExecutor;
    this.requestHeaders = requestHeaders;
    this.converter = converter;
    this.profiler = profiler;
  }

  /**
   * Adapts a Java interface to a REST API.
   * <p/>
   * The relative path for a given method is obtained from a {@link GET}, {@link POST}, {@link PUT},
   * or {@link DELETE} annotation on the method. Gets the names of URL parameters from
   * {@link javax.inject.Named Named} annotations on the method parameters.
   * <p/>
   * HTTP requests happen in one of two ways:
   * <ul>
   * <li>On the provided HTTP {@link Executor} with callbacks marshaled to the callback
   * {@link Executor}. The last method parameter should be of type {@link Callback}. The HTTP
   * response will be converted to the callback's parameter type using the specified
   * {@link Converter}. If the callback parameter type uses a wildcard, the lower bound will be used
   * as the conversion type.</li>
   * <li>On the current thread returning the response or throwing a {@link RetrofitError}. The HTTP
   * response will be converted to the method's return type using the specified
   * {@link Converter}.</li>
   * </ul>
   * <p/>
   * For example:
   * <pre>
   *   public interface MyApi {
   *     &#64;POST("go") // Asynchronous execution.
   *     void go(@Named("a") String a, @Named("b") int b, Callback&lt;? super MyResult> callback);
   *     &#64;POST("go") // Synchronous execution.
   *     MyResult go(@Named("a") String a, @Named("b") int b);
   *   }
   * </pre>
   *
   * @param type to implement
   */
  @SuppressWarnings("unchecked")
  public <T> T create(Class<T> type) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
        new RestHandler());
  }

  private class RestHandler implements InvocationHandler {
    private final Map<Method, Type> responseTypeCache = new HashMap<Method, Type>();

    @SuppressWarnings("unchecked") @Override
    public Object invoke(Object proxy, final Method method, final Object[] args) {
      if (methodWantsSynchronousInvocation(method)) {
        return invokeRequest(method, args, true);
      } else {
        if (httpExecutor == null || callbackExecutor == null) {
          throw new IllegalStateException("Asynchronous invocation requires calling setExecutors.");
        }
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
     * @throws RetrofitError Thrown if any error occurs during the HTTP request.
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
            .setHeaders(requestHeaders)
            .build();
        url = request.getURI().toString();

        if (!isSynchronousInvocation) {
          // If we are executing asynchronously then update the current thread with a useful name.
          Thread.currentThread().setName(THREAD_PREFIX + url);
        }

        // Determine deserialization type by return type or generic parameter to Callback argument.
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

        org.apache.http.Header[] realHeaders = response.getAllHeaders();
        Header[] headers = null;
        if (realHeaders != null) {
          headers = new Header[realHeaders.length];
          for (int i = 0; i < realHeaders.length; i++) {
            org.apache.http.Header realHeader = realHeaders[i];
            headers[i] = new Header(realHeader.getName(), realHeader.getValue());
          }
        }

        if (statusCode >= 200 && statusCode < 300) { // 2XX == successful request
          try {
            return converter.to(body, type);
          } catch (ConversionException e) {
            throw RetrofitError.conversionError(url, converter, statusCode, headers, body, type, e);
          }
        }
        throw RetrofitError.httpError(url, converter, statusCode, headers, body, type);
      } catch (RetrofitError e) {
        throw e; // Pass through our own errors.
      } catch (IOException e) {
        throw RetrofitError.networkError(url, e);
      } catch (Throwable t) {
        throw RetrofitError.unexpectedError(url, t);
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

  private static HttpProfiler.RequestInformation getRequestInfo(Server server, Method method,
      HttpUriRequest request) {
    RequestLine requestLine = RequestLine.fromMethod(method);
    HttpMethodType httpMethod = requestLine.getHttpMethod();
    HttpProfiler.Method profilerMethod = httpMethod.profilerMethod();

    long contentLength = 0;
    String contentType = null;
    if (request instanceof HttpEntityEnclosingRequestBase) {
      HttpEntityEnclosingRequestBase entityReq = (HttpEntityEnclosingRequestBase) request;
      HttpEntity entity = entityReq.getEntity();
      contentLength = entity.getContentLength();

      org.apache.http.Header entityContentType = entity.getContentType();
      contentType = entityContentType != null ? entityContentType.getValue() : null;
    }

    return new HttpProfiler.RequestInformation(profilerMethod, server.apiUrl(),
        requestLine.getRelativePath(), contentLength, contentType);
  }

  /**
   * Determine whether or not execution for a method should be done synchronously.
   *
   * @throws IllegalArgumentException if the supplied {@code method} has both a return type and
   * {@link Callback} argument or neither of the two.
   */
  static boolean methodWantsSynchronousInvocation(Method method) {
    boolean hasReturnType = method.getReturnType() != void.class;

    Class<?>[] parameterTypes = method.getParameterTypes();
    boolean hasCallback = parameterTypes.length > 0 && Callback.class.isAssignableFrom(
        parameterTypes[parameterTypes.length - 1]);

    if ((hasReturnType && hasCallback) || (!hasReturnType && !hasCallback)) {
      throw new IllegalArgumentException(
          "Method must have either a return type or Callback as last argument.");
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
    throw new IllegalArgumentException(String.format(
        "Last parameter of %s must be of type Callback<X,Y,Z> or Callback<? super X,..,..>.",
        method));
  }

  /**
   * Build a new {@link RestAdapter}.
   * <p/>
   * Calling the following methods is required before calling {@link #build()}:
   * <ul>
   * <li>{@link #setServer(Server)}</li>
   * <li>{@link #setClient(javax.inject.Provider)}</li>
   * <li>{@link #setConverter(Converter)}</li>
   * </ul>
   * If you are using asynchronous execution (i.e., with {@link Callback Callbacks}) the following
   * is also required:
   * <ul>
   * <li>{@link #setExecutors(java.util.concurrent.Executor, java.util.concurrent.Executor)}</li>
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
     * @param callbackExecutor Executor on which any {@link Callback} methods will be invoked. If
     * this argument is {@code null} then callback methods will be run on the same thread as the
     * HTTP client.
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
      if (server == null) {
        throw new IllegalArgumentException("Server may not be null.");
      }
      ensureSaneDefaults();
      return new RestAdapter(server, clientProvider, httpExecutor, callbackExecutor, headers,
          converter, profiler);
    }

    private void ensureSaneDefaults() {
      if (converter == null) {
        converter = Platform.get().defaultConverter();
      }
      if (clientProvider == null) {
        clientProvider = Platform.get().defaultHttpClient();
      }
      if (httpExecutor == null) {
        httpExecutor = Platform.get().defaultHttpExecutor();
      }
      if (callbackExecutor == null) {
        callbackExecutor = Platform.get().defaultCallbackExecutor();
      }
    }
  }

  static class SynchronousExecutor implements Executor {
    @Override public void execute(Runnable runnable) {
      runnable.run();
    }
  }
}