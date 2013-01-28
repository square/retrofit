// Copyright 2012 Square, Inc.
package retrofit.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Named;
import javax.inject.Provider;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import retrofit.http.HttpProfiler.RequestInformation;

import static retrofit.http.Utils.SynchronousExecutor;

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
  static final String UTF_8 = "UTF-8";

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
    if (!type.isInterface()) {
      throw new IllegalArgumentException("Only interface endpoint definitions are supported.");
    }
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
        new RestHandler(type));
  }

  private class RestHandler implements InvocationHandler {
    private final Class<?> declaringType;
    private final Map<Method, MethodDetails> methodDetailsCache =
        new LinkedHashMap<Method, MethodDetails>();

    RestHandler(Class<?> declaringType) {
      this.declaringType = declaringType;
    }

    @SuppressWarnings("unchecked")
    @Override public Object invoke(Object proxy, Method method, final Object[] args)
        throws InvocationTargetException, IllegalAccessException {
      // If the method is not a direct member of the interface then defer to normal invocation.
      if (method.getDeclaringClass() != declaringType) {
        return method.invoke(this, args);
      }

      // Load or create the details cache for the current method.
      final MethodDetails methodDetails;
      synchronized (methodDetailsCache) {
        MethodDetails tempMethodDetails = methodDetailsCache.get(method);
        if (tempMethodDetails == null) {
          tempMethodDetails = new MethodDetails(method);
          methodDetailsCache.put(method, tempMethodDetails);
        }
        methodDetails = tempMethodDetails;
      }

      if (methodDetails.isSynchronous) {
        return invokeRequest(methodDetails, args);
      }

      if (httpExecutor == null || callbackExecutor == null) {
        throw new IllegalStateException("Asynchronous invocation requires calling setExecutors.");
      }
      Callback<?> callback = (Callback<?>) args[args.length - 1];
      httpExecutor.execute(new CallbackRunnable(callback, callbackExecutor) {
        @Override public Object obtainResponse() {
          return invokeRequest(methodDetails, args);
        }
      });
      return null; // Asynchronous methods should have return type of void.
    }

    /**
     * Execute an HTTP request.
     *
     * @return HTTP response object of specified {@code type}.
     * @throws RetrofitError Thrown if any error occurs during the HTTP request.
     */
    private Object invokeRequest(MethodDetails methodDetails, Object[] args) {
      long start = System.nanoTime();

      methodDetails.init(); // Ensure all relevant method information has been loaded.

      String url = server.apiUrl();
      try {
        // Build the request and headers.
        final HttpUriRequest request = new HttpRequestBuilder(converter) //
            .setMethod(methodDetails)
            .setArgs(args)
            .setApiUrl(url)
            .setHeaders(requestHeaders)
            .build();
        url = request.getURI().toString();

        if (!methodDetails.isSynchronous) {
          // If we are executing asynchronously then update the current thread with a useful name.
          Thread.currentThread().setName(THREAD_PREFIX + url);
        }

        Object profilerObject = null;
        if (profiler != null) {
          profilerObject = profiler.beforeCall();
        }

        LOGGER.fine("Sending " + request.getMethod() + " to " + url);
        HttpResponse response = httpClientProvider.get().execute(request);
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();

        long elapsedTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        if (profiler != null) {
          RequestInformation requestInfo = getRequestInfo(server, methodDetails, request);
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
            String headerName = realHeader.getName();
            String headerValue = realHeader.getValue();

            if (HTTP.CONTENT_TYPE.equalsIgnoreCase(headerName) //
                && !UTF_8.equalsIgnoreCase(Utils.parseCharset(headerValue))) {
              throw new IOException("Only UTF-8 charset supported.");
            }

            headers[i] = new Header(headerName, headerValue);
          }
        }

        Type type = methodDetails.type;
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

  /** Cached details about an interface method. */
  static class MethodDetails {
    private static final Pattern PATH_PARAMETERS = Pattern.compile("\\{([a-z_-]*)\\}");

    final Method method;
    final boolean isSynchronous;

    private boolean loaded = false;

    Type type;
    HttpMethodType httpMethod;
    String path;
    Set<String> pathParams;
    QueryParam[] pathQueryParams;
    String[] pathNamedParams;
    int singleEntityArgumentIndex = -1;

    MethodDetails(Method method) {
      this.method = method;
      isSynchronous = parseResponseType();
    }

    synchronized void init() {
      if (loaded) return;

      parseMethodAnnotations();
      parseParameterAnnotations();

      loaded = true;
    }

    /** Loads {@link #httpMethod}, {@link #path}, and {@link #pathQueryParams}. */
    private void parseMethodAnnotations() {
      for (Annotation annotation : method.getAnnotations()) {
        Class<? extends Annotation> annotationType = annotation.annotationType();

        // Look for an HttpMethod annotation describing the request type.
        if (annotationType == GET.class
            || annotationType == POST.class
            || annotationType == PUT.class
            || annotationType == DELETE.class) {
          if (this.httpMethod != null) {
            throw new IllegalStateException(
                "Method annotated with multiple HTTP method annotations: " + method);
          }
          this.httpMethod = annotationType.getAnnotation(HttpMethod.class).value();
          try {
            path = (String) annotationType.getMethod("value").invoke(annotation);
          } catch (Exception e) {
            throw new IllegalStateException("Failed to extract URI path.", e);
          }

          pathParams = parsePathParameters(path);
        } else if (annotationType == QueryParams.class) {
          if (this.pathQueryParams != null) {
            throw new IllegalStateException(
                "QueryParam and QueryParams annotations are mutually exclusive.");
          }
          this.pathQueryParams = ((QueryParams) annotation).value();
        } else if (annotationType == QueryParam.class) {
          if (this.pathQueryParams != null) {
            throw new IllegalStateException(
                "QueryParam and QueryParams annotations are mutually exclusive.");
          }
          this.pathQueryParams = new QueryParam[] { (QueryParam) annotation };
        }
      }

      if (httpMethod == null) {
        throw new IllegalStateException(
            "Method not annotated with GET, POST, PUT, or DELETE: " + method);
      }
      if (pathQueryParams == null) {
        pathQueryParams = new QueryParam[0];
      }
    }

    /** Loads {@link #type}. Returns true if the method is synchronous. */
    private boolean parseResponseType() {
      // Synchronous methods have a non-void return type.
      Type returnType = method.getGenericReturnType();

      // Asynchronous methods should have a Callback type as the last argument.
      Type lastArgType = null;
      Class<?> lastArgClass = null;
      Type[] parameterTypes = method.getGenericParameterTypes();
      if (parameterTypes.length > 0) {
        Type typeToCheck = parameterTypes[parameterTypes.length - 1];
        lastArgType = typeToCheck;
        if (typeToCheck instanceof ParameterizedType) {
          typeToCheck = ((ParameterizedType) typeToCheck).getRawType();
        }
        if (typeToCheck instanceof Class) {
          lastArgClass = (Class<?>) typeToCheck;
        }
      }

      boolean hasReturnType = returnType != void.class;
      boolean hasCallback = lastArgClass != null && Callback.class.isAssignableFrom(lastArgClass);

      // Check for invalid configurations.
      if (hasReturnType && hasCallback) {
        throw new IllegalArgumentException(
            "Method may only have return type or Callback as last argument, not both.");
      }
      if (!hasReturnType && !hasCallback) {
        throw new IllegalArgumentException(
            "Method must have either a return type or Callback as last argument.");
      }

      if (hasReturnType) {
        type = returnType;
        return true;
      }

      lastArgType = Utils.getGenericSupertype(lastArgType, lastArgClass, Callback.class);
      if (lastArgType instanceof ParameterizedType) {
        Type[] types = ((ParameterizedType) lastArgType).getActualTypeArguments();
        for (int i = 0; i < types.length; i++) {
          Type type = types[i];
          if (type instanceof WildcardType) {
            types[i] = ((WildcardType) type).getUpperBounds()[0];
          }
        }
        type = types[0];
        return false;
      }
      throw new IllegalArgumentException(
          String.format("Last parameter of %s must be of type Callback<X> or Callback<? super X>.",
              method));
    }

    /** Loads {@link #pathNamedParams} and {@link #singleEntityArgumentIndex}. */
    private void parseParameterAnnotations() {
      Annotation[][] parameterAnnotations = method.getParameterAnnotations();
      int count = parameterAnnotations.length;
      if (!isSynchronous) {
        count -= 1; // Callback is last argument when not a synchronous method.
      }

      String[] namedParams = new String[count];
      for (int i = 0; i < count; i++) {
        for (Annotation parameterAnnotation : parameterAnnotations[i]) {
          Class<? extends Annotation> annotationType = parameterAnnotation.annotationType();
          if (annotationType == Named.class) {
            namedParams[i] = ((Named) parameterAnnotation).value();
          } else if (annotationType == SingleEntity.class) {
            if (singleEntityArgumentIndex != -1) {
              throw new IllegalStateException(
                  "Method annotated with multiple SingleEntity method annotations: " + method);
            }
            singleEntityArgumentIndex = i;
          } else {
            throw new IllegalArgumentException(
                "Method argument " + i + " not annotated with Named or SingleEntity: " + method);
          }
        }
      }
      pathNamedParams = namedParams;
    }

    /**
     * Gets the set of unique path parameters used in the given URI. If a parameter is used twice in
     * the URI, it will only show up once in the set.
     */
    static Set<String> parsePathParameters(String path) {
      Matcher m = PATH_PARAMETERS.matcher(path);
      Set<String> patterns = new LinkedHashSet<String>();
      while (m.find()) {
        patterns.add(m.group(1));
      }
      return patterns;
    }
  }

  private static void logResponseBody(String url, byte[] body, int statusCode, long elapsedTime)
      throws UnsupportedEncodingException {
    LOGGER.fine("---- HTTP " + statusCode + " from " + url + " (" + elapsedTime + "ms)");
    String bodyString = new String(body, UTF_8);
    for (int i = 0; i < body.length; i += LOG_CHUNK_SIZE) {
      int end = Math.min(bodyString.length(), i + LOG_CHUNK_SIZE);
      LOGGER.fine(bodyString.substring(i, end));
    }
    LOGGER.fine("---- END HTTP");
  }

  private static HttpProfiler.RequestInformation getRequestInfo(Server server,
      MethodDetails methodDetails, HttpUriRequest request) {
    HttpMethodType httpMethod = methodDetails.httpMethod;
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

    return new HttpProfiler.RequestInformation(profilerMethod, server.apiUrl(), methodDetails.path,
        contentLength, contentType);
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
}