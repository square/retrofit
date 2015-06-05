package retrofit2;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.Url;


/**
 * Adapts a Java interface to a REST API. <p> API endpoints are defined as methods on an interface
 * with annotations providing metadata about the form in which the HTTP call should be made. <p> The
 * relative path for a given method is obtained from an annotation on the method describing the
 * request type. The built-in methods are {@link retrofit2.http.GET GET}, {@link retrofit2.http.PUT
 * PUT}, {@link retrofit2.http.POST POST}, {@link retrofit2.http.PATCH PATCH}, {@link
 * retrofit2.http.HEAD HEAD}, and {@link retrofit2.http.DELETE DELETE}. You can use a custom HTTP
 * method with {@link HTTP @HTTP}. <p> Method parameters can be used to replace parts of the URL by
 * annotating them with {@link retrofit2.http.Path @Path}. Replacement sections are denoted by an
 * identifier surrounded by curly braces (e.g., "{foo}"). To add items to the query string of a URL
 * use {@link retrofit2.http.Query @Query}. <p> The body of a request is denoted by the {@link
 * retrofit2.http.Body @Body} annotation. The object will be converted to request representation by
 * a call to {@link Converter#convert(Object) toBody} on the supplied {@link Converter} for this
 * instance. A {@link RequestBody} can also be used which will not use the {@code Converter}. <p>
 * Alternative request body formats are supported by method annotations and corresponding parameter
 * annotations: <ul> <li>{@link retrofit2.http.FormUrlEncoded @FormUrlEncoded} - Form-encoded data
 * with key-value pairs specified by the {@link retrofit2.http.Field @Field} parameter annotation.
 * <li>{@link retrofit2.http.Multipart @Multipart} - RFC 2387-compliant multi-part data with parts
 * specified by the {@link retrofit2.http.Part @Part} parameter annotation. </ul> <p> Additional
 * static headers can be added for an endpoint using the {@link retrofit2.http.Headers @Headers}
 * method annotation. For per-request control over a header annotate a parameter with {@link Header
 * @Header}. <p> By default, methods return a {@link Call} which represents the HTTP request. The
 * generic parameter of the call is the response body type and will be converted by a call to {@link
 * Converter#convert(Object)} on the supplied {@link Converter} for this instance. {@link
 * ResponseBody} can also be used which will not use the {@code Converter}. <p> For example:
 * <pre>
 * public interface CategoryService {
 *   &#64;POST("/category/{cat}")
 *   Call&lt;List&lt;Item&gt;&gt; categoryList(@Path("cat") String a, @Query("page") int b);
 * }
 * </pre>
 * <p> Calling {@link #create(Class) create()} with {@code CategoryService.class} will validate the
 * annotations and create a new implementation of the service definition.
 *
 * @author Bob Lee (bob@squareup.com)
 * @author Jake Wharton (jw@squareup.com)
 */
public final class RestAdapter {
  private final Map<Method, ServiceMethod> serviceMethodCache = new LinkedHashMap<>();
  private final Retrofit retrofit;
  private final boolean validateEagerly;

  public RestAdapter(Retrofit retrofit, boolean validateEagerly) {
    this.retrofit = retrofit;
    this.validateEagerly = validateEagerly;
  }

  /**
   * Create an implementation of the API endpoints defined by the {@code service} interface. <p> The
   * relative path for a given method is obtained from an annotation on the method describing the
   * request type. The built-in methods are {@link retrofit2.http.GET GET}, {@link
   * retrofit2.http.PUT PUT}, {@link retrofit2.http.POST POST}, {@link retrofit2.http.PATCH PATCH},
   * {@link retrofit2.http.HEAD HEAD}, {@link retrofit2.http.DELETE DELETE} and {@link
   * retrofit2.http.OPTIONS OPTIONS}. You can use a custom HTTP method with {@link HTTP @HTTP}. For
   * a dynamic URL, omit the path on the annotation and annotate the first parameter with {@link Url
   * @Url}. <p> Method parameters can be used to replace parts of the URL by annotating them with
   * {@link retrofit2.http.Path @Path}. Replacement sections are denoted by an identifier surrounded
   * by curly braces (e.g., "{foo}"). To add items to the query string of a URL use {@link
   * retrofit2.http.Query @Query}. <p> The body of a request is denoted by the {@link
   * retrofit2.http.Body @Body} annotation. The object will be converted to request representation
   * by one of the {@link Converter.Factory} instances. A {@link RequestBody} can also be used for a
   * raw representation. <p> Alternative request body formats are supported by method annotations
   * and corresponding parameter annotations: <ul> <li>{@link retrofit2.http.FormUrlEncoded
   * @FormUrlEncoded} - Form-encoded data with key-value pairs specified by the {@link
   * retrofit2.http.Field @Field} parameter annotation. <li>{@link retrofit2.http.Multipart
   * @Multipart} - RFC 2388-compliant multipart data with parts specified by the {@link
   * retrofit2.http.Part @Part} parameter annotation. </ul> <p> Additional static headers can be
   * added for an endpoint using the {@link retrofit2.http.Headers @Headers} method annotation. For
   * per-request control over a header annotate a parameter with {@link Header @Header}. <p> By
   * default, methods return a {@link Call} which represents the HTTP request. The generic parameter
   * of the call is the response body type and will be converted by one of the {@link
   * Converter.Factory} instances. {@link ResponseBody} can also be used for a raw representation.
   * {@link Void} can be used if you do not care about the body contents. <p> For example:
   * <pre>
   * public interface CategoryService {
   *   &#64;POST("category/{cat}/")
   *   Call&lt;List&lt;Item&gt;&gt; categoryList(@Path("cat") String a, @Query("page") int b);
   * }
   * </pre>
   */
  @SuppressWarnings("unchecked") // Single-interface proxy creation guarded by parameter safety.
  public <T> T create(final Class<T> service) {
    Utils.validateServiceInterface(service);
    if (validateEagerly) {
      eagerlyValidateMethods(service);
    }
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service}, new
        InvocationHandler() {
      private final Platform platform = Platform.get();

      @Override public Object invoke(Object proxy, Method method, Object... args) throws Throwable {
        // If the method is a method from Object then defer to normal invocation.
        if (method.getDeclaringClass() == Object.class) {
          return method.invoke(this, args);
        }
        if (platform.isDefaultMethod(method)) {
          return platform.invokeDefaultMethod(method, service, proxy, args);
        }
        ServiceMethod serviceMethod = loadServiceMethod(method);
        DefaultCallFactory callFactory = new DefaultCallFactory(serviceMethod);
        OkHttpCall okHttpCall = new OkHttpCall<>(callFactory, args);
        return serviceMethod.callAdapter.adapt(okHttpCall);
      }
    });
  }

  private void eagerlyValidateMethods(Class<?> service) {
    Platform platform = Platform.get();
    for (Method method : service.getDeclaredMethods()) {
      if (!platform.isDefaultMethod(method)) {
        loadServiceMethod(method);
      }
    }
  }

  ServiceMethod loadServiceMethod(Method method) {
    ServiceMethod result;
    synchronized (serviceMethodCache) {
      result = serviceMethodCache.get(method);
      if (result == null) {
        result = new ServiceMethod.Builder(retrofit, method).build();
        serviceMethodCache.put(method, result);
      }
    }
    return result;
  }
}
