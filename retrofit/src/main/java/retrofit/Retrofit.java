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
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
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
 * will be converted to request representation by one of the {@link Converter.Factory} instances.
 * A {@link RequestBody} can also be used for a raw representation.
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
 * parameter of the call is the response body type and will be converted by one of the
 * {@link Converter.Factory} instances. {@link ResponseBody} can also be used for a raw
 * representation. {@link Void} can be used if you do not care about the body contents.
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
  private final Map<Method, MethodHandler<?>> methodHandlerCache = new LinkedHashMap<>();

  private final OkHttpClient client;
  private final BaseUrl baseUrl;
  private final List<Converter.Factory> converterFactories;
  private final List<CallAdapter.Factory> adapterFactories;
  private final Executor callbackExecutor;
  private final boolean validateEagerly;

  private Retrofit(OkHttpClient client, BaseUrl baseUrl, List<Converter.Factory> converterFactories,
      List<CallAdapter.Factory> adapterFactories, Executor callbackExecutor,
      boolean validateEagerly) {
    this.client = client;
    this.baseUrl = baseUrl;
    this.converterFactories = converterFactories;
    this.adapterFactories = adapterFactories;
    this.callbackExecutor = callbackExecutor;
    this.validateEagerly = validateEagerly;
  }

  /** Create an implementation of the API defined by the {@code service} interface. */
  @SuppressWarnings("unchecked") // Single-interface proxy creation guarded by parameter safety.
  public <T> T create(final Class<T> service) {
    Utils.validateServiceInterface(service);
    if (validateEagerly) {
      eagerlyValidateMethods(service);
    }
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        new InvocationHandler() {
          private final Platform platform = Platform.get();

          @Override public Object invoke(Object proxy, Method method, Object... args)
              throws Throwable {
            // If the method is a method from Object then defer to normal invocation.
            if (method.getDeclaringClass() == Object.class) {
              return method.invoke(this, args);
            }
            if (platform.isDefaultMethod(method)) {
              return platform.invokeDefaultMethod(method, service, proxy, args);
            }
            return loadMethodHandler(method).invoke(args);
          }
        });
  }

  private void eagerlyValidateMethods(Class<?> service) {
    Platform platform = Platform.get();
    for (Method method : service.getDeclaredMethods()) {
      if (!platform.isDefaultMethod(method)) {
        loadMethodHandler(method);
      }
    }
  }

  MethodHandler<?> loadMethodHandler(Method method) {
    MethodHandler<?> handler;
    synchronized (methodHandlerCache) {
      handler = methodHandlerCache.get(method);
      if (handler == null) {
        handler = MethodHandler.create(this, method);
        methodHandlerCache.put(method, handler);
      }
    }
    return handler;
  }

  public OkHttpClient client() {
    return client;
  }

  public BaseUrl baseUrl() {
    return baseUrl;
  }

  public List<CallAdapter.Factory> callAdapterFactories() {
    return Collections.unmodifiableList(adapterFactories);
  }

  /**
   * Returns the {@link CallAdapter} for {@code returnType} from the available {@linkplain
   * #callAdapterFactories() factories}.
   */
  public CallAdapter<?> callAdapter(Type returnType, Annotation[] annotations) {
    return nextCallAdapter(null, returnType, annotations);
  }

  /**
   * Returns the {@link CallAdapter} for {@code returnType} from the available {@linkplain
   * #callAdapterFactories() factories} except {@code skipPast}.
   */
  public CallAdapter<?> nextCallAdapter(CallAdapter.Factory skipPast, Type returnType,
      Annotation[] annotations) {
    checkNotNull(returnType, "returnType == null");
    checkNotNull(annotations, "annotations == null");

    int start = adapterFactories.indexOf(skipPast) + 1;
    for (int i = start, count = adapterFactories.size(); i < count; i++) {
      CallAdapter<?> adapter = adapterFactories.get(i).get(returnType, annotations, this);
      if (adapter != null) {
        return adapter;
      }
    }

    StringBuilder builder = new StringBuilder("Could not locate call adapter for ")
        .append(returnType)
        .append(". Tried:");
    for (int i = start, count = adapterFactories.size(); i < count; i++) {
      builder.append("\n * ").append(adapterFactories.get(i).getClass().getName());
    }
    if (skipPast != null) {
      builder.append("\nSkipped:");
      for (int i = 0; i < start; i++) {
        builder.append("\n * ").append(adapterFactories.get(i).getClass().getName());
      }
    }
    throw new IllegalArgumentException(builder.toString());
  }

  /**
   * TODO
   */
  public List<Converter.Factory> converterFactories() {
    return Collections.unmodifiableList(converterFactories);
  }

  /**
   * Returns a {@link Converter} for {@code type} to {@link RequestBody} from the available
   * {@linkplain #converterFactories() factories}.
   */
  public <T> Converter<T, RequestBody> requestConverter(Type type, Annotation[] annotations) {
    checkNotNull(type, "type == null");
    checkNotNull(annotations, "annotations == null");

    for (int i = 0, count = converterFactories.size(); i < count; i++) {
      Converter<?, RequestBody> converter =
          converterFactories.get(i).toRequestBody(type, annotations);
      if (converter != null) {
        //noinspection unchecked
        return (Converter<T, RequestBody>) converter;
      }
    }

    StringBuilder builder = new StringBuilder("Could not locate RequestBody converter for ")
        .append(type)
        .append(". Tried:");
    for (Converter.Factory converterFactory : converterFactories) {
      builder.append("\n * ").append(converterFactory.getClass().getName());
    }
    throw new IllegalArgumentException(builder.toString());
  }

  /**
   * Returns a {@link Converter} for {@link ResponseBody} to {@code type} from the available
   * {@linkplain #converterFactories() factories}.
   */
  public <T> Converter<ResponseBody, T> responseConverter(Type type, Annotation[] annotations) {
    checkNotNull(type, "type == null");
    checkNotNull(annotations, "annotations == null");

    for (int i = 0, count = converterFactories.size(); i < count; i++) {
      Converter<ResponseBody, ?> converter =
          converterFactories.get(i).fromResponseBody(type, annotations);
      if (converter != null) {
        //noinspection unchecked
        return (Converter<ResponseBody, T>) converter;
      }
    }

    StringBuilder builder = new StringBuilder("Could not locate ResponseBody converter for ")
        .append(type)
        .append(". Tried:");
    for (Converter.Factory converterFactory : converterFactories) {
      builder.append("\n * ").append(converterFactory.getClass().getName());
    }
    throw new IllegalArgumentException(builder.toString());
  }

  public Executor callbackExecutor() {
    return callbackExecutor;
  }

  /**
   * Build a new {@link Retrofit}.
   * <p>
   * Calling {@link #baseUrl} is required before calling {@link #build()}. All other methods
   * are optional.
   */
  public static final class Builder {
    private OkHttpClient client;
    private BaseUrl baseUrl;
    private List<Converter.Factory> converterFactories = new ArrayList<>();
    private List<CallAdapter.Factory> adapterFactories = new ArrayList<>();
    private Executor callbackExecutor;
    private boolean validateEagerly;

    public Builder() {
      // Add the built-in converter factory first. This prevents overriding its behavior but also
      // ensures correct behavior when using converters that consume all types.
      converterFactories.add(new BuiltInConverters());
    }

    /** The HTTP client used for requests. */
    public Builder client(OkHttpClient client) {
      this.client = checkNotNull(client, "client == null");
      return this;
    }

    /** API base URL. */
    public Builder baseUrl(String baseUrl) {
      checkNotNull(baseUrl, "baseUrl == null");
      HttpUrl httpUrl = HttpUrl.parse(baseUrl);
      if (httpUrl == null) {
        throw new IllegalArgumentException("Illegal URL: " + baseUrl);
      }
      return baseUrl(httpUrl);
    }

    /** API base URL. */
    public Builder baseUrl(final HttpUrl baseUrl) {
      checkNotNull(baseUrl, "baseUrl == null");
      return baseUrl(new BaseUrl() {
        @Override public HttpUrl url() {
          return baseUrl;
        }
      });
    }

    /** API base URL. */
    public Builder baseUrl(BaseUrl baseUrl) {
      this.baseUrl = checkNotNull(baseUrl, "baseUrl == null");
      return this;
    }

    /** Add converter factory for serialization and deserialization of objects. */
    public Builder addConverterFactory(Converter.Factory converterFactory) {
      converterFactories.add(checkNotNull(converterFactory, "converterFactory == null"));
      return this;
    }

    /**
     * TODO
     */
    public Builder addCallAdapterFactory(CallAdapter.Factory factory) {
      adapterFactories.add(checkNotNull(factory, "factory == null"));
      return this;
    }

    /**
     * The executor on which {@link Callback} methods are invoked when returning {@link Call} from
     * your service method.
     */
    public Builder callbackExecutor(Executor callbackExecutor) {
      this.callbackExecutor = checkNotNull(callbackExecutor, "callbackExecutor == null");
      return this;
    }

    /**
     * When calling {@link #create} on the resulting {@link Retrofit} instance, eagerly validate
     * the configuration of all methods in the supplied interface.
     */
    public Builder validateEagerly() {
      validateEagerly = true;
      return this;
    }

    /** Create the {@link Retrofit} instances. */
    public Retrofit build() {
      if (baseUrl == null) {
        throw new IllegalStateException("Base URL required.");
      }

      OkHttpClient client = this.client;
      if (client == null) {
        client = new OkHttpClient();
      }

      // Make a defensive copy of the adapters and add the default Call adapter.
      List<CallAdapter.Factory> adapterFactories = new ArrayList<>(this.adapterFactories);
      adapterFactories.add(Platform.get().defaultCallAdapterFactory(callbackExecutor));

      // Make a defensive copy of the converters.
      List<Converter.Factory> converterFactories = new ArrayList<>(this.converterFactories);

      return new Retrofit(client, baseUrl, converterFactories, adapterFactories, callbackExecutor,
          validateEagerly);
    }
  }
}
