// Copyright 2012 Square, Inc.
package retrofit.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import retrofit.http.client.Request;
import retrofit.http.mime.TypedOutput;
import retrofit.http.mime.TypedString;

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
  private List<Parameter> createParamList() {
    List<Parameter> params = new ArrayList<Parameter>();

    // Add arguments as parameters.
    String[] pathNamedParams = methodInfo.namedParams;
    int singleEntityArgumentIndex = methodInfo.singleEntityArgumentIndex;
    int baseUrlArgumentIndex = methodInfo.baseUrlArgumentIndex;
    for (int i = 0; i < pathNamedParams.length; i++) {
      Object arg = args[i];
      if (arg == null) continue;
      if ((i != singleEntityArgumentIndex && (i != baseUrlArgumentIndex))) {
        params.add(new Parameter(pathNamedParams[i], arg, arg.getClass()));
      }
    }

    return params;
  }

  Request build() {
    // Alter parameter list if path parameters are present.
    Set<String> pathParams = new LinkedHashSet<String>(methodInfo.pathParams);
    List<Parameter> paramList = createParamList();
    String replacedPath = methodInfo.path;
    for (String pathParam : pathParams) {
      Parameter found = null;
      for (Parameter param : paramList) {
        if (param.getName().equals(pathParam)) {
          found = param;
          break;
        }
      }
      if (found != null) {
        String value = getUrlEncodedValue(found);
        replacedPath = replacedPath.replace("{" + found.getName() + "}", value);
        paramList.remove(found);
      } else {
        throw new IllegalArgumentException(
            "URL param " + pathParam + " has no matching method @Name param.");
      }
    }

    if (methodInfo.singleEntityArgumentIndex != NO_SINGLE_ENTITY) {
      // We're passing a JSON object as the entity: paramList should only contain path param values.
      if (!paramList.isEmpty()) {
        throw new IllegalStateException(
            "Found @Name param on single-entity request that was not used for path substitution.");
      }
    }

    StringBuilder url = new StringBuilder(apiUrl);
    if (apiUrl.endsWith("/")) {
      // We enforce relative paths to start with '/'. Prevent a double-slash.
      url.deleteCharAt(url.length() - 1);
    }
    url.append(replacedPath);

    // Add query parameter(s), if specified.
    for (QueryParam annotation : methodInfo.pathQueryParams) {
      paramList.add(new Parameter(annotation.name(), annotation.value(), String.class));
    }

    TypedOutput body = null;
    if (!methodInfo.restMethod.hasBody()) {
      for (int i = 0, count = paramList.size(); i < count; i++) {
        url.append((i == 0) ? '?' : '&');
        Parameter nonPathParam = paramList.get(i);
        String value = getUrlEncodedValue(nonPathParam);
        url.append(nonPathParam.getName()).append("=").append(value);
      }
    } else if (!paramList.isEmpty()) {
      if (methodInfo.isMultipart) {
        MultipartTypedOutput multipartBody = new MultipartTypedOutput();
        for (Parameter parameter : paramList) {
          Object value = parameter.getValue();
          TypedOutput typedOutput;
          if (value instanceof TypedOutput) {
            typedOutput = (TypedOutput) value;
          } else {
            typedOutput = new TypedString(value.toString());
          }
          multipartBody.addPart(parameter.getName(), typedOutput);
        }
        body = multipartBody;
      } else {
        body = converter.toBody(paramList);
      }
    } else if (methodInfo.singleEntityArgumentIndex != NO_SINGLE_ENTITY) {
      Object singleEntity = args[methodInfo.singleEntityArgumentIndex];
      if (singleEntity instanceof TypedOutput) {
        body = (TypedOutput) singleEntity;
      } else {
        body = converter.toBody(singleEntity);
      }
    }

    return new Request(methodInfo.restMethod.value(), url.toString(), headers, body);
  }

  private static String getUrlEncodedValue(Parameter found) {
    try {
      return URLEncoder.encode(String.valueOf(found.getValue()), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }
}
