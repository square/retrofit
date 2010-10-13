package retrofit.http;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.*;
import com.google.inject.name.Named;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import retrofit.core.Callback;
import retrofit.core.MainThread;
import retrofit.io.TypedBytes;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  /**
   * Adapts a Java interface to a REST API. HTTP requests happen in a
   * background thread. Callbacks happen in the UI thread.
   *
   * <p>Gets the relative path for a given method from a {@link Path}
   * annotation on the method. Gets the names of URL parameters from {@link
   * com.google.inject.name.Named} annotations on the method parameters.
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
   *     &#64;Path("go") public void go(@Named("a") String a, @Named("b") int b,
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
        String relativePath = getRelativePath(method);

        // Construct POST HTTP request.
        final HttpPost post = new HttpPost(server.apiUrl() + relativePath);
        headers.setHeaders(post);

        // Convert all but the last method argument to HTTP request parameters.
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        Class<?>[] parameterTypes = method.getParameterTypes();

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
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
          List<BasicNameValuePair> pairs
              = new ArrayList<BasicNameValuePair>(count);
          for (int i = 0; i < count; i++) {
            Object arg = args[i];
            if (arg == null) continue;
            String name = getName(parameterAnnotations[i], method, i);
            pairs.add(new BasicNameValuePair(name, String.valueOf(arg)));
          }
          try {
            post.setEntity(new UrlEncodedFormEntity(pairs));
          } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
          }
        }

        // The last parameter should be of type Callback<T>. Determine T.
        final Type resultType = getCallbackParameterType(method,
            genericParameterTypes);
        logger.fine(String.format("POSTing to %s.", post.getURI()));

        httpClientProvider.get().execute(post, GsonResponseHandler.create(
            resultType, callback));
      } catch (IOException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
        callback.networkError();
      } catch (Throwable t) {
        callback.unexpectedError(t);
      }
    }

    /** Returns true if the post contains a file upload. */
    private boolean useMultipart(Class<?>[] parameterTypes) {
      for (Class<?> parameterType : parameterTypes) {
        if (TypedBytes.class.isAssignableFrom(parameterType)) return true;
      }
      return false;
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

    /** Gets the parameter name from the @Named annotation. */
    private String getName(Annotation[] annotations, Method method,
        int parameterIndex) {
      return findAnnotation(annotations, Named.class, method,
          parameterIndex).value();
    }

    /**
     * Finds a parameter annotation.
     *
     * @throws IllegalArgumentException if the annotation isn't found
     */
    private <A extends Annotation> A findAnnotation(Annotation[] annotations,
        Class<A> annotationType, Method method, int parameterIndex) {
      for (Annotation annotation : annotations) {
        if (annotation.annotationType() == annotationType) {
          return annotationType.cast(annotation);
        }
      }
      throw new IllegalArgumentException(annotationType + " missing on"
          + " parameter #" + parameterIndex + " of " + method + ".");
    }


    /** Gets the relative path from the @Path annotation. */
    private String getRelativePath(Method method) {
      Path pathAnnotation = method.getAnnotation(Path.class);
      if (pathAnnotation == null) throw new IllegalArgumentException(method
          + " is missing an @Path annotation.");
      return pathAnnotation.value();
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
   * Callback#call}.
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
}
