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

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
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
 * {@link retrofit.http.PUT PUT}, {@link retrofit.http.POST POST}, {@link retrofit.http.PATCH
 * PATCH}, {@link retrofit.http.HEAD HEAD}, and {@link retrofit.http.DELETE DELETE}. You can use a
 * custom HTTP method with {@link HTTP @HTTP}.
 * <p>
 * Method parameters can be used to replace parts of the URL by annotating them with
 * {@link retrofit.http.Path @Path}. Replacement sections are denoted by an identifier surrounded
 * by curly braces (e.g., "{foo}"). To add items to the query string of a URL use
 * {@link retrofit.http.Query @Query}.
 * <p>
 * The body of a request is denoted by the {@link retrofit.http.Body @Body} annotation. The object
 * will be converted to request representation by a call to
 * {@link Converter#toBody(Object) toBody}
 * on the supplied {@link Converter} for this instance. A {@link RequestBody} can also be used
 * which will not use the {@code Converter}.
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
 * By default, methods return a {@link Call} which represents the HTTP request. The generic
 * parameter of the call is the response body type and will be converted by a call to
 * {@link Converter#fromBody(ResponseBody) fromBody} on the supplied {@link Converter} for
 * this instance. {@link ResponseBody} can also be used which will not use the {@code Converter}.
 * <p>
 * For example:
 * <pre>
 * public interface CategoryService {
 *   &#64;POST("/category/{cat}")
 *   Call&lt;List&lt;Item&gt;&gt; categoryList(@Path("cat") String a, @Query("page") int b);
 * }
 * </pre>
 * <p>
 * Calling {@link #create(Class) create()} with {@code CategoryService.class} will validate the
 * annotations and create a new implementation of the service definition.
 *
 * @author Bob Lee (bob@squareup.com)
 * @author Jake Wharton (jw@squareup.com)
 */
public final class Retrofit {
  private final Map<Method, MethodInfo> methodInfoCache = new LinkedHashMap<>();
  private final OkHttpClient client;
  private final Endpoint endpoint;
  private final Converter.Factory converterFactory;
  private final CallAdapter.Factory adapterFactory;

  private Retrofit(OkHttpClient client, Endpoint endpoint, Converter.Factory converterFactory,
      CallAdapter.Factory adapterFactory) {
    this.client = client;
    this.endpoint = endpoint;
    this.converterFactory = converterFactory;
    this.adapterFactory = adapterFactory;
  }

  /** Create an implementation of the API defined by the {@code service} interface. */
  @SuppressWarnings("unchecked") // Single interface proxy creation guarded by parameter safety.
  public <T> T create(Class<T> service) {
    Utils.validateServiceClass(service);
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        handler);
  }

  private final InvocationHandler handler = new InvocationHandler() {
    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      // If the method is a method from Object then defer to normal invocation.
      if (method.getDeclaringClass() == Object.class) {
        return method.invoke(this, args);
      }
      return invokeMethod(method, args);
    }
  };

  // Package-private avoids synthetic accessor method from InvocationHandler. Also for testing.
  Object invokeMethod(Method method, Object... args) {
    MethodInfo methodInfo = loadMethodInfo(method);
    Converter<?> responseConverter = methodInfo.responseConverter;
    Call call = new OkHttpCall<>(client, endpoint, responseConverter, methodInfo, args);
    return methodInfo.adapter.adapt(call);
  }

  private MethodInfo loadMethodInfo(Method method) {
    MethodInfo methodInfo = methodInfoCache.get(method);
    if (methodInfo == null) {
      synchronized (methodInfoCache) {
        methodInfo = methodInfoCache.get(method);
        if (methodInfo == null) {
          methodInfo = new MethodInfo(method, adapterFactory, converterFactory);
          methodInfoCache.put(method, methodInfo);
        }
      }
    }
    return methodInfo;
  }

  public OkHttpClient client() {
    return client;
  }

  public Endpoint endpoint() {
    return endpoint;
  }

  /**
   * TODO
   * <p>
   * May be null.
   */
  public Converter.Factory converterFactory() {
    return converterFactory;
  }

  public CallAdapter.Factory callAdapterFactory() {
    return adapterFactory;
  }

  /**
   * Build a new {@link Retrofit}.
   * <p>
   * Calling {@link #endpoint} is required before calling {@link #build()}. All other methods
   * are optional.
   */
  public static class Builder {
    private OkHttpClient client;
    private Endpoint endpoint;
    private Converter.Factory converterFactory;
    private CallAdapter.Factory adapterFactory;

    /** The HTTP client used for requests. */
    public Builder client(OkHttpClient client) {
      this.client = checkNotNull(client, "client == null");
      return this;
    }

    /** API endpoint URL. */
    public Builder endpoint(String url) {
      checkNotNull(url, "url == null");
      HttpUrl httpUrl = HttpUrl.parse(url);
      if (httpUrl == null) {
        throw new IllegalArgumentException("Illegal URL: " + url);
      }
      return endpoint(httpUrl);
    }

    /** API endpoint URL. */
    public Builder endpoint(final HttpUrl url) {
      checkNotNull(url, "url == null");
      return endpoint(new Endpoint() {
        @Override public HttpUrl url() {
          return url;
        }
      });
    }

    /** API endpoint. */
    public Builder endpoint(Endpoint endpoint) {
      this.endpoint = checkNotNull(endpoint, "endpoint == null");
      return this;
    }

    /** The converter used for serialization and deserialization of objects. */
    public Builder converterFactory(Converter.Factory converterFactory) {
      this.converterFactory = checkNotNull(converterFactory, "converterFactory == null");
      return this;
    }

    /**
     * TODO
     */
    public Builder callAdapterFactory(CallAdapter.Factory factory) {
      this.adapterFactory = checkNotNull(factory, "factory == null");
      return this;
    }

    /** Create the {@link Retrofit} instances. */
    public Retrofit build() {
      if (endpoint == null) {
        throw new IllegalStateException("Endpoint required.");
      }

      // Set any platform-appropriate defaults for unspecified components.
      if (client == null) {
        client = Platform.get().defaultClient();
      }
      if (adapterFactory == null) {
        adapterFactory = Platform.get().defaultCallAdapterFactory();
      }

      return new Retrofit(client, endpoint, converterFactory, adapterFactory);
    }
  }
}
