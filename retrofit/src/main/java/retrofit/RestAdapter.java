/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import retrofit.Profiler.RequestInformation;
import retrofit.client.Client;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.MimeUtil;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * Adapts a Java interface to a REST API.
 * <p>
 * API endpoints are defined as methods on an interface with annotations providing metadata about
 * the form in which the HTTP call should be made.
 * <p>
 * The relative path for a given method is obtained from an annotation on the method describing
 * the request type. The built-in methods are {@link retrofit.http.GET GET},
 * {@link retrofit.http.PUT PUT}, {@link retrofit.http.POST POST}, {@link retrofit.http.HEAD HEAD},
 * and {@link retrofit.http.DELETE DELETE}. You can define your own HTTP method by creating an
 * annotation that takes a {code String} value and itself is annotated with
 * {@link retrofit.http.RestMethod @RestMethod}.
 * <p>
 * Method parameters can be used to replace parts of the URL by annotating them with
 * {@link retrofit.http.Path @Path}. Replacement sections are denoted by an identifier surrounded
 * by curly braces (e.g., "{foo}"). To add items to the query string of a URL use
 * {@link retrofit.http.Query @Query}.
 * <p>
 * HTTP requests happen in one of two ways:
 * <ul>
 * <li>On the provided HTTP {@link Executor} with callbacks marshaled to the callback
 * {@link Executor}. The last method parameter should be of type {@link Callback}. The HTTP
 * response will be converted to the callback's parameter type using the specified
 * {@link retrofit.converter.Converter Converter}. If the callback parameter type uses a wildcard,
 * the lower bound will be used as the conversion type.
 * <li>On the current thread returning the response or throwing a {@link RetrofitError}. The HTTP
 * response will be converted to the method's return type using the specified
 * {@link retrofit.converter.Converter Converter}.
 * </ul>
 * <p>
 * The body of a request is denoted by the {@link retrofit.http.Body @Body} annotation. The object
 * will be converted to request representation by a call to
 * {@link retrofit.converter.Converter#toBody(Object) toBody} on the supplied
 * {@link retrofit.converter.Converter Converter} for this instance. The body can also be a
 * {@link TypedOutput} where it will be used directly.
 * <p>
 * Alternative request body formats are supported by method annotations and corresponding parameter
 * annotations:
 * <ul>
 * <li>{@link retrofit.http.FormUrlEncoded @FormUrlEncoded} - Form-encoded data with key-value
 * pairs specified by the {@link retrofit.http.Field @Field} parameter annotation.
 * <li>{@link retrofit.http.Multipart @Multipart} - RFC 2387-compliant multi-part data with parts
 * specified by the {@link retrofit.http.Part @Part} parameter annotation.
 * </ul>
 * <p>
 * Additional static headers can be added for an endpoint using the
 * {@link retrofit.http.Headers @Headers} method annotation. For per-request control over a header
 * annotate a parameter with {@link Header @Header}.
 * <p>
 * For example:
 * <pre>
 * public interface MyApi {
 *   &#64;POST("/category/{cat}") // Asynchronous execution.
 *   void categoryList(@Path("cat") String a, @Query("page") int b, Callback&lt;List&lt;Item>> cb);
 *   &#64;POST("/category/{cat}") // Synchronous execution.
 *   List&lt;Item> categoryList(@Path("cat") String a, @Query("page") int b);
 * }
 * </pre>
 * <p>
 * Calling {@link #create(Class)} with {@code MyApi.class} will validate and create a new
 * implementation of the API.
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
  private final RequestInterceptor requestInterceptor;
  private final Converter converter;
  private final Profiler profiler;
  private final ErrorHandler errorHandler;
  private final Log log;
  private volatile boolean debug;

  private RestAdapter(Server server, Client.Provider clientProvider, Executor httpExecutor,
      Executor callbackExecutor, RequestInterceptor requestInterceptor, Converter converter,
      Profiler profiler, ErrorHandler errorHandler, Log log, boolean debug) {
    this.server = server;
    this.clientProvider = clientProvider;
    this.httpExecutor = httpExecutor;
    this.callbackExecutor = callbackExecutor;
    this.requestInterceptor = requestInterceptor;
    this.converter = converter;
    this.profiler = profiler;
    this.errorHandler = errorHandler;
    this.log = log;
    this.debug = debug;
  }

  /** Toggle debug logging on or off. */
  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  /** Create an implementation of the API defined by the specified {@code service} interface. */
  @SuppressWarnings("unchecked")
  public <T> T create(Class<T> service) {
    if (!service.isInterface()) {
      throw new IllegalArgumentException("Only interface endpoint definitions are supported.");
    }
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        new RestHandler());
  }

  private class RestHandler implements InvocationHandler {
    private final Map<Method, RestMethodInfo> methodDetailsCache =
        new LinkedHashMap<Method, RestMethodInfo>();

    @SuppressWarnings("unchecked") //
    @Override public Object invoke(Object proxy, Method method, final Object[] args)
        throws Throwable {
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
        try {
          return invokeRequest(methodDetails, args);
        } catch (RetrofitError error) {
          Throwable newError = errorHandler.handleError(error);
          if (newError == null) {
            throw new IllegalStateException("Error handler returned null for wrapped exception.",
                error);
          }
          throw newError;
        }
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
     * @return HTTP response object of specified {@code type} or {@code null}.
     * @throws RetrofitError if any error occurs during the HTTP request.
     */
    private Object invokeRequest(RestMethodInfo methodDetails, Object[] args) {
      methodDetails.init(); // Ensure all relevant method information has been loaded.

      String serverUrl = server.getUrl();
      String url = serverUrl; // Keep some url in case RequestBuilder throws an exception.
      try {
        RequestBuilder requestBuilder = new RequestBuilder(converter, methodDetails);
        requestBuilder.setApiUrl(serverUrl);
        requestBuilder.setArguments(args);

        requestInterceptor.intercept(requestBuilder);

        Request request = requestBuilder.build();
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
          //noinspection unchecked
          profiler.afterCall(requestInfo, elapsedTime, statusCode, profilerObject);
        }

        if (debug) {
          response = logAndReplaceResponse(url, response, elapsedTime);
        }

        Type type = methodDetails.responseObjectType;

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
      String bodyString = new String(bodyBytes, MimeUtil.parseCharset(bodyMime));
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
      String bodyCharset = MimeUtil.parseCharset(bodyMime);
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

    return new Profiler.RequestInformation(methodDetails.requestMethod, serverUrl,
        methodDetails.requestUrl, contentLength, contentType);
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
    private RequestInterceptor requestInterceptor;
    private Converter converter;
    private Profiler profiler;
    private ErrorHandler errorHandler;
    private Log log;
    private boolean debug;

    /** API server base URL. */
    public Builder setServer(String endpoint) {
      if (endpoint == null || endpoint.trim().length() == 0) {
        throw new NullPointerException("Server may not be blank.");
      }
      return setServer(new Server(endpoint));
    }

    /** API server. */
    public Builder setServer(Server server) {
      if (server == null) {
        throw new NullPointerException("Server may not be null.");
      }
      this.server = server;
      return this;
    }

    /** The HTTP client used for requests. */
    public Builder setClient(final Client client) {
      if (client == null) {
        throw new NullPointerException("Client may not be null.");
      }
      return setClient(new Client.Provider() {
        @Override public Client get() {
          return client;
        }
      });
    }

    /** The HTTP client used for requests. */
    public Builder setClient(Client.Provider clientProvider) {
      if (clientProvider == null) {
        throw new NullPointerException("Client provider may not be null.");
      }
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
      if (httpExecutor == null) {
        throw new NullPointerException("HTTP executor may not be null.");
      }
      if (callbackExecutor == null) {
        callbackExecutor = new Utils.SynchronousExecutor();
      }
      this.httpExecutor = httpExecutor;
      this.callbackExecutor = callbackExecutor;
      return this;
    }

    /** A request interceptor for adding data to every request. */
    public Builder setRequestInterceptor(RequestInterceptor requestInterceptor) {
      if (requestInterceptor == null) {
        throw new NullPointerException("Request interceptor may not be null.");
      }
      this.requestInterceptor = requestInterceptor;
      return this;
    }

    /** The converter used for serialization and deserialization of objects. */
    public Builder setConverter(Converter converter) {
      if (converter == null) {
        throw new NullPointerException("Converter may not be null.");
      }
      this.converter = converter;
      return this;
    }

    /** Set the profiler used to measure requests. */
    public Builder setProfiler(Profiler profiler) {
      if (profiler == null) {
        throw new NullPointerException("Profiler may not be null.");
      }
      this.profiler = profiler;
      return this;
    }

    /**
     * The error handler allows you to customize the type of exception thrown for errors on
     * synchronous requests.
     */
    public Builder setErrorHandler(ErrorHandler errorHandler) {
      if (errorHandler == null) {
        throw new NullPointerException("Error handler may not be null.");
      }
      this.errorHandler = errorHandler;
      return this;
    }

    /** Configure debug logging mechanism. */
    public Builder setLog(Log log) {
      if (log == null) {
        throw new NullPointerException("Log may not be null.");
      }
      this.log = log;
      return this;
    }

    /** Enable debug logging. */
    public Builder setDebug(boolean debug) {
      this.debug = debug;
      return this;
    }

    /** Create the {@link RestAdapter} instances. */
    public RestAdapter build() {
      if (server == null) {
        throw new IllegalArgumentException("Server may not be null.");
      }
      ensureSaneDefaults();
      return new RestAdapter(server, clientProvider, httpExecutor, callbackExecutor,
          requestInterceptor, converter, profiler, errorHandler, log, debug);
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
      if (errorHandler == null) {
        errorHandler = ErrorHandler.DEFAULT;
      }
      if (log == null) {
        log = Platform.get().defaultLog();
      }
      if (requestInterceptor == null) {
        requestInterceptor = RequestInterceptor.NONE;
      }
    }
  }
}
