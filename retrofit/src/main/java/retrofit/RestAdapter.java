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

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import retrofit.converter.Converter;
import retrofit.http.HTTP;
import retrofit.http.Header;

import static retrofit.Utils.checkNotNull;

/**
 * Adapts a Java interface to a REST API.
 * <p>
 * API endpoints are defined as methods on an interface with annotations providing metadata about
 * the form in which the HTTP call should be made.
 * <p>
 * The relative path for a given method is obtained from an annotation on the method describing
 * the request type. The built-in methods are {@link retrofit.http.GET GET},
 * {@link retrofit.http.PUT PUT}, {@link retrofit.http.POST POST}, {@link retrofit.http.POST PATCH},
 * {@link retrofit.http.HEAD HEAD}, and {@link retrofit.http.DELETE DELETE}. You can use a custom
 * HTTP method with {@link HTTP @HTTP}.
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
 * {@link retrofit.converter.Converter#toBody(Object, java.lang.reflect.Type) toBody}
 * on the supplied {@link retrofit.converter.Converter Converter} for this instance.
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
 *   void categoryList(@Path("cat") String a, @Query("page") int b,
 *                     Callback&lt;List&lt;Item&gt;&gt; cb);
 *   &#64;POST("/category/{cat}") // Synchronous execution.
 *   List&lt;Item&gt; categoryList(@Path("cat") String a, @Query("page") int b);
 * }
 * </pre>
 * <p>
 * Calling {@link #create(Class)} with {@code MyApi.class} will validate and create a new
 * implementation of the API.
 *
 * @author Bob Lee (bob@squareup.com)
 * @author Jake Wharton (jw@squareup.com)
 */
public final class RestAdapter {
  private final Map<Class<?>, Map<Method, MethodInfo>> serviceMethodInfoCache =
      new LinkedHashMap<Class<?>, Map<Method, MethodInfo>>();

  final Endpoint endpoint;
  final Executor callbackExecutor;
  final RequestInterceptor requestInterceptor;
  final Converter converter;
  final ErrorHandler errorHandler;

  private final OkHttpClient client;
  private RxSupport rxSupport;

  private RestAdapter(Endpoint endpoint, OkHttpClient client, Executor callbackExecutor,
      RequestInterceptor requestInterceptor, Converter converter, ErrorHandler errorHandler) {
    this.endpoint = endpoint;
    this.client = client;
    this.callbackExecutor = callbackExecutor;
    this.requestInterceptor = requestInterceptor;
    this.converter = converter;
    this.errorHandler = errorHandler;
  }

  /** Create an implementation of the API defined by the specified {@code service} interface. */
  @SuppressWarnings("unchecked")
  public <T> T create(Class<T> service) {
    Utils.validateServiceClass(service);
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        new RestHandler(getMethodInfoCache(service)));
  }

  Map<Method, MethodInfo> getMethodInfoCache(Class<?> service) {
    synchronized (serviceMethodInfoCache) {
      Map<Method, MethodInfo> methodInfoCache = serviceMethodInfoCache.get(service);
      if (methodInfoCache == null) {
        methodInfoCache = new LinkedHashMap<Method, MethodInfo>();
        serviceMethodInfoCache.put(service, methodInfoCache);
      }
      return methodInfoCache;
    }
  }

  static MethodInfo getMethodInfo(Map<Method, MethodInfo> cache, Method method) {
    synchronized (cache) {
      MethodInfo methodInfo = cache.get(method);
      if (methodInfo == null) {
        methodInfo = new MethodInfo(method);
        cache.put(method, methodInfo);
      }
      return methodInfo;
    }
  }

  private class RestHandler implements InvocationHandler {
    private final Map<Method, MethodInfo> methodDetailsCache;

    RestHandler(Map<Method, MethodInfo> methodDetailsCache) {
      this.methodDetailsCache = methodDetailsCache;
    }

    @SuppressWarnings("unchecked") //
    @Override public Object invoke(Object proxy, Method method, final Object[] args)
        throws Throwable {
      // If the method is a method from Object then defer to normal invocation.
      if (method.getDeclaringClass() == Object.class) {
        return method.invoke(this, args);
      }

      MethodInfo methodInfo = getMethodInfo(methodDetailsCache, method);
      Request request = createRequest(methodInfo, args);
      switch (methodInfo.executionType) {
        case SYNC:
          return invokeSync(methodInfo, request);
        case ASYNC:
          invokeAsync(methodInfo, request, (Callback) args[args.length - 1]);
          return null; // Async has void return type.
        case RX:
          return invokeRx(methodInfo, request);
        default:
          throw new IllegalStateException("Unknown response type: " + methodInfo.executionType);
      }
    }

    private Object invokeSync(MethodInfo methodInfo, Request request) throws Throwable {
      try {
        Response response = client.newCall(request).execute();
        return createResult(methodInfo, response);
      } catch (IOException e) {
        throw handleError(RetrofitError.networkFailure(request.urlString(), e));
      } catch (RetrofitError error) {
        throw handleError(error);
      }
    }

    private Throwable handleError(RetrofitError error) {
      Throwable throwable = errorHandler.handleError(error);
      if (throwable == null) {
        return new IllegalStateException("Error handler returned null for wrapped exception.",
            error);
      }
      return throwable;
    }

    private void invokeAsync(final MethodInfo methodInfo, final Request request,
        final Callback callback) {
      Call call = client.newCall(request);
      call.enqueue(new com.squareup.okhttp.Callback() {
        @Override public void onFailure(Request request, IOException e) {
          callFailure(callback, RetrofitError.networkFailure(request.urlString(), e));
        }

        @Override public void onResponse(Response response) {
          try {
            Object result = createResult(methodInfo, response);
            callResponse(callback, result, response);
          } catch (RetrofitError error) {
            callFailure(callback, error);
          }
        }
      });
    }

    private Object invokeRx(final MethodInfo methodInfo, final Request request) {
      if (rxSupport == null) {
        if (Platform.HAS_RX_JAVA) {
          rxSupport = new RxSupport();
        } else {
          throw new IllegalStateException("Found Observable return type but RxJava not present.");
        }
      }
      return rxSupport.createRequestObservable(new RxSupport.Invoker() {
        @Override public void invoke(final Callback callback) {
          Call call = client.newCall(request);
          call.enqueue(new com.squareup.okhttp.Callback() {
            @Override public void onFailure(Request request, IOException e) {
              callback.next(RetrofitError.networkFailure(request.urlString(), e));
            }

            @Override public void onResponse(Response response) {
              try {
                Object result = createResult(methodInfo, response);
                callback.next(result);
              } catch (RetrofitError error) {
                callback.error(handleError(error));
              }
            }
          });

        }
      });
    }

    /**
     * Create the object to return to the caller for a response.
     *
     * @throws RetrofitError if any HTTP, network, or unexpected errors occurred.
     */
    private Object createResult(MethodInfo methodInfo, Response response) {
      try {
        return parseResult(methodInfo, response);
      } catch (RetrofitError error) {
        throw error; // Let our own errors pass through.
      } catch (IOException e) {
        throw RetrofitError.networkError(response, e);
      } catch (Throwable t) {
        throw RetrofitError.unexpectedError(response, t);
      }
    }

    /**
     * Parse the object to return to the caller from a response.
     *
     * @throws RetrofitError on non-2xx response codes (kind = HTTP).
     * @throws IOException on network problems reading the response data.
     * @throws RuntimeException on malformed response data.
     */
    private Object parseResult(MethodInfo methodInfo, Response response) throws IOException {
      Type type = methodInfo.responseObjectType;

      int statusCode = response.code();
      if (statusCode < 200 || statusCode >= 300) {
        response = Utils.readBodyToBytesIfNecessary(response);
        throw RetrofitError.httpError(response, converter, type);
      }

      if (type.equals(Response.class)) {
        if (!methodInfo.isStreaming) {
          // Read the entire stream and replace with one backed by a byte[].
          response = Utils.readBodyToBytesIfNecessary(response);
        }
        return response;
      }

      ResponseBody body = response.body();
      if (statusCode == 204 || statusCode == 205) {
        // HTTP 204 No Content "...response MUST NOT include a message-body"
        // HTTP 205 Reset Content "...response MUST NOT include an entity"
        if (body.contentLength() > 0) {
          throw new IllegalStateException(statusCode + " response must not include body.");
        }
        return null;
      }

      ExceptionCatchingRequestBody wrapped = new ExceptionCatchingRequestBody(body);
      try {
        return converter.fromBody(wrapped, type);
      } catch (RuntimeException e) {
        // If the underlying input stream threw an exception, propagate that rather than
        // indicating that it was a conversion exception.
        if (wrapped.threwException()) {
          throw wrapped.getThrownException();
        }
        throw e;
      }
    }

    private void callResponse(final Callback callback, final Object result,
        final Response response) {
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          callback.success(result, response);
        }
      });
    }

    private void callFailure(final Callback callback, RetrofitError error) {
      Throwable throwable = handleError(error);
      if (throwable != error) {
        Response response = error.getResponse();
        if (response != null) {
          error = RetrofitError.unexpectedError(response, throwable);
        } else {
          error = RetrofitError.unexpectedError(error.getUrl(), throwable);
        }
      }
      final RetrofitError finalError = error;
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          callback.failure(finalError);
        }
      });
    }

    private Request createRequest(MethodInfo methodInfo, Object[] args) {
      String serverUrl = endpoint.url();
      RequestBuilder requestBuilder = new RequestBuilder(serverUrl, methodInfo, converter);
      requestBuilder.setArguments(args);

      requestInterceptor.intercept(requestBuilder);

      return requestBuilder.build();
    }
  }

  /**
   * Build a new {@link RestAdapter}.
   * <p>
   * Calling {@link #endpoint} is required before calling {@link #build()}. All other methods
   * are optional.
   */
  public static class Builder {
    private Endpoint endpoint;
    private OkHttpClient client;
    private Executor callbackExecutor;
    private RequestInterceptor requestInterceptor;
    private Converter converter;
    private ErrorHandler errorHandler;

    /** API endpoint URL. */
    public Builder endpoint(String url) {
       return endpoint(Endpoint.createFixed(url));
    }

    /** API endpoint. */
    public Builder endpoint(Endpoint endpoint) {
      this.endpoint = checkNotNull(endpoint, "endpoint == null");
      return this;
    }

    /** The HTTP client used for requests. */
    public Builder client(OkHttpClient client) {
      this.client = checkNotNull(client, "client == null");
      return this;
    }

    /**
     * Executor on which any {@link Callback} methods will be invoked. If this argument is
     * {@code null} then callback methods will be run on the same thread as the HTTP client.
     */
    public Builder callbackExecutor(Executor callbackExecutor) {
      if (callbackExecutor == null) {
        callbackExecutor = new Utils.SynchronousExecutor();
      }
      this.callbackExecutor = callbackExecutor;
      return this;
    }

    /** A request interceptor for adding data to every request. */
    public Builder requestInterceptor(RequestInterceptor requestInterceptor) {
      this.requestInterceptor = checkNotNull(requestInterceptor, "requestInterceptor == null");
      return this;
    }

    /** The converter used for serialization and deserialization of objects. */
    public Builder converter(Converter converter) {
      this.converter = checkNotNull(converter, "converter == null");
      return this;
    }

    /**
     * The error handler allows you to customize the type of exception thrown for errors on
     * synchronous requests.
     */
    public Builder errorHandler(ErrorHandler errorHandler) {
      this.errorHandler = checkNotNull(errorHandler, "errorHandler == null");
      return this;
    }

    /** Create the {@link RestAdapter} instances. */
    public RestAdapter build() {
      checkNotNull(endpoint, "Endpoint required.");
      ensureSaneDefaults();
      return new RestAdapter(endpoint, client, callbackExecutor, requestInterceptor, converter,
          errorHandler);
    }

    private void ensureSaneDefaults() {
      if (converter == null) {
        converter = Platform.get().defaultConverter();
      }
      if (client == null) {
        client = Platform.get().defaultClient();
      }
      if (callbackExecutor == null) {
        callbackExecutor = Platform.get().defaultCallbackExecutor();
      }
      if (errorHandler == null) {
        errorHandler = ErrorHandler.DEFAULT;
      }
      if (requestInterceptor == null) {
        requestInterceptor = RequestInterceptor.NONE;
      }
    }
  }
}
