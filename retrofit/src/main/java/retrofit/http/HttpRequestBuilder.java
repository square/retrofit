package retrofit.http;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Named;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import retrofit.io.TypedBytes;

import static retrofit.http.RestAdapter.MethodDetails;

/**
 * Builds HTTP requests from Java method invocations.  Handles "path parameters" in the
 * {@code apiUrl} in the form of "path/to/url/{id}/action" where a parameter annotated with
 * {@code @Named("id")} is inserted into the url.  Note that this replacement can be recursive if:
 * <ol>
 * <li>Multiple sets of brackets are nested ("path/to/{{key}a}.</li>
 * <li>The order of {@link Named @Named} values go from innermost to outermost.</li>
 * <li>The values replaced correspond to {@link Named @Named} parameters.</li>
 * </ol>
 */
final class HttpRequestBuilder {
  private final Converter converter;

  private MethodDetails methodDetails;
  private Object[] args;
  private String apiUrl;
  private String replacedRelativePath;
  private Headers headers;
  private List<NameValuePair> nonPathParams;
  private TypedBytes singleEntity;

  HttpRequestBuilder(Converter converter) {
    this.converter = converter;
  }

  HttpRequestBuilder setMethod(MethodDetails methodDetails) {
    this.methodDetails = methodDetails;
    return this;
  }

  Method getMethod() {
    return methodDetails.method;
  }

  boolean isSynchronous() {
    return methodDetails.isSynchronous;
  }

  String getRelativePath() {
    return replacedRelativePath != null ? replacedRelativePath : methodDetails.path;
  }

  HttpRequestBuilder setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
    return this;
  }

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
   * {@code includePathParams} is {@code true}, path parameters (like 'id' in "/entity/{id}") will
   * be included in this list.
   */
  List<NameValuePair> getParamList(boolean includePathParams) {
    if (includePathParams || nonPathParams == null) return createParamList();
    return nonPathParams;
  }

  /** Converts all but the last method argument to a list of HTTP request parameters. */
  private List<NameValuePair> createParamList() {
    List<NameValuePair> params = new ArrayList<NameValuePair>();

    // Add query parameter(s), if specified.
    for (QueryParam annotation : methodDetails.pathQueryParams) {
      params.add(new BasicNameValuePair(annotation.name(), annotation.value()));
    }

    // Add arguments as parameters.
    String[] pathNamedParams = methodDetails.pathNamedParams;
    int singleEntityArgumentIndex = methodDetails.singleEntityArgumentIndex;
    for (int i = 0; i < pathNamedParams.length; i++) {
      Object arg = args[i];
      if (arg == null) continue;
      if (i != singleEntityArgumentIndex) {
        params.add(new BasicNameValuePair(pathNamedParams[i], String.valueOf(arg)));
      } else {
        if (arg instanceof TypedBytes) {
          // Let the object specify its own entity representation.
          singleEntity = (TypedBytes) arg;
        } else {
          // Just an object: serialize it with supplied converter.
          singleEntity = converter.from(arg);
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

  HttpUriRequest build() throws URISyntaxException {
    // Alter parameter list if path parameters are present.
    Set<String> pathParams = new LinkedHashSet<String>(methodDetails.pathParams);
    List<NameValuePair> paramList = createParamList();
    if (!pathParams.isEmpty()) {
      String replacedPath = methodDetails.path;

      for (String pathParam : pathParams) {
        NameValuePair found = null;
        for (NameValuePair param : paramList) {
          if (param.getName().equals(pathParam)) {
            found = param;
          }
        }
        if (found != null) {
          String value;
          try {
            value = URLEncoder.encode(found.getValue(), "UTF-8");
          } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
          }
          replacedPath = doReplace(replacedPath, found.getName(), value);
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

    return methodDetails.httpMethod.createFrom(this);
  }

  private String doReplace(String replacedPath, String paramName, String newVal) {
    replacedPath = replacedPath.replaceAll("\\{" + paramName + "\\}", newVal);
    return replacedPath;
  }

  /** Gets the parameter name from the {@link Named} annotation. */
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
}