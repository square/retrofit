// Copyright 2012 Square, Inc.
package retrofit.http;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.protocol.HTTP;
import retrofit.http.Profiler.RequestInformation;
import retrofit.http.client.Client;
import retrofit.http.client.Request;
import retrofit.http.client.Response;
import retrofit.http.mime.TypedByteArray;
import retrofit.http.mime.TypedInput;
import retrofit.http.mime.TypedOutput;

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
  private final Client.Provider clientProvider;
  private final Executor httpExecutor;
  private final Executor callbackExecutor;
  private final Headers headers;
  private final Converter converter;
  private final Profiler profiler;

  private RestAdapter(Server server, Client.Provider clientProvider, Executor httpExecutor,
      Executor callbackExecutor, Headers headers, Converter converter, Profiler profiler) {
    this.server = server;
    this.clientProvider = clientProvider;
    this.httpExecutor = httpExecutor;
    this.callbackExecutor = callbackExecutor;
    this.headers = headers;
    this.converter = converter;
    this.profiler = profiler;
  }

  /**
   * Adapts a Java interface to a REST API.
   * <p/>
   * The relative path for a given method is obtained from an annotation on the method describing
   * the request type. The names of URL parameters are retrieved from {@link Name}
   * annotations on the method parameters.
   * <p/>
   * HTTP requests happen in one of two ways:
   * <ul>
   * <li>On the provided HTTP {@link Executor} with callbacks marshaled to the callback
   * {@link Executor}. The last method parameter should be of type {@link Callback}. The HTTP
   * response will be converted to the callback's parameter type using the specified
   * {@link Converter}. If the callback parameter type uses a wildcard, the lower bound will be
   * used as the conversion type.</li>
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
        new RestHandler());
  }

  private class RestHandler implements InvocationHandler {
    private final Map<Method, RestMethodInfo> methodDetailsCache =
        new LinkedHashMap<Method, RestMethodInfo>();

    @SuppressWarnings("unchecked") //
    @Override public Object invoke(Object proxy, Method method, final Object[] args)
        throws InvocationTargetException, IllegalAccessException {
      // If the method is a method from Object then defer to normal invocation.
      if (method.getDeclaringClass() == Object.class) {
        return method.invoke(this, args);
      }

      // Load or create the details cache for the current method.
      final RestMethodInfo methodDetails;
      synchronized (methodDetailsCache) {
        RestMethodInfo tempMethodDetails = methodDetailsCache.get(method);
        if (tempMethodDetails == null) {
          tempMethodDetails = new RestMethodInfo(method);
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
    private Object invokeRequest(RestMethodInfo methodDetails, Object[] args) {
      methodDetails.init(); // Ensure all relevant method information has been loaded.

      String url = server.apiUrl();
      try {
        Request request = new RequestBuilder(converter) //
            .setApiUrl(server.apiUrl())
            .setArgs(args)
            .setHeaders(headers.get())
            .setMethodInfo(methodDetails)
            .build();
        url = request.getUrl();
        LOGGER.fine("Sending " + request.getMethod() + " to " + url);

        if (!methodDetails.isSynchronous) {
          // If we are executing asynchronously then update the current thread with a useful name.
          Thread.currentThread().setName(THREAD_PREFIX + url);
        }

        Object profilerObject = null;
        if (profiler != null) {
          profilerObject = profiler.beforeCall();
        }

        long start = System.nanoTime();
        Response response = clientProvider.get().execute(request);
        long elapsedTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        int statusCode = response.getStatus();
        if (profiler != null) {
          RequestInformation requestInfo = getRequestInfo(server, methodDetails, request);
          profiler.afterCall(requestInfo, elapsedTime, statusCode, profilerObject);
        }

        TypedInput body = response.getBody();
        if (LOGGER.isLoggable(Level.FINE)) {
          // Replace the response since the logger needs to consume the entire input stream.
          body = logResponseBody(url, response.getStatus(), body, elapsedTime);
        }

        List<Header> headers = response.getHeaders();
        for (Header header : headers) {
          if (HTTP.CONTENT_TYPE.equalsIgnoreCase(header.getName()) //
              && !UTF_8.equalsIgnoreCase(Utils.parseCharset(header.getValue()))) {
            throw new IOException("Only UTF-8 charset supported.");
          }
        }

        Type type = methodDetails.type;
        if (statusCode >= 200 && statusCode < 300) { // 2XX == successful request
          if (type.equals(Response.class)) {
            return response;
          }
          if (body == null) {
            return null;
          }
          try {
            return converter.fromBody(body, type);
          } catch (ConversionException e) {
            throw RetrofitError.conversionError(url, response, converter, type, e);
          }
        }
        throw RetrofitError.httpError(url, response, converter, type);
      } catch (RetrofitError e) {
        throw e; // Pass through our own errors.
      } catch (IOException e) {
        throw RetrofitError.networkError(url, e);
      } catch (Throwable t) {
        throw RetrofitError.unexpectedError(url, t);
      }
    }
  }

  /** Log response data. Returns replacement {@link TypedInput}. */
  private static TypedInput logResponseBody(String url, int statusCode, TypedInput body,
      long elapsedTime) throws IOException {
    LOGGER.fine("---- HTTP " + statusCode + " from " + url + " (" + elapsedTime + "ms)");

    byte[] bodyBytes = Utils.streamToBytes(body.in());
    String bodyString = new String(bodyBytes, UTF_8);
    for (int i = 0; i < bodyString.length(); i += LOG_CHUNK_SIZE) {
      int end = Math.min(bodyString.length(), i + LOG_CHUNK_SIZE);
      LOGGER.fine(bodyString.substring(i, end));
    }

    LOGGER.fine("---- END HTTP");

    // Since we consumed the entire input stream, return a new, identical one from its bytes.
    return new TypedByteArray(body.mimeType(), bodyBytes);
  }

  private static Profiler.RequestInformation getRequestInfo(Server server,
      RestMethodInfo methodDetails, Request request) {
    long contentLength = 0;
    String contentType = null;

    TypedOutput body = request.getBody();
    if (body != null) {
      contentLength = body.length();
      contentType = body.mimeType();
    }

    return new Profiler.RequestInformation(methodDetails.restMethod.value(), server.apiUrl(),
        methodDetails.path, contentLength, contentType);
  }

  /**
   * Build a new {@link RestAdapter}.
   * <p/>
   * Calling the following methods is required before calling {@link #build()}:
   * <ul>
   * <li>{@link #setServer(Server)}</li>
   * <li>{@link #setClient(Client.Provider)}</li>
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
    private Client.Provider clientProvider;
    private Executor httpExecutor;
    private Executor callbackExecutor;
    private Headers headers;
    private Converter converter;
    private Profiler profiler;

    public Builder setServer(String endpoint) {
      if (endpoint == null) throw new NullPointerException("endpoint");
      return setServer(new Server(endpoint));
    }

    public Builder setServer(Server server) {
      if (server == null) throw new NullPointerException("server");
      this.server = server;
      return this;
    }

    public Builder setClient(final Client client) {
      if (client == null) throw new NullPointerException("client");
      return setClient(new Client.Provider() {
        @Override public Client get() {
          return client;
        }
      });
    }

    public Builder setClient(Client.Provider clientProvider) {
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

    public Builder setProfiler(Profiler profiler) {
      if (profiler == null) throw new NullPointerException("profiler");
      this.profiler = profiler;
      return this;
    }

    public RestAdapter build() {
      if (server == null) {
        throw new IllegalArgumentException("Server may not be null.");
      }
      ensureSaneDefaults();
      return new RestAdapter(server, clientProvider, httpExecutor, callbackExecutor,
          headers, converter, profiler);
    }

    private void ensureSaneDefaults() {
      if (converter == null) {
        converter = Platform.get().defaultConverter();
      }
      if (clientProvider == null) {
        clientProvider = Platform.get().defaultClient();
      }
      if (httpExecutor == null) {
        httpExecutor = Platform.get().defaultHttpExecutor();
      }
      if (callbackExecutor == null) {
        callbackExecutor = Platform.get().defaultCallbackExecutor();
      }
      if (headers == null) {
        headers = Headers.NONE;
      }
    }
  }
}