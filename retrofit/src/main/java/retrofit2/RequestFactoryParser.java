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
package retrofit2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.OPTIONS;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;

import static retrofit2.Utils.methodError;

final class RequestFactoryParser {
  // Upper and lower characters, digits, underscores, and hyphens, starting with a character.
  private static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";
  private static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);
  private static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");

  static RequestFactory parse(Method method, Type responseType, Retrofit retrofit) {
    RequestFactoryParser parser = new RequestFactoryParser(method);
    parser.parseMethodAnnotations(responseType);
    parser.parseParameters(retrofit);
    return parser.toRequestFactory(retrofit.baseUrl());
  }

  private final Method method;

  private String httpMethod;
  private boolean hasBody;
  private boolean isFormEncoded;
  private boolean isMultipart;
  private String relativeUrl;
  private okhttp3.Headers headers;
  private MediaType contentType;
  private RequestAction[] requestActions;

  private Set<String> relativeUrlParamNames;

  private RequestFactoryParser(Method method) {
    this.method = method;
  }

  private RequestFactory toRequestFactory(BaseUrl baseUrl) {
    return new RequestFactory(httpMethod, baseUrl, relativeUrl, headers, contentType, hasBody,
        isFormEncoded, isMultipart, requestActions);
  }

  private RuntimeException parameterError(Throwable cause, int index, String message,
      Object... args) {
    return methodError(cause, method, message + " (parameter #" + (index + 1) + ")", args);
  }

  private RuntimeException parameterError(int index, String message, Object... args) {
    return methodError(method, message + " (parameter #" + (index + 1) + ")", args);
  }

  private void parseMethodAnnotations(Type responseType) {
    for (Annotation annotation : method.getAnnotations()) {
      if (annotation instanceof DELETE) {
        parseHttpMethodAndPath("DELETE", ((DELETE) annotation).value(), false);
      } else if (annotation instanceof GET) {
        parseHttpMethodAndPath("GET", ((GET) annotation).value(), false);
      } else if (annotation instanceof HEAD) {
        parseHttpMethodAndPath("HEAD", ((HEAD) annotation).value(), false);
        if (!Void.class.equals(responseType)) {
          throw methodError(method, "HEAD method must use Void as response type.");
        }
      } else if (annotation instanceof PATCH) {
        parseHttpMethodAndPath("PATCH", ((PATCH) annotation).value(), true);
      } else if (annotation instanceof POST) {
        parseHttpMethodAndPath("POST", ((POST) annotation).value(), true);
      } else if (annotation instanceof PUT) {
        parseHttpMethodAndPath("PUT", ((PUT) annotation).value(), true);
      } else if (annotation instanceof OPTIONS) {
        parseHttpMethodAndPath("OPTIONS", ((OPTIONS) annotation).value(), false);
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

  private okhttp3.Headers parseHeaders(String[] headers) {
    okhttp3.Headers.Builder builder = new okhttp3.Headers.Builder();
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

  private void parseParameters(Retrofit retrofit) {
    Type[] methodParameterTypes = method.getGenericParameterTypes();
    Annotation[][] methodParameterAnnotationArrays = method.getParameterAnnotations();

    boolean gotField = false;
    boolean gotPart = false;
    boolean gotBody = false;
    boolean gotPath = false;
    boolean gotQuery = false;
    boolean gotUrl = false;

    int count = methodParameterAnnotationArrays.length;
    RequestAction[] requestActions = new RequestAction[count];
    for (int i = 0; i < count; i++) {
      Type methodParameterType = methodParameterTypes[i];
      if (Utils.hasUnresolvableType(methodParameterType)) {
        throw parameterError(i, "Parameter type must not include a type variable or wildcard: %s",
            methodParameterType);
      }

      Annotation[] methodParameterAnnotations = methodParameterAnnotationArrays[i];
      if (methodParameterAnnotations != null) {
        for (Annotation methodParameterAnnotation : methodParameterAnnotations) {
          RequestAction action = null;
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
            action = new RequestAction.Url();

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

            Converter<?, String> valueConverter =
                retrofit.stringConverter(methodParameterType, methodParameterAnnotations);
            action = new RequestAction.Path<>(name, valueConverter, path.encoded());

          } else if (methodParameterAnnotation instanceof Query) {
            Query query = (Query) methodParameterAnnotation;
            String name = query.value();
            boolean encoded = query.encoded();

            Class<?> rawParameterType = Utils.getRawType(methodParameterType);
            if (Iterable.class.isAssignableFrom(rawParameterType)) {
              if (!(methodParameterType instanceof ParameterizedType)) {
                throw parameterError(i, rawParameterType.getSimpleName()
                    + " must include generic type (e.g., "
                    + rawParameterType.getSimpleName()
                    + "<String>)");
              }
              ParameterizedType parameterizedType = (ParameterizedType) methodParameterType;
              Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
              Converter<?, String> valueConverter =
                  retrofit.stringConverter(iterableType, methodParameterAnnotations);
              action = new RequestAction.Query<>(name, valueConverter, encoded).iterable();
            } else if (rawParameterType.isArray()) {
              Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
              Converter<?, String> valueConverter =
                  retrofit.stringConverter(arrayComponentType, methodParameterAnnotations);
              action = new RequestAction.Query<>(name, valueConverter, encoded).array();
            } else {
              Converter<?, String> valueConverter =
                  retrofit.stringConverter(methodParameterType, methodParameterAnnotations);
              action = new RequestAction.Query<>(name, valueConverter, encoded);
            }

            gotQuery = true;

          } else if (methodParameterAnnotation instanceof QueryMap) {
            if (!Map.class.isAssignableFrom(Utils.getRawType(methodParameterType))) {
              throw parameterError(i, "@QueryMap parameter type must be Map.");
            }
            if (!(methodParameterType instanceof ParameterizedType)) {
              throw parameterError(i, "Map must include generic types (e.g., Map<String, String>)");
            }
            ParameterizedType parameterizedType = (ParameterizedType) methodParameterType;
            Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
            if (String.class != keyType) {
              throw parameterError(i, "@QueryMap keys must be of type String: " + keyType);
            }
            Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
            Converter<?, String> valueConverter =
                retrofit.stringConverter(valueType, methodParameterAnnotations);

            QueryMap queryMap = (QueryMap) methodParameterAnnotation;
            action = new RequestAction.QueryMap<>(valueConverter, queryMap.encoded());

          } else if (methodParameterAnnotation instanceof Header) {
            Header header = (Header) methodParameterAnnotation;
            String name = header.value();

            Class<?> rawParameterType = Utils.getRawType(methodParameterType);
            if (Iterable.class.isAssignableFrom(rawParameterType)) {
              if (!(methodParameterType instanceof ParameterizedType)) {
                throw parameterError(i, rawParameterType.getSimpleName()
                    + " must include generic type (e.g., "
                    + rawParameterType.getSimpleName()
                    + "<String>)");
              }
              ParameterizedType parameterizedType = (ParameterizedType) methodParameterType;
              Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
              Converter<?, String> valueConverter =
                  retrofit.stringConverter(iterableType, methodParameterAnnotations);
              action = new RequestAction.Header<>(name, valueConverter).iterable();
            } else if (rawParameterType.isArray()) {
              Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
              Converter<?, String> valueConverter =
                  retrofit.stringConverter(arrayComponentType, methodParameterAnnotations);
              action = new RequestAction.Header<>(name, valueConverter).array();
            } else {
              Converter<?, String> valueConverter =
                  retrofit.stringConverter(methodParameterType, methodParameterAnnotations);
              action = new RequestAction.Header<>(name, valueConverter);
            }

          } else if (methodParameterAnnotation instanceof Field) {
            if (!isFormEncoded) {
              throw parameterError(i, "@Field parameters can only be used with form encoding.");
            }
            Field field = (Field) methodParameterAnnotation;
            String name = field.value();
            boolean encoded = field.encoded();

            Class<?> rawParameterType = Utils.getRawType(methodParameterType);
            if (Iterable.class.isAssignableFrom(rawParameterType)) {
              if (!(methodParameterType instanceof ParameterizedType)) {
                throw parameterError(i, rawParameterType.getSimpleName()
                    + " must include generic type (e.g., "
                    + rawParameterType.getSimpleName()
                    + "<String>)");
              }
              ParameterizedType parameterizedType = (ParameterizedType) methodParameterType;
              Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
              Converter<?, String> valueConverter =
                  retrofit.stringConverter(iterableType, methodParameterAnnotations);
              action = new RequestAction.Field<>(name, valueConverter, encoded).iterable();
            } else if (rawParameterType.isArray()) {
              Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
              Converter<?, String> valueConverter =
                  retrofit.stringConverter(arrayComponentType, methodParameterAnnotations);
              action = new RequestAction.Field<>(name, valueConverter, encoded).array();
            } else {
              Converter<?, String> valueConverter =
                  retrofit.stringConverter(methodParameterType, methodParameterAnnotations);
              action = new RequestAction.Field<>(name, valueConverter, encoded);
            }

            gotField = true;

          } else if (methodParameterAnnotation instanceof FieldMap) {
            if (!isFormEncoded) {
              throw parameterError(i, "@FieldMap parameters can only be used with form encoding.");
            }
            if (!Map.class.isAssignableFrom(Utils.getRawType(methodParameterType))) {
              throw parameterError(i, "@FieldMap parameter type must be Map.");
            }
            if (!(methodParameterType instanceof ParameterizedType)) {
              throw parameterError(i, "Map must include generic types (e.g., Map<String, String>)");
            }
            ParameterizedType parameterizedType = (ParameterizedType) methodParameterType;
            Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
            if (String.class != keyType) {
              throw parameterError(i, "@FieldMap keys must be of type String: " + keyType);
            }
            Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
            Converter<?, String> valueConverter =
                retrofit.stringConverter(valueType, methodParameterAnnotations);

            FieldMap fieldMap = (FieldMap) methodParameterAnnotation;
            action = new RequestAction.FieldMap<>(valueConverter, fieldMap.encoded());
            gotField = true;

          } else if (methodParameterAnnotation instanceof Part) {
            if (!isMultipart) {
              throw parameterError(i, "@Part parameters can only be used with multipart encoding.");
            }
            Part part = (Part) methodParameterAnnotation;
            okhttp3.Headers headers = okhttp3.Headers.of(
                "Content-Disposition", "form-data; name=\"" + part.value() + "\"",
                "Content-Transfer-Encoding", part.encoding());

            Class<?> rawParameterType = Utils.getRawType(methodParameterType);
            if (Iterable.class.isAssignableFrom(rawParameterType)) {
              if (!(methodParameterType instanceof ParameterizedType)) {
                throw parameterError(i, rawParameterType.getSimpleName()
                    + " must include generic type (e.g., "
                    + rawParameterType.getSimpleName()
                    + "<String>)");
              }
              ParameterizedType parameterizedType = (ParameterizedType) methodParameterType;
              Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
              Converter<?, RequestBody> valueConverter =
                  retrofit.requestBodyConverter(iterableType, methodParameterAnnotations);
              action = new RequestAction.Part<>(headers, valueConverter).iterable();
            } else if (rawParameterType.isArray()) {
              Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
              Converter<?, RequestBody> valueConverter =
                  retrofit.requestBodyConverter(arrayComponentType, methodParameterAnnotations);
              action = new RequestAction.Part<>(headers, valueConverter).array();
            } else {
              Converter<?, RequestBody> valueConverter =
                  retrofit.requestBodyConverter(methodParameterType, methodParameterAnnotations);
              action = new RequestAction.Part<>(headers, valueConverter);
            }

            gotPart = true;

          } else if (methodParameterAnnotation instanceof PartMap) {
            if (!isMultipart) {
              throw parameterError(i,
                  "@PartMap parameters can only be used with multipart encoding.");
            }
            if (!Map.class.isAssignableFrom(Utils.getRawType(methodParameterType))) {
              throw parameterError(i, "@PartMap parameter type must be Map.");
            }
            if (!(methodParameterType instanceof ParameterizedType)) {
              throw parameterError(i, "Map must include generic types (e.g., Map<String, String>)");
            }
            ParameterizedType parameterizedType = (ParameterizedType) methodParameterType;
            Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
            if (String.class != keyType) {
              throw parameterError(i, "@PartMap keys must be of type String: " + keyType);
            }
            Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
            Converter<?, RequestBody> valueConverter =
                retrofit.requestBodyConverter(valueType, methodParameterAnnotations);

            PartMap partMap = (PartMap) methodParameterAnnotation;
            action = new RequestAction.PartMap<>(valueConverter, partMap.encoding());
            gotPart = true;

          } else if (methodParameterAnnotation instanceof Body) {
            if (isFormEncoded || isMultipart) {
              throw parameterError(i,
                  "@Body parameters cannot be used with form or multi-part encoding.");
            }
            if (gotBody) {
              throw parameterError(i, "Multiple @Body method annotations found.");
            }

            Converter<?, RequestBody> converter;
            try {
              converter =
                  retrofit.requestBodyConverter(methodParameterType, methodParameterAnnotations);
            } catch (RuntimeException e) { // Wide exception range because factories are user code.
              throw parameterError(e, i, "Unable to create @Body converter for %s",
                  methodParameterType);
            }
            action = new RequestAction.Body<>(converter);
            gotBody = true;
          }

          if (action != null) {
            if (requestActions[i] != null) {
              throw parameterError(i, "Multiple Retrofit annotations found, only one allowed.");
            }
            requestActions[i] = action;
          }
        }
      }

      if (requestActions[i] == null) {
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

    this.requestActions = requestActions;
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

  private static Class<?> boxIfPrimitive(Class<?> type) {
    if (boolean.class == type) return Boolean.class;
    if (byte.class == type) return Byte.class;
    if (char.class == type) return Character.class;
    if (double.class == type) return Double.class;
    if (float.class == type) return Float.class;
    if (int.class == type) return Integer.class;
    if (long.class == type) return Long.class;
    if (short.class == type) return Short.class;
    return type;
  }
}
