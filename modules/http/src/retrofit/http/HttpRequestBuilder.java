package retrofit.http;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;

/**
 * Builds HTTP requests from Java method invocations.
 */
final class HttpRequestBuilder {

  private Method javaMethod;
  private Object[] args;
  private HttpMethodType httpMethod;
  private String apiUrl;
  private String replacedRelativePath;
  private Headers headers;
  private String originalRelativePath;
  private List<NameValuePair> nonPathParams;

  public HttpRequestBuilder setMethod(Method method) {
    this.javaMethod = method;
    RequestLine requestLine = RequestLine.fromMethod(method);
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
   * "/entity/{id}" will be included in this list.
   */
  public List<NameValuePair> getParamList(boolean includePathParams) {
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
      String name = RestAdapter.getName(parameterAnnotations[i], javaMethod, i);
      params.add(new BasicNameValuePair(name, String.valueOf(arg)));
    }

    return params;
  }

  protected BasicNameValuePair addPair(QueryParam queryParam) {
    return new BasicNameValuePair(queryParam.name(), queryParam.value());
  }

  public HttpUriRequest build() throws URISyntaxException {
    // special handling if there are path parameters:
    if (hasPathParameters()) {
      List<NameValuePair> paramList = createParamList();

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