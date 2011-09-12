package retrofit.http;

import com.google.inject.name.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;

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
  private static final Logger logger =
      Logger.getLogger(HttpRequestBuilder.class.getName());

  private Method javaMethod;
  private Object[] args;
  private String apiUrl;
  private String replacedRelativePath;
  private Headers headers;
  private List<NameValuePair> nonPathParams;
  private RequestLine requestLine;

  HttpRequestBuilder setMethod(Method method) {
    this.javaMethod = method;
    requestLine = RequestLine.fromMethod(method);
    return this;
  }

  Method getMethod() {
    return javaMethod;
  }

  String getRelativePath() {
    return replacedRelativePath != null ? replacedRelativePath
        : requestLine.getRelativePath();
  }

  private boolean hasPathParameters() {
    return requestLine.getRelativePath().contains("{");
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
    String host = apiUrl.substring(
        apiUrl.indexOf("://") + 3, apiUrl.length());
    if (host.endsWith("/")) host = host.substring(0, host.length() - 1);
    return host;
  }

  /**
   * Converts all but the last method argument to a list of HTTP request
   * parameters.  If includePathParams is true, path parameters (like id in
   * "/entity/{id}" will be included in this list.
   */
  List<NameValuePair> getParamList(boolean includePathParams) {
    if (includePathParams || nonPathParams == null) return createParamList();
    return nonPathParams;
  }

  /**
   * Converts all but the last method argument to a list of HTTP request
   * parameters.
   */
  private List<NameValuePair> createParamList() {
    Annotation[][] parameterAnnotations =
        javaMethod.getParameterAnnotations();
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
      String name = getName(parameterAnnotations[i], javaMethod, i);
      params.add(new BasicNameValuePair(name, String.valueOf(arg)));
    }

    return params;
  }

  private BasicNameValuePair addPair(QueryParam queryParam) {
    return new BasicNameValuePair(queryParam.name(), queryParam.value());
  }

  HttpUriRequest build() throws URISyntaxException {
    // Alter parameter list if path parameters are present.
    if (hasPathParameters()) {
      List<NameValuePair> paramList = createParamList();

      String replacedPath = requestLine.getRelativePath();
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

    HttpUriRequest request = requestLine.getHttpMethod().createFrom(this);
    if (logger.isLoggable(Level.FINE)) logger.fine("Request params: "+getParamList(true));
    return request;
  }

  /** Gets the parameter name from the @Named annotation. */
  static String getName(Annotation[] annotations, Method method,
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
}