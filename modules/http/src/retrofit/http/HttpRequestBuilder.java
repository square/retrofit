package retrofit.http;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import retrofit.internal.gson.Gson;
import retrofit.io.MimeType;
import retrofit.io.TypedBytes;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds HTTP requests from Java method invocations.  Handles "path parameters"
 * in the apiUrl in the form of "path/to/url/{id}/action" where a parameter
 * &#64;{@link Named}("id") is inserted into the url.  Note that this
 * replacement can be recursive if:
 * <ol>
 * <li> multiple sets of brackets are nested ("path/to/{{key}a}
 * <li> the order of &#64;{@link Named} values go from innermost to outermost
 * <li> the values replaced correspond to &#64;{@link Named} parameters.
 * </ol>
 */
final class HttpRequestBuilder {
  private static final Logger logger = Logger.getLogger(HttpRequestBuilder.class.getName());

  private final Gson gson;

  private Method javaMethod;
  private Object[] args;
  private String apiUrl;
  private String replacedRelativePath;
  private Headers headers;
  private List<NameValuePair> nonPathParams;
  private RequestLine requestLine;
  private TypedBytes singleEntity;

  HttpRequestBuilder(Gson gson) {
    this.gson = gson;
  }

  HttpRequestBuilder setMethod(Method method) {
    this.javaMethod = method;
    requestLine = RequestLine.fromMethod(method);
    return this;
  }

  Method getMethod() {
    return javaMethod;
  }

  String getRelativePath() {
    return replacedRelativePath != null ? replacedRelativePath : requestLine.getRelativePath();
  }

  HttpRequestBuilder setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
    return this;
  }

  /** The last argument is assumed to be the Callback and is ignored. */
  HttpRequestBuilder setArgs(Object[] args) {
    this.args = args;
    return this;
  }

  Object[] getArgs() {
    return args;
  }

  HttpRequestBuilder setHeaders(Headers headers) {
    this.headers = headers;
    return this;
  }

  Headers getHeaders() {
    return headers;
  }

  String getScheme() {
    return apiUrl.substring(0, apiUrl.indexOf("://"));
  }

  String getHost() {
    String host = apiUrl.substring(apiUrl.indexOf("://") + 3, apiUrl.length());
    if (host.endsWith("/")) host = host.substring(0, host.length() - 1);
    return host;
  }

  /**
   * Converts all but the last method argument to a list of HTTP request parameters.  If
   * includePathParams is true, path parameters (like id in "/entity/{id}" will be included in this
   * list.
   */
  List<NameValuePair> getParamList(boolean includePathParams) {
    if (includePathParams || nonPathParams == null) return createParamList();
    return nonPathParams;
  }

  /**
   * Converts all but the last method argument to a list of HTTP request parameters.
   */
  private List<NameValuePair> createParamList() {
    Annotation[][] parameterAnnotations = javaMethod.getParameterAnnotations();
    int count = parameterAnnotations.length - 1;

    List<NameValuePair> params = new ArrayList<NameValuePair>(count);

    // Add query parameter(s), if specified.
    QueryParams queryParams = javaMethod.getAnnotation(QueryParams.class);
    if (queryParams != null) {
      QueryParam[] annotations = queryParams.value();
      for (QueryParam annotation : annotations) {
        params.add(addPair(annotation));
      }
    }

    // Also check for a single specified query parameter.
    QueryParam queryParam = javaMethod.getAnnotation(QueryParam.class);
    if (queryParam != null) {
      params.add(addPair(queryParam));
    }

    // Add arguments as parameters.
    for (int i = 0; i < count; i++) {
      Object arg = args[i];
      if (arg == null) continue;
      for (Annotation annotation : parameterAnnotations[i]) {
        final Class<? extends Annotation> type = annotation.annotationType();
        if (type == Named.class) {
          String name = getName(parameterAnnotations[i], javaMethod, i);
          if (arg.getClass().isArray()) {
            Object[] array = (Object[]) arg;
            for (Object anArray : array) {
              params.add(new BasicNameValuePair(name, String.valueOf(anArray)));
            }
          } else {
            params.add(new BasicNameValuePair(name, String.valueOf(arg)));
          }
        } else if (type == SingleEntity.class) {
          if (arg instanceof TypedBytes) { // Let the object specify its own entity representation.
            singleEntity = (TypedBytes) arg;
          } else { // Just an object: serialize it with json
            singleEntity = new JsonTypedBytes(arg);
          }
        }
      }
    }

    return params;
  }

  public TypedBytes getSingleEntity() {
    return singleEntity;
  }

  /**
   * If this builder has a custom mime-type for the request, this returns it.
   *
   * @return "Content-Type" string if present, null otherwise.
   */
  public String getMimeType() {
    return singleEntity == null ? null : singleEntity.mimeType().mimeName();
  }

  private BasicNameValuePair addPair(QueryParam queryParam) {
    return new BasicNameValuePair(queryParam.name(), queryParam.value());
  }

  HttpUriRequest build() throws URISyntaxException {
    // Alter parameter list if path parameters are present.
    Set<String> pathParams = getPathParameters(requestLine.getRelativePath());
    List<NameValuePair> paramList = createParamList();
    if (!pathParams.isEmpty()) {
      String replacedPath = requestLine.getRelativePath();

      for (String pathParam : pathParams) {
        NameValuePair found = null;
        for (NameValuePair param : paramList) {
          if (param.getName().equals(pathParam)) {
            found = param;
          }
        }
        if (found != null) {
          replacedPath = doReplace(replacedPath, found.getName(), found.getValue());
          paramList.remove(found);
        } else {
          throw new IllegalArgumentException(
              "Got pathParam " + pathParam + " that wasn't specified with @Named param.");
        }
      }
      replacedRelativePath = replacedPath;

      nonPathParams = paramList;
    }

    if (getSingleEntity() != null) {
      // We're passing a JSON object as the main entity: paramList should only contain path
      // parameter values.
      if (!paramList.isEmpty()) {
        throw new IllegalArgumentException("Found @Named param on single-entity request that "
            + "wasn't used for path substitution: this shouldn't be on the method.");
      }
    }

    HttpUriRequest request = requestLine.getHttpMethod().createFrom(this);
    return request;
  }

  private String doReplace(String replacedPath, String paramName, String newVal) {
    replacedPath = replacedPath.replaceAll("\\{" + paramName + "\\}", newVal);
    return replacedPath;
  }

  /**
   * Gets the set of unique path params used in the given uri.  If a param is used twice in the uri,
   * it will only show up once in the set.
   *
   * @param path the path to search through.
   * @return set of path params.
   */
  static Set<String> getPathParameters(String path) {
    Pattern p = Pattern.compile("\\{([a-z_-]*)\\}");
    Matcher m = p.matcher(path);
    Set<String> patterns = new HashSet<String>();
    while (m.find()) {
      patterns.add(m.group(1));
    }
    return patterns;
  }

  /** Gets the parameter name from the @Named annotation. */
  static String getName(Annotation[] annotations, Method method, int parameterIndex) {
    return findAnnotation(annotations, Named.class, method, parameterIndex).value();
  }

  /**
   * Finds a parameter annotation.
   *
   * @throws IllegalArgumentException if the annotation isn't found
   */
  private static <A extends Annotation> A findAnnotation(Annotation[] annotations,
      Class<A> annotationType, Method method, int parameterIndex) {
    for (Annotation annotation : annotations) {
      if (annotation.annotationType() == annotationType) {
        return annotationType.cast(annotation);
      }
    }
    throw new IllegalArgumentException(
        annotationType + " missing on" + " parameter #" + parameterIndex + " of " + method + ".");
  }

  private class JsonTypedBytes implements TypedBytes {
    private Object obj;

    public JsonTypedBytes(Object obj) {
      this.obj = obj;
    }

    @Override public MimeType mimeType() {
      return MimeType.JSON;
    }

    @Override public int length() {
      return toJson().length();
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      final String str = toJson();
      // TODO use requested encoding?
      out.write(str.getBytes("UTF-8"));
    }

    protected String toJson() {
      return gson.toJson(obj);
    }
  }
}