package retrofit;

import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import retrofit.http.HTTP;
import retrofit.http.Header;


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
 * {@link Converter#toBody(Object, java.lang.reflect.Type) toBody}
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
 * {@link Converter#fromBody(ResponseBody, Type) fromBody} on the supplied {@link Converter} for
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
public final class RestAdapter {
  private final Map<Method, MethodHandler<?>> methodHandlerCache = new LinkedHashMap<>();
  private final Retrofit retrofit;
  private final boolean validateEagerly;

  public RestAdapter(Retrofit retrofit, boolean validateEagerly) {
    this.retrofit = retrofit;
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
        handler = MethodHandler.create(retrofit, method);
        methodHandlerCache.put(method, handler);
      }
    }
    return handler;
  }
}
