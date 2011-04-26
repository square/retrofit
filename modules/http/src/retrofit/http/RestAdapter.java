package retrofit.http;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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
   * <p>Gets the relative path for a given method from a {@link GET} or
   * {@link POST} annotation on the method. Gets the names of URL parameters
   * from {@link com.google.inject.name.Named} annotations on the method
   * parameters.
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
      public void configure(Binder binder) {
        binder.bind(type).toProvider(new Provider<T>() {
          @Inject RestAdapter restAdapter;

          public T get() {
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

    public RequestLine(String relativePath, HttpMethod method) {
      this.relativePath = relativePath;
      this.httpMethod = method;
    }
    public String getRelativePath() {
      return relativePath;
    }
    public HttpMethod getHttpMethod() {
      return httpMethod;
    }
  }

  private static RequestLine readHttpMethodAnnotation(Method method) {
    GET getAnnotation = method.getAnnotation(GET.class);
    boolean hasGet = getAnnotation != null;

    POST postAnnotation = method.getAnnotation(POST.class);
    boolean hasPost = postAnnotation != null;

    if (hasGet && hasPost) {
      throw new IllegalArgumentException(
          "Method annotated with both GET and POST: " + method.getName());
    }

    if (hasGet) {
      return new RequestLine(getAnnotation.value(), HttpMethod.GET);
    } else if (hasPost) {
      return new RequestLine(postAnnotation.value(), HttpMethod.POST);
    } else {
      throw new IllegalArgumentException(
          "Method not annotated with GET or POST: " + method.getName());
    }
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
      HttpUriRequest createFrom(HttpRequestBuilder builder)
          throws URISyntaxException {
        List<NameValuePair> queryParams = builder.createParamList();
        String queryString = URLEncodedUtils.format(queryParams, "UTF-8");
        URI uri = URIUtils.createURI(builder.getScheme(), builder.getHost(), -1,
            builder.getRelativePath(), queryString, null);
        HttpGet httpGet = new HttpGet(uri);
        builder.getHeaders().setOn(httpGet);
        return httpGet;
      }
    },

    POST {
      HttpUriRequest createFrom(HttpRequestBuilder builder)
          throws URISyntaxException {
        URI uri = URIUtils.createURI(builder.getScheme(), builder.getHost(), -1,
            builder.getRelativePath(), null, null);
        HttpPost post = new HttpPost(uri);
        addParamsToPost(post, builder);
        builder.getHeaders().setOn(post);
        return post;
      }

      /**
       * Adds all but the last method argument as parameters of HTTP post
       * object.
       */
      private void addParamsToPost(HttpPost post, HttpRequestBuilder builder) {
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
          post.setEntity(form);
        } else {
          try {
            List<NameValuePair> paramList = builder.createParamList();
            post.setEntity(new UrlEncodedFormEntity(paramList));
          } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
          }
        }
      }

      /** Returns true if the post contains a file upload. */
      private boolean useMultipart(Class<?>[] parameterTypes) {
        for (Class<?> parameterType : parameterTypes) {
          if (TypedBytes.class.isAssignableFrom(parameterType)) return true;
        }
        return false;
      }
    };

    /**
     * Create a request object from HttpRequestBuilder.
     */
    abstract HttpUriRequest createFrom(HttpRequestBuilder builder)
        throws URISyntaxException;
  }

  /**
   * Builds HTTP requests from Java method invocations.
   */
  private static final class HttpRequestBuilder {

    private Method javaMethod;
    private Object[] args;
    private HttpMethod httpMethod;
    private String apiUrl;
    private String relativePath;
    private Headers headers;

    public HttpRequestBuilder setMethod(Method method) {
      this.javaMethod = method;
      RequestLine requestLine = readHttpMethodAnnotation(method);
      this.relativePath = requestLine.getRelativePath();
      this.httpMethod = requestLine.getHttpMethod();
      return this;
    }

    public Method getMethod() {
      return javaMethod;
    }

    public String getRelativePath() {
      return relativePath;
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
     * parameters.
     */
    public List<NameValuePair> createParamList() {
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
      return httpMethod.createFrom(this);
    }
  }

  private class RestHandler implements InvocationHandler {

    public Object invoke(Object proxy, final Method method,
        final Object[] args) {
      // Execute HTTP request in the background.
      executor.execute(new Runnable() {
        @SuppressWarnings("unchecked")
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
        ResponseHandler<? extends Void> rh = (profiler == null) ?
            gsonResponseHandler : createProfiler(gsonResponseHandler, profiler,
                method, server.apiUrl());

        httpClientProvider.get().execute(request, rh);
      } catch (IOException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
        callback.networkError();
      } catch (Throwable t) {
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

    public long getContentLength() {
      return typedBytes.length();
    }

    public String getFilename() {
      return name;
    }

    public String getCharset() {
      return null;
    }

    public String getTransferEncoding() {
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
   * Converts JSON response to an object using Gson and then passes it to {@link
   * Callback#call(T)}.
   */
  static class GsonResponseHandler<T> extends CallbackResponseHandler<T> {

    private final Type type;

    GsonResponseHandler(Type type, UiCallback<T> callback) {
      super(callback);
      this.type = type;
    }

    static <T> GsonResponseHandler<T> create(Type type,
        UiCallback<T> callback) {
      return new GsonResponseHandler<T>(type, callback);
    }

    @Override protected T parse(HttpEntity entity) throws IOException,
        ServerException {
      try {
        if (logger.isLoggable(Level.FINE)) {
          entity = HttpClients.copyAndLog(entity);
        }

        // TODO: Use specified encoding.
        InputStreamReader in = new InputStreamReader(entity.getContent(),
            "UTF-8");

        /*
         * It technically isn't safe for fromJson() to return T here.
         * We derived type from Callback<T>, so we know we're safe.
         */
        @SuppressWarnings("unchecked")
        T t = (T) new Gson().fromJson(in, type);
        return t;
      } catch (JsonParseException e) {
        // The server returned us bad JSON!
        throw new ServerException(e);
      }
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

    public Void handleResponse(HttpResponse httpResponse) throws IOException {
      // Intercept the response and send data to profiler.
      long elapsedTime = System.currentTimeMillis() - startTime;
      int statusCode = httpResponse.getStatusLine().getStatusCode();
      profiler.called(method, apiUrl, relativePath, elapsedTime, statusCode);

      // Pass along the response to the normal handler.
      return delegate.handleResponse(httpResponse);
    }
  }
}