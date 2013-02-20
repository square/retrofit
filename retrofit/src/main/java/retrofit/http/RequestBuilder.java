// Copyright 2012 Square, Inc.
package retrofit.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import retrofit.http.client.Request;
import retrofit.http.mime.TypedOutput;
import retrofit.http.mime.TypedString;

import static retrofit.http.RestAdapter.UTF_8;
import static retrofit.http.RestMethodInfo.NO_SINGLE_ENTITY;

/**
 * Builds HTTP requests from Java method invocations.  Handles "path parameters" in the
 * {@code apiUrl} in the form of "path/to/url/{id}/action" where a parameter annotated with
 * {@code @Name("id")} is inserted into the url.  Note that this replacement can be recursive if:
 * <ol>
 * <li>Multiple sets of brackets are nested ("path/to/{{key}a}.</li>
 * <li>The order of {@link Name @Name} values go from innermost to outermost.</li>
 * <li>The values replaced correspond to {@link Name @Name} parameters.</li>
 * </ol>
 */
final class RequestBuilder {
  private final Converter converter;

  private RestMethodInfo methodInfo;
  private Object[] args;
  private String apiUrl;
  private List<Header> headers;

  RequestBuilder(Converter converter) {
    this.converter = converter;
  }

  RequestBuilder setMethodInfo(RestMethodInfo methodDetails) {
    this.methodInfo = methodDetails;
    return this;
  }

  RequestBuilder setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
    return this;
  }

  RequestBuilder setArgs(Object[] args) {
    this.args = args;
    return this;
  }

  RequestBuilder setHeaders(List<Header> headers) {
    this.headers = headers;
    return this;
  }

  /** List of all URL parameters. Return value will be mutated. */
  private Map<String, Parameter> createParamList() {
    Map<String, Parameter> params = new LinkedHashMap<String, Parameter>();

    String[] pathNamedParams = methodInfo.namedParams;
    int singleEntityArgumentIndex = methodInfo.singleEntityArgumentIndex;
    for (int i = 0; i < pathNamedParams.length; i++) {
      Object arg = args[i];
      if (arg == null || i == singleEntityArgumentIndex) continue;
      String name = pathNamedParams[i];
      params.put(name, new Parameter(name, arg, arg.getClass()));
    }

    return params;
  }

  Request build() {
    StringBuilder url = new StringBuilder(apiUrl);
    if (apiUrl.endsWith("/")) {
      // We enforce relative paths to start with '/'. Prevent a double-slash.
      url.deleteCharAt(url.length() - 1);
    }

    Map<String, Parameter> params = createParamList();
    if (methodInfo.pathParams.isEmpty()) {
      url.append(methodInfo.path);
    } else {
      String path = methodInfo.path;
      int end = -1;
      for (int start = path.indexOf("{"); start != -1; start = path.indexOf("{", end)) {
        url.append(path.substring(end + 1, start));

        end = path.indexOf("}", start);
        if (end == -1) {
          break; // No more keys.
        }
        String key = path.substring(start + 1, end);
        Parameter param = params.remove(key);
        if (param != null) {
          Object value = param.getValue();
          if (value != null) {
            url.append(value.toString());
          }
        }
      }
      if (end < path.length()) {
        url.append(path.substring(end + 1));
      }
    }

    // Add query parameter(s), if specified.
    for (QueryParam annotation : methodInfo.pathQueryParams) {
      String name = annotation.name();
      params.put(name, new Parameter(name, annotation.value(), String.class));
    }

    TypedOutput body = null;
    Map<String, TypedOutput> bodyParams = new LinkedHashMap<String, TypedOutput>();
    if (!methodInfo.restMethod.hasBody()) {
      // HTTP method does not have a request body. Append remaining parameters, if any.
      boolean first = true;
      for (Parameter param : params.values()) {
        url.append(first ? '?' : '&');
        first = false;

        Object value = param.getValue();
        if (value != null) {
          try {
            url.append(param.getName())
                .append("=")
                .append(URLEncoder.encode(String.valueOf(value), UTF_8));
          } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to URL encode \"" + value + "\"", e);
          }
        }
      }
    } else if (methodInfo.singleEntityArgumentIndex != NO_SINGLE_ENTITY) {
      // HTTP method has a request body and one parameter which represents it.
      Object singleEntity = args[methodInfo.singleEntityArgumentIndex];
      if (singleEntity instanceof TypedOutput) {
        body = (TypedOutput) singleEntity;
      } else {
        body = converter.toBody(singleEntity);
      }
    } else if (!params.isEmpty()) {
      if (!methodInfo.isMultipart) {
        throw new IllegalStateException("Non-multipart request body with remaining parameters.");
      }

      // HTTP method has a request body and remaining parameters.
      for (Parameter parameter : params.values()) {
        Object value = parameter.getValue();
        TypedOutput typedOutput;
        if (value instanceof TypedOutput) {
          typedOutput = (TypedOutput) value;
        } else {
          typedOutput = new TypedString(value.toString());
        }
        bodyParams.put(parameter.getName(), typedOutput);
      }
    }

    return new Request(methodInfo.restMethod.value(), url.toString(), headers,
        methodInfo.isMultipart, body, bodyParams);
  }
}