package retrofit.http;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import retrofit.core.Callback;
import retrofit.core.MainThread;
import retrofit.io.TypedBytes;

/**
 * Converts Java method calls to Rest calls.
 *
 * @author Bob Lee (bob@squareup.com)
 */
@Singleton public class RestAdapter {
  private static final Logger logger =
      Logger.getLogger(RestAdapter.class.getName());

  @Inject private Server server;
  @Inject private Provider<HttpClient> httpClientProvider;
  @Inject private Executor executor;
  @Inject private MainThread mainThread;
  @Inject private Headers headers;
  @Inject(optional = true) private HttpProfiler profiler;

  /**
   * Adapts a Java interface to a REST API. HTTP requests happen in a
   * background thread. Callbacks happen in the UI thread.
   *
   * <p>Gets the relative path for a given method from a {@link GET},
   * {@link POST}, {@link PUT}, or {@link DELETE} annotation on the method.
   * Gets the names of URL parameters from {@link com.google.inject.name.Named}
   * annotations on the method parameters.
   *
   * <p>The last method parameter should be of type {@link Callback}. The
   * JSON HTTP response will be converted to the callback's parameter type
   * using GSON. If the callback parameter type uses a wildcard, the
   * lower bound will be used as the conversion type.
   *
   * <p>For example:
   *
   * <pre>
   *   public interface MyApi {
   *     &#64;POST("go") public void go(@Named("a") String a, @Named("b") int b,
   *         Callback&lt;? super MyResult> callback);
   *   }
   * </pre>
   *
   * @param type to implement
   */
  @SuppressWarnings("unchecked")
  public static <T> Module service(final Class<T> type) {
    return new Module() {
      @Override public void configure(Binder binder) {
        binder.bind(type).toProvider(new Provider<T>() {
          @Inject RestAdapter restAdapter;

          @Override public T get() {
            RestAdapter.RestHandler handler = restAdapter.new RestHandler();
            return (T) Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] { type }, handler);
          }
        });
      }
    };
  }

  private static final class RequestLine {
    private final String relativePath;
    private final HttpMethod httpMethod;

    private RequestLine(retrofit.http.HttpMethod.Type methodType,
        Annotation methodAnnotation) {
      relativePath = getValue(methodAnnotation);
      httpMethod = fromType(methodType);
    }

    private static HttpMethod fromType(
        retrofit.http.HttpMethod.Type methodType)
        throws AssertionError {
      switch (methodType) {
        case DELETE: return HttpMethod.DELETE;
        case GET:    return HttpMethod.GET;
        case POST:   return HttpMethod.POST;
        case PUT:    return HttpMethod.PUT;
        default:
          throw new AssertionError("This line should be unreachable. Has a" +
          		" new kind of http method annotation been added?");
      }
    }

    private static String getValue(Annotation methodAnnotation) {
      try {
        final Method valueMethod = methodAnnotation.annotationType()
            .getMethod("value");
        return (String) valueMethod.invoke(methodAnnotation);

      } catch (Exception ex) {
        throw new IllegalStateException("Failed to extract method path", ex);
      }
    }

    public String getRelativePath() {
      return relativePath;
    }
    public HttpMethod getHttpMethod() {
      return httpMethod;
    }
  }

  /**
   * Looks for exactly one annotation of type {@link DELETE}, {@link GET},
   * {@link POST}, or {@link PUT} and extracts its path data.  Throws an
   * {@link IllegalStateException} if none or multiple are found.
   */
  private static RequestLine readHttpMethodAnnotation(Method method) {
    Annotation[] annotations = method.getAnnotations();
    RequestLine found = null;
    for (Annotation annotation : annotations) {
      // look for an HttpMethod annotation describing the type:
      final retrofit.http.HttpMethod typeAnnotation = annotation.annotationType()
          .getAnnotation(retrofit.http.HttpMethod.class);
      if (typeAnnotation != null) {
        if (found != null) {
          throw new IllegalStateException(
              "Method annotated with multiple HTTP method annotations: "
                + method.toString());
        }
        found = new RequestLine(typeAnnotation.value(), annotation);
      }
    }

    if (found == null) {
      throw new IllegalStateException(
          "Method not annotated with GET, POST, PUT, or DELETE: "
            + method.toString());
    }
    return found;
  }

  /** Gets the parameter name from the @Named annotation. */
  private static String getName(Annotation[] annotations, Method method,
      int parameterIndex) {
    return findAnnotation(annotations, Named.class, method,
        parameterIndex).value();
  }

  /**
   * Finds a parameter annotation.
   *
   * @throws IllegalArgumentException if the annotation isn't found
   */
  private static <A extends Annotation> A findAnnotation(
      Annotation[] annotations, Class<A> annotationType, Method method,
      int parameterIndex) {
    for (Annotation annotation : annotations) {
      if (annotation.annotationType() == annotationType) {
        return annotationType.cast(annotation);
      }
    }
    throw new IllegalArgumentException(annotationType + " missing on"
        + " parameter #" + parameterIndex + " of " + method + ".");
  }

  private static enum HttpMethod {

    GET {
      @Override HttpUriRequest createFrom(HttpRequestBuilder builder)
          throws URISyntaxException {
        List<NameValuePair> queryParams = builder.getParamList(false);
        String queryString = URLEncodedUtils.format(queryParams, "UTF-8");
        URI uri = URIUtils.createURI(builder.getScheme(), builder.getHost(), -1,
            builder.getRelativePath(), queryString, null);
        HttpGet request = new HttpGet(uri);
        builder.getHeaders().setOn(request);
        return request;
      }
    },

    POST {
      @Override HttpUriRequest createFrom(HttpRequestBuilder builder)
          throws URISyntaxException {
        URI uri = URIUtils.createURI(builder.getScheme(), builder.getHost(), -1,
            builder.getRelativePath(), null, null);
        HttpPost request = new HttpPost(uri);
        addParams(request, builder);
        builder.getHeaders().setOn(request);
        return request;
      }
    },

    PUT {
      @Override HttpUriRequest createFrom(HttpRequestBuilder builder)
          throws URISyntaxException {
        URI uri = URIUtils.createURI(builder.getScheme(), builder.getHost(), -1,
            builder.getRelativePath(), null, null);
        HttpPut request = new HttpPut(uri);
        addParams(request, builder);
        builder.getHeaders().setOn(request);
        return request;
      }
    },

    DELETE {
      @Override HttpUriRequest createFrom(HttpRequestBuilder builder)
          throws URISyntaxException {
        List<NameValuePair> queryParams = builder.getParamList(false);
        String queryString = URLEncodedUtils.format(queryParams, "UTF-8");
        URI uri = URIUtils.createURI(builder.getScheme(), builder.getHost(), -1,
            builder.getRelativePath(), queryString, null);
        HttpDelete request = new HttpDelete(uri);
        builder.getHeaders().setOn(request);
        return request;
      }
    };

    /**
     * Create a request object from HttpRequestBuilder.
     */
    abstract HttpUriRequest createFrom(HttpRequestBuilder builder)
        throws URISyntaxException;

    /**
     * Adds all but the last method argument as parameters of HTTP request
     * object.
     */
    private static void addParams(HttpEntityEnclosingRequestBase request,
        HttpRequestBuilder builder) {
      Method method = builder.getMethod();
      Object[] args = builder.getArgs();
      Class<?>[] parameterTypes = method.getParameterTypes();

      Annotation[][] parameterAnnotations =
          method.getParameterAnnotations();
      int count = parameterAnnotations.length - 1;

      if (useMultipart(parameterTypes)) {
        MultipartEntity form = new MultipartEntity(
            HttpMultipartMode.BROWSER_COMPATIBLE);
        for (int i = 0; i < count; i++) {
          Object arg = args[i];
          if (arg == null) continue;
          Annotation[] annotations = parameterAnnotations[i];
          String name = getName(annotations, method, i);
          Class<?> type = parameterTypes[i];

          if (TypedBytes.class.isAssignableFrom(type)) {
            TypedBytes typedBytes = (TypedBytes) arg;
            form.addPart(name, new TypedBytesBody(typedBytes, name));
          } else {
            try {
              form.addPart(name, new StringBody(String.valueOf(arg)));
            } catch (UnsupportedEncodingException e) {
              throw new AssertionError(e);
            }
          }
        }
        request.setEntity(form);
      } else {
        try {
          List<NameValuePair> paramList = builder.getParamList(true);
          request.setEntity(new UrlEncodedFormEntity(paramList));
        } catch (UnsupportedEncodingException e) {
          throw new AssertionError(e);
        }
      }
    }

    /** Returns true if the parameters contain a file upload. */
    private static boolean useMultipart(Class<?>[] parameterTypes) {
      for (Class<?> parameterType : parameterTypes) {
        if (TypedBytes.class.isAssignableFrom(parameterType)) return true;
      }
      return false;
    }

  }

  /**
   * Builds HTTP requests from Java method invocations.
   */
  private static final class HttpRequestBuilder {

    private Method javaMethod;
    private Object[] args;
    private HttpMethod httpMethod;
    private String apiUrl;
    private String replacedRelativePath;
    private Headers headers;
    private String originalRelativePath;
    private List<NameValuePair> nonPathParams;

    public HttpRequestBuilder setMethod(Method method) {
      this.javaMethod = method;
      RequestLine requestLine = readHttpMethodAnnotation(method);
      this.originalRelativePath = requestLine.getRelativePath();
      this.httpMethod = requestLine.getHttpMethod();
      return this;
    }

    public Method getMethod() {
      return javaMethod;
    }

    public String getRelativePath() {
      return replacedRelativePath != null ? replacedRelativePath
          : originalRelativePath;
    }

    private boolean hasPathParameters() {
      return originalRelativePath.contains("{");
    }

    public HttpRequestBuilder setApiUrl(String apiUrl) {
      this.apiUrl = apiUrl;
      return this;
    }

    /** The last argument is assumed to be the Callback and is ignored. */
    public HttpRequestBuilder setArgs(Object[] args) {
      this.args = args;
      return this;
    }

    public Object[] getArgs() {
      return args;
    }

    public HttpRequestBuilder setHeaders(Headers headers) {
      this.headers = headers;
      return this;
    }

    public Headers getHeaders() {
      return headers;
    }

    public String getScheme() {
      return apiUrl.substring(0, apiUrl.indexOf("://"));
    }

    public String getHost() {
      String host = apiUrl.substring(
          apiUrl.indexOf("://") + 3, apiUrl.length());
      if (host.endsWith("/")) host = host.substring(0, host.length() - 1);
      return host;
    }

    /**
     * Converts all but the last method argument to a list of HTTP request
     * parameters.  If includePathParams is true, path parameters (like id in
     * "/entity/{id}"Êwill be included in this list.
     */
    public List<NameValuePair> getParamList(boolean includePathParams) {
      if (includePathParams || nonPathParams == null) return createParamListN();
      return nonPathParams;
    }

    /**
     * Converts all but the last method argument to a list of HTTP request
     * parameters.
     */
    private List<NameValuePair> createParamListN() {
      Annotation[][] parameterAnnotations =
          javaMethod.getParameterAnnotations();
      int count = parameterAnnotations.length - 1;

      List<NameValuePair> params = new ArrayList<NameValuePair>();
      for (int i = 0; i < count; i++) {
        Object arg = args[i];
        if (arg == null) continue;
        String name = getName(parameterAnnotations[i], javaMethod, i);
        params.add(new BasicNameValuePair(name, String.valueOf(arg)));
      }

      return params;
    }

    public HttpUriRequest build() throws URISyntaxException {
      // special handling if there are path parameters:
      if (hasPathParameters()) {
        List<NameValuePair> paramList = createParamListN();

        String replacedPath = originalRelativePath;
        Iterator<NameValuePair> itor = paramList.iterator();
        while (itor.hasNext()) {
          NameValuePair pair = itor.next();
          String paramName = pair.getName();
          if (replacedPath.contains("{" + paramName + "}")) {
            replacedPath = replacedPath.replaceAll(
                "\\{" + paramName + "\\}", pair.getValue());
            itor.remove();
          }
        }

        replacedRelativePath = replacedPath;
        nonPathParams = paramList;
      }

      return httpMethod.createFrom(this);
    }
  }

  private class RestHandler implements InvocationHandler {

    @Override public Object invoke(Object proxy, final Method method,
        final Object[] args) {
      // Execute HTTP request in the background.
      executor.execute(new Runnable() {
        @Override @SuppressWarnings("unchecked")
        public void run() {
          backgroundInvoke(method, args);
        }
      });

      // Methods should return void.
      return null;
    }

    private void backgroundInvoke(Method method, Object[] args) {
      UiCallback<?> callback = UiCallback.create(
          (Callback<?>) args[args.length - 1], mainThread);

      try {

        // Construct HTTP request.
        HttpUriRequest request = new HttpRequestBuilder()
            .setMethod(method)
            .setArgs(args)
            .setApiUrl(server.apiUrl())
            .setHeaders(headers)
            .build();

        // The last parameter should be of type Callback<T>. Determine T.
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        final Type resultType = getCallbackParameterType(method,
            genericParameterTypes);
        logger.fine("Sending " + request.getMethod() + " to " +
            request.getURI());

        final GsonResponseHandler<?> gsonResponseHandler =
            GsonResponseHandler.create(resultType, callback);

        // Optionally wrap the response handler for server call profiling.
        ResponseHandler<Void> rh = (profiler == null) ?
            gsonResponseHandler : createProfiler(gsonResponseHandler, profiler,
                method, server.apiUrl());

        httpClientProvider.get().execute(request, rh);
      } catch (IOException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
        callback.networkError();
      } catch (Throwable t) {
        t.printStackTrace();
        callback.unexpectedError(t);
      }
    }

    /**
     * Wraps a {@code GsonResponseHandler} with a
     * {@code ProfilingResponseHandler}.
     */
    private ProfilingResponseHandler createProfiler(
        ResponseHandler<Void> handlerToWrap, HttpProfiler profiler,
        Method method, String apiUrl) {
      RequestLine requestLine = readHttpMethodAnnotation(method);

      HttpProfiler.Method profilerMethod;
      HttpMethod httpMethod = requestLine.getHttpMethod();
      if (httpMethod == RestAdapter.HttpMethod.GET) {
        profilerMethod = HttpProfiler.Method.GET;
      } else if (httpMethod == HttpMethod.POST) {
        profilerMethod = HttpProfiler.Method.POST;
      } else {
        throw new IllegalStateException("Unrecognized method: " + httpMethod);
      }

      return new ProfilingResponseHandler(handlerToWrap, profiler,
          profilerMethod, apiUrl, requestLine.getRelativePath());
    }

    private static final String NOT_CALLBACK = "Last parameter of %s must be"
        + " of type Callback<X> or Callback<? super X>.";

    /** Gets the callback parameter type. */
    private Type getCallbackParameterType(Method method,
        Type[] parameterTypes) {
      Type lastType = parameterTypes[parameterTypes.length - 1];

      /*
       * Note: When more than one parameter is present, we seem to get
       * ParameterizedType Callback<? extends Object> instead of
       * WilcardType Callback<? super Foo> like we expected. File a bug.
       */
      if (lastType instanceof ParameterizedType) {
        ParameterizedType pType = (ParameterizedType) lastType;
        if (pType.getRawType() != Callback.class) {
          throw notCallback(method);
        }
        return pType.getActualTypeArguments()[0];
      }

      if (lastType instanceof WildcardType) {
        // Example: ? super SimpleResponse
        // Use the lower bound.
        WildcardType wildcardType = (WildcardType) lastType;
        Type[] lowerBounds = wildcardType.getLowerBounds();
        if (lowerBounds.length != 1) {
          throw notCallback(method);
        }
        return lowerBounds[0];
      }

      throw notCallback(method);
    }

    private IllegalArgumentException notCallback(Method method) {
      return new IllegalArgumentException(String.format(NOT_CALLBACK, method));
    }

  }

  /** Adapts ContentBody to TypedBytes. */
  private static class TypedBytesBody extends AbstractContentBody {
    private final TypedBytes typedBytes;
    private final String name;

    public TypedBytesBody(TypedBytes typedBytes, String baseName) {
      super(typedBytes.mimeType().mimeName());
      this.typedBytes = typedBytes;
      this.name = baseName + "." + typedBytes.mimeType().extension();
    }

    @Override public long getContentLength() {
      return typedBytes.length();
    }

    @Override public String getFilename() {
      return name;
    }

    @Override public String getCharset() {
      return null;
    }

    @Override public String getTransferEncoding() {
      return MIME.ENC_BINARY;
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      /*
       * Note: We probably want to differentiate I/O errors that occur
       * while reading a file from network errors. Network operations can
       * be retried. File operations will probably continue to fail.
       *
       * In the case of photo uploads, we at least check that the file
       * exists before we even try to upload it.
       */
      typedBytes.writeTo(out);
    }
  }

  /**
   * Sends server call times and response status codes to {@link HttpProfiler}.
   */
  private static class ProfilingResponseHandler
      implements ResponseHandler<Void> {
    private final ResponseHandler<Void> delegate;
    private final HttpProfiler profiler;
    private final String apiUrl;
    private final String relativePath;
    private final HttpProfiler.Method method;
    private final long startTime = System.currentTimeMillis();

    /**
     * Wraps the delegate response handler.
     */
    private ProfilingResponseHandler(ResponseHandler<Void> delegate,
                                     HttpProfiler profiler,
                                     HttpProfiler.Method method,
                                     String apiUrl,
                                     String relativePath) {
      this.delegate = delegate;
      this.profiler = profiler;
      this.method = method;
      this.apiUrl = apiUrl;
      this.relativePath = relativePath;
    }

    @Override public Void handleResponse(HttpResponse httpResponse) throws IOException {
      // Intercept the response and send data to profiler.
      long elapsedTime = System.currentTimeMillis() - startTime;
      int statusCode = httpResponse.getStatusLine().getStatusCode();
      profiler.called(method, apiUrl, relativePath, elapsedTime, statusCode);

      // Pass along the response to the normal handler.
      return delegate.handleResponse(httpResponse);
    }
  }
}