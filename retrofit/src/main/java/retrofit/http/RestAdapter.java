// Copyright 2012 Square, Inc.
package retrofit.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
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
  private static final int LOG_CHUNK_SIZE = 4000;
  static final String THREAD_PREFIX = "Retrofit-";
  static final String IDLE_THREAD_NAME = THREAD_PREFIX + "Idle";

  /** Simple logging abstraction for debug messages. */
  public interface Log {
    /** Log a debug message to the appropriate console. */
    void log(String message);
  }

  private final Server server;
  private final Client.Provider clientProvider;
  private final Executor httpExecutor;
  private final Executor callbackExecutor;
  private final Headers headers;
  private final Converter converter;
  private final Profiler profiler;
  private final Log log;
  private volatile boolean debug;

  private RestAdapter(Server server, Client.Provider clientProvider, Executor httpExecutor,
      Executor callbackExecutor, Headers headers, Converter converter, Profiler profiler, Log log,
      boolean debug) {
    this.server = server;
    this.clientProvider = clientProvider;
    this.httpExecutor = httpExecutor;
    this.callbackExecutor = callbackExecutor;
    this.headers = headers;
    this.converter = converter;
    this.profiler = profiler;
    this.log = log;
    this.debug = debug;
  }

  /** Toggle debug logging on and off. */
  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  /**
   * Adapts a Java interface to a REST API.
   * <p>
   * The relative path for a given method is obtained from an annotation on the method describing
   * the request type. The names of URL parameters are retrieved from {@link Name}
   * annotations on the method parameters.
   * <p>
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
   * <p>
   * For example:
   * <pre>
   *   public interface MyApi {
   *     &#64;POST("go") // Asynchronous execution.
   *     void go(@Name("a") String a, @Name("b") int b, Callback&lt;? super MyResult> callback);
   *     &#64;POST("go") // Synchronous execution.
   *     MyResult go(@Name("a") String a, @Name("b") int b);
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
        @Override public ResponseWrapper obtainResponse() {
          return (ResponseWrapper) invokeRequest(methodDetails, args);
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

      String serverUrl = server.getUrl();
      String url = serverUrl; // Keep some url in case RequestBuilder throws an exception.
      try {
        Request request = new RequestBuilder(converter) //
            .setApiUrl(serverUrl)
            .setArgs(args)
            .setHeaders(headers.get())
            .setMethodInfo(methodDetails)
            .build();
        url = request.getUrl();

        if (!methodDetails.isSynchronous) {
          // If we are executing asynchronously then update the current thread with a useful name.
          Thread.currentThread().setName(THREAD_PREFIX + url.substring(serverUrl.length()));
        }

        if (debug) {
          request = logAndReplaceRequest(request);
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
          RequestInformation requestInfo = getRequestInfo(serverUrl, methodDetails, request);
          profiler.afterCall(requestInfo, elapsedTime, statusCode, profilerObject);
        }

        if (debug) {
          response = logAndReplaceResponse(url, response, elapsedTime);
        }

        Type type = methodDetails.type;

        if (statusCode >= 200 && statusCode < 300) { // 2XX == successful request
          // Caller requested the raw Response object directly.
          if (type.equals(Response.class)) {
            // Read the entire stream and replace with one backed by a byte[]
            response = Utils.readBodyToBytesIfNecessary(response);

            if (methodDetails.isSynchronous) {
              return response;
            }
            return new ResponseWrapper(response, response);
          }

          TypedInput body = response.getBody();
          if (body == null) {
            return new ResponseWrapper(response, null);
          }
          try {
            Object convert = converter.fromBody(body, type);
            if (methodDetails.isSynchronous) {
              return convert;
            }
            return new ResponseWrapper(response, convert);
          } catch (ConversionException e) {
            // The response body was partially read by the converter. Replace it with null.
            response = Utils.replaceResponseBody(response, null);

            throw RetrofitError.conversionError(url, response, converter, type, e);
          }
        }

        response = Utils.readBodyToBytesIfNecessary(response);
        throw RetrofitError.httpError(url, response, converter, type);
      } catch (RetrofitError e) {
        throw e; // Pass through our own errors.
      } catch (IOException e) {
        throw RetrofitError.networkError(url, e);
      } catch (Throwable t) {
        throw RetrofitError.unexpectedError(url, t);
      } finally {
        if (!methodDetails.isSynchronous) {
          Thread.currentThread().setName(IDLE_THREAD_NAME);
        }
      }
    }
  }

  /** Log request headers and body. Consumes request body and returns identical replacement. */
  private Request logAndReplaceRequest(Request request) throws IOException {
    log.log(String.format("---> HTTP %s %s", request.getMethod(), request.getUrl()));

    for (Header header : request.getHeaders()) {
      log.log(header.getName() + ": " + header.getValue());
    }

    TypedOutput body = request.getBody();
    int bodySize = 0;
    if (body != null) {
      if (!request.getHeaders().isEmpty()) {
        log.log("");
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      body.writeTo(baos);
      byte[] bodyBytes = baos.toByteArray();
      bodySize = bodyBytes.length;
      String bodyMime = body.mimeType();
      String bodyString = new String(bodyBytes, Utils.parseCharset(bodyMime));
      for (int i = 0, len = bodyString.length(); i < len; i += LOG_CHUNK_SIZE) {
        int end = Math.min(len, i + LOG_CHUNK_SIZE);
        log.log(bodyString.substring(i, end));
      }

      body = new TypedByteArray(bodyMime, bodyBytes);
    }

    log.log(String.format("---> END HTTP (%s-byte body)", bodySize));

    // Since we consumed the original request, return a new, identical one from its bytes.
    return new Request(request.getMethod(), request.getUrl(), request.getHeaders(), body);
  }

  /** Log response headers and body. Consumes response body and returns identical replacement. */
  private Response logAndReplaceResponse(String url, Response response, long elapsedTime)
      throws IOException {
    log.log(String.format("<--- HTTP %s %s (%sms)", response.getStatus(), url, elapsedTime));

    for (Header header : response.getHeaders()) {
      log.log(header.getName() + ": " + header.getValue());
    }

    TypedInput body = response.getBody();
    int bodySize = 0;
    if (body != null) {
      if (!response.getHeaders().isEmpty()) {
        log.log("");
      }

      if (!(body instanceof TypedByteArray)) {
        // Read the entire response body to we can log it and replace the original response
        response = Utils.readBodyToBytesIfNecessary(response);
        body = response.getBody();
      }

      byte[] bodyBytes = ((TypedByteArray) body).getBytes();
      bodySize = bodyBytes.length;
      String bodyMime = body.mimeType();
      String bodyCharset = Utils.parseCharset(bodyMime);
      String bodyString = new String(bodyBytes, bodyCharset);
      for (int i = 0, len = bodyString.length(); i < len; i += LOG_CHUNK_SIZE) {
        int end = Math.min(len, i + LOG_CHUNK_SIZE);
        log.log(bodyString.substring(i, end));
      }
    }

    log.log(String.format("<--- END HTTP (%s-byte body)", bodySize));

    return response;
  }

  private static Profiler.RequestInformation getRequestInfo(String serverUrl,
      RestMethodInfo methodDetails, Request request) {
    long contentLength = 0;
    String contentType = null;

    TypedOutput body = request.getBody();
    if (body != null) {
      contentLength = body.length();
      contentType = body.mimeType();
    }

    return new Profiler.RequestInformation(methodDetails.restMethod.value(), serverUrl,
        methodDetails.path, contentLength, contentType);
  }

  /**
   * Build a new {@link RestAdapter}.
   * <p>
   * Calling the following methods is required before calling {@link #build()}:
   * <ul>
   * <li>{@link #setServer(Server)}</li>
   * <li>{@link #setClient(Client.Provider)}</li>
   * <li>{@link #setConverter(Converter)}</li>
   * </ul>
   * <p>
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
    private Log log;
    private boolean debug;

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

    public Builder setLog(Log log) {
      if (log == null) throw new NullPointerException("log");
      this.log = log;
      return this;
    }

    public Builder setDebug(boolean debug) {
      this.debug = debug;
      return this;
    }

    public RestAdapter build() {
      if (server == null) {
        throw new IllegalArgumentException("Server may not be null.");
      }
      ensureSaneDefaults();
      return new RestAdapter(server, clientProvider, httpExecutor, callbackExecutor, headers,
          converter, profiler, log, debug);
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
      if (log == null) {
        log = Platform.get().defaultLog();
      }
      if (headers == null) {
        headers = Headers.NONE;
      }
    }
  }
}
