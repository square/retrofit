/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit;

import com.squareup.okhttp.MediaType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FieldMap;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.HEAD;
import retrofit.http.HTTP;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.Multipart;
import retrofit.http.PATCH;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.PartMap;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.QueryMap;
import retrofit.http.Url;

import static retrofit.Utils.methodError;

final class RequestFactoryParser {
  // Upper and lower characters, digits, underscores, and hyphens, starting with a character.
  private static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";
  private static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);
  private static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");

  static RequestFactory parse(Method method, BaseUrl baseUrl,
      List<Converter.Factory> converterFactories) {
    RequestFactoryParser parser = new RequestFactoryParser(method);
    parser.parseMethodAnnotations();
    parser.parseParameters(converterFactories);
    return parser.toRequestFactory(baseUrl);
  }

  private final Method method;

  private String httpMethod;
  private boolean hasBody;
  private boolean isFormEncoded;
  private boolean isMultipart;
  private String relativeUrl;
  private com.squareup.okhttp.Headers headers;
  private MediaType contentType;
  private RequestBuilderAction[] requestBuilderActions;

  private Set<String> relativeUrlParamNames;

  private RequestFactoryParser(Method method) {
    this.method = method;
  }

  private RequestFactory toRequestFactory(BaseUrl baseUrl) {
    return new RequestFactory(httpMethod, baseUrl, relativeUrl, headers, contentType, hasBody,
        isFormEncoded, isMultipart, requestBuilderActions);
  }

  private RuntimeException parameterError(Throwable cause, int index, String message,
      Object... args) {
    return methodError(cause, method, message + " (parameter #" + (index + 1) + ")", args);
  }

  private RuntimeException parameterError(int index, String message, Object... args) {
    return methodError(method, message + " (parameter #" + (index + 1) + ")", args);
  }

  private void parseMethodAnnotations() {
    for (Annotation annotation : method.getAnnotations()) {
      if (annotation instanceof DELETE) {
        parseHttpMethodAndPath("DELETE", ((DELETE) annotation).value(), false);
      } else if (annotation instanceof GET) {
        parseHttpMethodAndPath("GET", ((GET) annotation).value(), false);
      } else if (annotation instanceof HEAD) {
        parseHttpMethodAndPath("HEAD", ((HEAD) annotation).value(), false);
      } else if (annotation instanceof PATCH) {
        parseHttpMethodAndPath("PATCH", ((PATCH) annotation).value(), true);
      } else if (annotation instanceof POST) {
        parseHttpMethodAndPath("POST", ((POST) annotation).value(), true);
      } else if (annotation instanceof PUT) {
        parseHttpMethodAndPath("PUT", ((PUT) annotation).value(), true);
      } else if (annotation instanceof HTTP) {
        HTTP http = (HTTP) annotation;
        parseHttpMethodAndPath(http.method(), http.path(), http.hasBody());
      } else if (annotation instanceof Headers) {
        String[] headersToParse = ((Headers) annotation).value();
        if (headersToParse.length == 0) {
          throw methodError(method, "@Headers annotation is empty.");
        }
        headers = parseHeaders(headersToParse);
      } else if (annotation instanceof Multipart) {
        if (isFormEncoded) {
          throw methodError(method, "Only one encoding annotation is allowed.");
        }
        isMultipart = true;
      } else if (annotation instanceof FormUrlEncoded) {
        if (isMultipart) {
          throw methodError(method, "Only one encoding annotation is allowed.");
        }
        isFormEncoded = true;
      }
    }

    if (httpMethod == null) {
      throw methodError(method, "HTTP method annotation is required (e.g., @GET, @POST, etc.).");
    }
    if (!hasBody) {
      if (isMultipart) {
        throw methodError(method,
            "Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
      }
      if (isFormEncoded) {
        throw methodError(method,
            "FormUrlEncoded can only be specified on HTTP methods with request body "
                + "(e.g., @POST).");
      }
    }
  }

  private void parseHttpMethodAndPath(String httpMethod, String value, boolean hasBody) {
    if (this.httpMethod != null) {
      throw methodError(method, "Only one HTTP method is allowed. Found: %s and %s.",
          this.httpMethod, httpMethod);
    }
    this.httpMethod = httpMethod;
    this.hasBody = hasBody;

    if (value.isEmpty()) {
      return;
    }

    // Get the relative URL path and existing query string, if present.
    int question = value.indexOf('?');
    if (question != -1 && question < value.length() - 1) {
      // Ensure the query string does not have any named parameters.
      String queryParams = value.substring(question + 1);
      Matcher queryParamMatcher = PARAM_URL_REGEX.matcher(queryParams);
      if (queryParamMatcher.find()) {
        throw methodError(method, "URL query string \"%s\" must not have replace block. "
            + "For dynamic query parameters use @Query.", queryParams);
      }
    }

    this.relativeUrl = value;
    this.relativeUrlParamNames = parsePathParameters(value);
  }

  private com.squareup.okhttp.Headers parseHeaders(String[] headers) {
    com.squareup.okhttp.Headers.Builder builder = new com.squareup.okhttp.Headers.Builder();
    for (String header : headers) {
      int colon = header.indexOf(':');
      if (colon == -1 || colon == 0 || colon == header.length() - 1) {
        throw methodError(method,
            "@Headers value must be in the form \"Name: Value\". Found: \"%s\"", header);
      }
      String headerName = header.substring(0, colon);
      String headerValue = header.substring(colon + 1).trim();
      if ("Content-Type".equalsIgnoreCase(headerName)) {
        contentType = MediaType.parse(headerValue);
      } else {
        builder.add(headerName, headerValue);
      }
    }
    return builder.build();
  }

  private void parseParameters(List<Converter.Factory> converterFactories) {
    Type[] methodParameterTypes = method.getGenericParameterTypes();
    Annotation[][] methodParameterAnnotationArrays = method.getParameterAnnotations();

    boolean gotField = false;
    boolean gotPart = false;
    boolean gotBody = false;
    boolean gotPath = false;
    boolean gotQuery = false;
    boolean gotUrl = false;

    int count = methodParameterAnnotationArrays.length;
    RequestBuilderAction[] requestBuilderActions = new RequestBuilderAction[count];
    for (int i = 0; i < count; i++) {
      Type methodParameterType = methodParameterTypes[i];
      Annotation[] methodParameterAnnotations = methodParameterAnnotationArrays[i];
      if (methodParameterAnnotations != null) {
        for (Annotation methodParameterAnnotation : methodParameterAnnotations) {
          RequestBuilderAction action = null;
          if (methodParameterAnnotation instanceof Url) {
            if (gotUrl) {
              throw parameterError(i, "Multiple @Url method annotations found.");
            }
            if (gotPath) {
              throw parameterError(i, "@Path parameters may not be used with @Url.");
            }
            if (gotQuery) {
              throw parameterError(i, "A @Url parameter must not come after a @Query");
            }
            if (methodParameterType != String.class) {
              throw parameterError(i, "@Url must be String type.");
            }
            if (relativeUrl != null) {
              throw parameterError(i, "@Url cannot be used with @%s URL", httpMethod);
            }
            gotUrl = true;
            action = new RequestBuilderAction.Url();

          } else if (methodParameterAnnotation instanceof Path) {
            if (gotQuery) {
              throw parameterError(i, "A @Path parameter must not come after a @Query.");
            }
            if (gotUrl) {
              throw parameterError(i, "@Path parameters may not be used with @Url.");
            }
            if (relativeUrl == null) {
              throw parameterError(i, "@Path can only be used with relative url on @%s",
                  httpMethod);
            }
            gotPath = true;

            Path path = (Path) methodParameterAnnotation;
            String name = path.value();
            validatePathName(i, name);
            action = new RequestBuilderAction.Path(name, path.encoded());

          } else if (methodParameterAnnotation instanceof Query) {
            Query query = (Query) methodParameterAnnotation;
            action = new RequestBuilderAction.Query(query.value(), query.encoded());
            gotQuery = true;

          } else if (methodParameterAnnotation instanceof QueryMap) {
            if (!Map.class.isAssignableFrom(Utils.getRawType(methodParameterType))) {
              throw parameterError(i, "@QueryMap parameter type must be Map.");
            }
            QueryMap queryMap = (QueryMap) methodParameterAnnotation;
            action = new RequestBuilderAction.QueryMap(queryMap.encoded());

          } else if (methodParameterAnnotation instanceof Header) {
            Header header = (Header) methodParameterAnnotation;
            action = new RequestBuilderAction.Header(header.value());

          } else if (methodParameterAnnotation instanceof Field) {
            if (!isFormEncoded) {
              throw parameterError(i, "@Field parameters can only be used with form encoding.");
            }
            Field field = (Field) methodParameterAnnotation;
            action = new RequestBuilderAction.Field(field.value(), field.encoded());
            gotField = true;

          } else if (methodParameterAnnotation instanceof FieldMap) {
            if (!isFormEncoded) {
              throw parameterError(i, "@FieldMap parameters can only be used with form encoding.");
            }
            if (!Map.class.isAssignableFrom(Utils.getRawType(methodParameterType))) {
              throw parameterError(i, "@FieldMap parameter type must be Map.");
            }
            FieldMap fieldMap = (FieldMap) methodParameterAnnotation;
            action = new RequestBuilderAction.FieldMap(fieldMap.encoded());
            gotField = true;

          } else if (methodParameterAnnotation instanceof Part) {
            if (!isMultipart) {
              throw parameterError(i, "@Part parameters can only be used with multipart encoding.");
            }
            Part part = (Part) methodParameterAnnotation;
            com.squareup.okhttp.Headers headers = com.squareup.okhttp.Headers.of(
                "Content-Disposition", "name=\"" + part.value() + "\"",
                "Content-Transfer-Encoding", part.encoding());
            Converter<?> converter;
            try {
              converter = Utils.resolveConverter(converterFactories, methodParameterType,
                  methodParameterAnnotations);
            } catch (RuntimeException e) { // Wide exception range because factories are user code.
              throw parameterError(e, i, "Unable to create @Part converter for %s",
                  methodParameterType);
            }
            action = new RequestBuilderAction.Part<>(headers, converter);
            gotPart = true;

          } else if (methodParameterAnnotation instanceof PartMap) {
            if (!isMultipart) {
              throw parameterError(i,
                  "@PartMap parameters can only be used with multipart encoding.");
            }
            if (!Map.class.isAssignableFrom(Utils.getRawType(methodParameterType))) {
              throw parameterError(i, "@PartMap parameter type must be Map.");
            }
            PartMap partMap = (PartMap) methodParameterAnnotation;
            action = new RequestBuilderAction.PartMap(converterFactories, partMap.encoding(),
                methodParameterAnnotations);
            gotPart = true;

          } else if (methodParameterAnnotation instanceof Body) {
            if (isFormEncoded || isMultipart) {
              throw parameterError(i,
                  "@Body parameters cannot be used with form or multi-part encoding.");
            }
            if (gotBody) {
              throw parameterError(i, "Multiple @Body method annotations found.");
            }

            Converter<?> converter;
            try {
              converter = Utils.resolveConverter(converterFactories, methodParameterType,
                  methodParameterAnnotations);
            } catch (RuntimeException e) { // Wide exception range because factories are user code.
              throw parameterError(e, i, "Unable to create @Body converter for %s",
                  methodParameterType);
            }
            action = new RequestBuilderAction.Body<>(converter);
            gotBody = true;
          }

          if (action != null) {
            if (requestBuilderActions[i] != null) {
              throw parameterError(i, "Multiple Retrofit annotations found, only one allowed.");
            }
            requestBuilderActions[i] = action;
          }
        }
      }

      if (requestBuilderActions[i] == null) {
        throw parameterError(i, "No Retrofit annotation found.");
      }
    }

    if (relativeUrl == null && !gotUrl) {
      throw methodError(method, "Missing either @%s URL or @Url parameter.", httpMethod);
    }
    if (!isFormEncoded && !isMultipart && !hasBody && gotBody) {
      throw methodError(method, "Non-body HTTP method cannot contain @Body.");
    }
    if (isFormEncoded && !gotField) {
      throw methodError(method, "Form-encoded method must contain at least one @Field.");
    }
    if (isMultipart && !gotPart) {
      throw methodError(method, "Multipart method must contain at least one @Part.");
    }

    this.requestBuilderActions = requestBuilderActions;
  }

  private void validatePathName(int index, String name) {
    if (!PARAM_NAME_REGEX.matcher(name).matches()) {
      throw parameterError(index, "@Path parameter name must match %s. Found: %s",
          PARAM_URL_REGEX.pattern(), name);
    }
    // Verify URL replacement name is actually present in the URL path.
    if (!relativeUrlParamNames.contains(name)) {
      throw parameterError(index, "URL \"%s\" does not contain \"{%s}\".", relativeUrl, name);
    }
  }

  /**
   * Gets the set of unique path parameters used in the given URI. If a parameter is used twice
   * in the URI, it will only show up once in the set.
   */
  static Set<String> parsePathParameters(String path) {
    Matcher m = PARAM_URL_REGEX.matcher(path);
    Set<String> patterns = new LinkedHashSet<>();
    while (m.find()) {
      patterns.add(m.group(1));
    }
    return patterns;
  }
}
