/*
 * Copyright (C) 2015 Square, Inc.
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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.HeaderMap;
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

/** Adapts an invocation of an interface method into an HTTP call. */
final class ServiceMethod<R, T> {
  // Upper and lower characters, digits, underscores, and hyphens, starting with a character.
  static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";
  static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");
  static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);

  final okhttp3.Call.Factory callFactory;
  final CallAdapter<R, T> callAdapter;

  private final HttpUrl baseUrl;
  private final Converter<ResponseBody, R> responseConverter;
  private final String httpMethod;
  private final String relativeUrl;
  private final Headers headers;
  private final MediaType contentType;
  private final boolean hasBody;
  private final boolean isFormEncoded;
  private final boolean isMultipart;
  private final ParameterHandler<?>[] parameterHandlers;

  ServiceMethod(Builder<R, T> builder) {
    this.callFactory = builder.retrofit.callFactory();
    this.callAdapter = builder.callAdapter;
    this.baseUrl = builder.retrofit.baseUrl();
    this.responseConverter = builder.responseConverter;
    this.httpMethod = builder.httpMethod;
    this.relativeUrl = builder.relativeUrl;
    this.headers = builder.headers;
    this.contentType = builder.contentType;
    this.hasBody = builder.hasBody;
    this.isFormEncoded = builder.isFormEncoded;
    this.isMultipart = builder.isMultipart;
    this.parameterHandlers = builder.parameterHandlers;
  }

  /** Builds an HTTP request from method arguments. */
  Request toRequest(Object... args) throws IOException {
    RequestBuilder requestBuilder = new RequestBuilder(httpMethod, baseUrl, relativeUrl, headers,
        contentType, hasBody, isFormEncoded, isMultipart);

    @SuppressWarnings("unchecked") // It is an error to invoke a method with the wrong arg types.
    ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;

    int argumentCount = args != null ? args.length : 0;
    if (argumentCount != handlers.length) {
      throw new IllegalArgumentException("Argument count (" + argumentCount
          + ") doesn't match expected count (" + handlers.length + ")");
    }

    for (int p = 0; p < argumentCount; p++) {
      handlers[p].apply(requestBuilder, args[p]);
    }

    return requestBuilder.build();
  }

  /** Builds a method return value from an HTTP response body. */
  R toResponse(ResponseBody body) throws IOException {
    return responseConverter.convert(body);
  }

  /**
   * Inspects the annotations on an interface method to construct a reusable service method. This
   * requires potentially-expensive reflection so it is best to build each service method only once
   * and reuse it. Builders cannot be reused.
   */
  static final class Builder<T, R> {
    final Retrofit retrofit;
    final Method method;
    final Annotation[] methodAnnotations;
    final Annotation[][] parameterAnnotationsArray;
    final Type[] parameterTypes;

    Type responseType;
    boolean gotField;
    boolean gotPart;
    boolean gotBody;
    boolean gotPath;
    boolean gotQuery;
    boolean gotUrl;
    String httpMethod;
    boolean hasBody;
    boolean isFormEncoded;
    boolean isMultipart;
    String relativeUrl;
    Headers headers;
    MediaType contentType;
    Set<String> relativeUrlParamNames;
    ParameterHandler<?>[] parameterHandlers;
    Converter<ResponseBody, T> responseConverter;
    CallAdapter<T, R> callAdapter;

    Builder(Retrofit retrofit, Method method) {
      this.retrofit = retrofit;
      this.method = method;
      this.methodAnnotations = method.getAnnotations();
      this.parameterTypes = method.getGenericParameterTypes();
      this.parameterAnnotationsArray = method.getParameterAnnotations();
    }

    public ServiceMethod build() {
      callAdapter = createCallAdapter();
      responseType = callAdapter.responseType();
      if (responseType == Response.class || responseType == okhttp3.Response.class) {
        throw methodError("'"
            + Utils.getRawType(responseType).getName()
            + "' is not a valid response body type. Did you mean ResponseBody?");
      }
      responseConverter = createResponseConverter();

      for (Annotation annotation : methodAnnotations) {
        parseMethodAnnotation(annotation);
      }

      if (httpMethod == null) {
        throw methodError("HTTP method annotation is required (e.g., @GET, @POST, etc.).");
      }

      if (!hasBody) {
        if (isMultipart) {
          throw methodError(
              "Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
        }
        if (isFormEncoded) {
          throw methodError("FormUrlEncoded can only be specified on HTTP methods with "
              + "request body (e.g., @POST).");
        }
      }

      int parameterCount = parameterAnnotationsArray.length;
      parameterHandlers = new ParameterHandler<?>[parameterCount];
      for (int p = 0; p < parameterCount; p++) {
        Type parameterType = parameterTypes[p];
        if (Utils.hasUnresolvableType(parameterType)) {
          throw parameterError(p, "Parameter type must not include a type variable or wildcard: %s",
              parameterType);
        }

        Annotation[] parameterAnnotations = parameterAnnotationsArray[p];
        if (parameterAnnotations == null) {
          throw parameterError(p, "No Retrofit annotation found.");
        }

        parameterHandlers[p] = parseParameter(p, parameterType, parameterAnnotations);
      }

      if (relativeUrl == null && !gotUrl) {
        throw methodError("Missing either @%s URL or @Url parameter.", httpMethod);
      }
      if (!isFormEncoded && !isMultipart && !hasBody && gotBody) {
        throw methodError("Non-body HTTP method cannot contain @Body.");
      }
      if (isFormEncoded && !gotField) {
        throw methodError("Form-encoded method must contain at least one @Field.");
      }
      if (isMultipart && !gotPart) {
        throw methodError("Multipart method must contain at least one @Part.");
      }

      return new ServiceMethod<>(this);
    }

    private CallAdapter<T, R> createCallAdapter() {
      Type returnType = method.getGenericReturnType();
      if (Utils.hasUnresolvableType(returnType)) {
        throw methodError(
            "Method return type must not include a type variable or wildcard: %s", returnType);
      }
      if (returnType == void.class) {
        throw methodError("Service methods cannot return void.");
      }
      Annotation[] annotations = method.getAnnotations();
      try {
        //noinspection unchecked
        return (CallAdapter<T, R>) retrofit.callAdapter(returnType, annotations);
      } catch (RuntimeException e) { // Wide exception range because factories are user code.
        throw methodError(e, "Unable to create call adapter for %s", returnType);
      }
    }

    private void parseMethodAnnotation(Annotation annotation) {
      if (annotation instanceof DELETE) {
        parseHttpMethodAndPath("DELETE", ((DELETE) annotation).value(), false);
      } else if (annotation instanceof GET) {
        parseHttpMethodAndPath("GET", ((GET) annotation).value(), false);
      } else if (annotation instanceof HEAD) {
        parseHttpMethodAndPath("HEAD", ((HEAD) annotation).value(), false);
        if (!Void.class.equals(responseType)) {
          throw methodError("HEAD method must use Void as response type.");
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
      } else if (annotation instanceof retrofit2.http.Headers) {
        String[] headersToParse = ((retrofit2.http.Headers) annotation).value();
        if (headersToParse.length == 0) {
          throw methodError("@Headers annotation is empty.");
        }
        headers = parseHeaders(headersToParse);
      } else if (annotation instanceof Multipart) {
        if (isFormEncoded) {
          throw methodError("Only one encoding annotation is allowed.");
        }
        isMultipart = true;
      } else if (annotation instanceof FormUrlEncoded) {
        if (isMultipart) {
          throw methodError("Only one encoding annotation is allowed.");
        }
        isFormEncoded = true;
      }
    }

    private void parseHttpMethodAndPath(String httpMethod, String value, boolean hasBody) {
      if (this.httpMethod != null) {
        throw methodError("Only one HTTP method is allowed. Found: %s and %s.",
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
          throw methodError("URL query string \"%s\" must not have replace block. "
              + "For dynamic query parameters use @Query.", queryParams);
        }
      }

      this.relativeUrl = value;
      this.relativeUrlParamNames = parsePathParameters(value);
    }

    private Headers parseHeaders(String[] headers) {
      Headers.Builder builder = new Headers.Builder();
      for (String header : headers) {
        int colon = header.indexOf(':');
        if (colon == -1 || colon == 0 || colon == header.length() - 1) {
          throw methodError(
              "@Headers value must be in the form \"Name: Value\". Found: \"%s\"", header);
        }
        String headerName = header.substring(0, colon);
        String headerValue = header.substring(colon + 1).trim();
        if ("Content-Type".equalsIgnoreCase(headerName)) {
          MediaType type = MediaType.parse(headerValue);
          if (type == null) {
            throw methodError("Malformed content type: %s", headerValue);
          }
          contentType = type;
        } else {
          builder.add(headerName, headerValue);
        }
      }
      return builder.build();
    }

    private ParameterHandler<?> parseParameter(
        int p, Type parameterType, Annotation[] annotations) {
      ParameterHandler<?> result = null;
      for (Annotation annotation : annotations) {
        ParameterHandler<?> annotationAction = parseParameterAnnotation(
            p, parameterType, annotations, annotation);

        if (annotationAction == null) {
          continue;
        }

        if (result != null) {
          throw parameterError(p, "Multiple Retrofit annotations found, only one allowed.");
        }

        result = annotationAction;
      }

      if (result == null) {
        throw parameterError(p, "No Retrofit annotation found.");
      }

      return result;
    }

    private ParameterHandler<?> parseParameterAnnotation(
        int p, Type type, Annotation[] annotations, Annotation annotation) {
      if (annotation instanceof Url) {
        if (gotUrl) {
          throw parameterError(p, "Multiple @Url method annotations found.");
        }
        if (gotPath) {
          throw parameterError(p, "@Path parameters may not be used with @Url.");
        }
        if (gotQuery) {
          throw parameterError(p, "A @Url parameter must not come after a @Query");
        }
        if (relativeUrl != null) {
          throw parameterError(p, "@Url cannot be used with @%s URL", httpMethod);
        }

        gotUrl = true;

        if (type == HttpUrl.class
            || type == String.class
            || type == URI.class
            || (type instanceof Class && "android.net.Uri".equals(((Class<?>) type).getName()))) {
          return new ParameterHandler.RelativeUrl();
        } else {
          throw parameterError(p,
              "@Url must be okhttp3.HttpUrl, String, java.net.URI, or android.net.Uri type.");
        }

      } else if (annotation instanceof Path) {
        if (gotQuery) {
          throw parameterError(p, "A @Path parameter must not come after a @Query.");
        }
        if (gotUrl) {
          throw parameterError(p, "@Path parameters may not be used with @Url.");
        }
        if (relativeUrl == null) {
          throw parameterError(p, "@Path can only be used with relative url on @%s", httpMethod);
        }
        gotPath = true;

        Path path = (Path) annotation;
        String name = path.value();
        validatePathName(p, name);

        Converter<?, String> converter = retrofit.stringConverter(type, annotations);
        return new ParameterHandler.Path<>(name, converter, path.encoded());

      } else if (annotation instanceof Query) {
        Query query = (Query) annotation;
        String name = query.value();
        gotQuery = true;

        Converter<?, String> converter =
            retrofit.stringConverter(getItemType(type, p), annotations);
        return wrapParameterHandler(type, new ParameterHandler.NamedParameterHandler<>(
            name, new ParameterHandler.Query<>(converter, query.encoded())));

      } else if (annotation instanceof QueryMap) {
        QueryMap queryMap = (QueryMap) annotation;
        Type valueItemType = getMapValueItemType(type, queryMap.multimap(), annotation, p);
        Converter<?, String> converter = retrofit.stringConverter(valueItemType, annotations);
        return wrapMapParameterHandler(type, queryMap.multimap(),
            new ParameterHandler.Query<>(converter, queryMap.encoded()), "Query");

      } else if (annotation instanceof Header) {
        Header header = (Header) annotation;
        String name = header.value();

        Converter<?, String> converter =
            retrofit.stringConverter(getItemType(type, p), annotations);
        return wrapParameterHandler(type, new ParameterHandler.NamedParameterHandler<>(
            name, new ParameterHandler.Header<>(converter)));

      } else if (annotation instanceof HeaderMap) {
        HeaderMap headerMap = (HeaderMap) annotation;
        Type valueItemType = getMapValueItemType(type, headerMap.multimap(), annotation, p);
        Converter<?, String> converter = retrofit.stringConverter(valueItemType, annotations);
        return wrapMapParameterHandler(type, headerMap.multimap(),
            new ParameterHandler.Header<>(converter), "Header");

      } else if (annotation instanceof Field) {
        if (!isFormEncoded) {
          throw parameterError(p, "@Field parameters can only be used with form encoding.");
        }
        Field field = (Field) annotation;
        String name = field.value();

        gotField = true;

        Converter<?, String> converter =
            retrofit.stringConverter(getItemType(type, p), annotations);
        return wrapParameterHandler(type, new ParameterHandler.NamedParameterHandler<>(
            name, new ParameterHandler.Field<>(converter, field.encoded())));

      } else if (annotation instanceof FieldMap) {
        if (!isFormEncoded) {
          throw parameterError(p, "@FieldMap parameters can only be used with form encoding.");
        }
        gotField = true;
        FieldMap fieldMap = (FieldMap) annotation;
        Type valueItemType = getMapValueItemType(type, fieldMap.multimap(), annotation, p);
        Converter<?, String> converter = retrofit.stringConverter(valueItemType, annotations);
        return wrapMapParameterHandler(type, fieldMap.multimap(),
            new ParameterHandler.Field<>(converter, ((FieldMap) annotation).encoded()), "Field");

      } else if (annotation instanceof Part) {
        if (!isMultipart) {
          throw parameterError(p, "@Part parameters can only be used with multipart encoding.");
        }
        Part part = (Part) annotation;
        gotPart = true;

        String partName = part.value();
        Type itemType = getItemType(type, p);

        if (MultipartBody.Part.class.isAssignableFrom((Class<?>) itemType)) {
          if (!partName.isEmpty()) {
            throw parameterError(p, "@Part parameters using the MultipartBody.Part must not "
                    + "include a part name in the annotation.");
          }
          return wrapParameterHandler(type, new ParameterHandler.RawPart());
        } else {
          if (partName.isEmpty()) {
            throw parameterError(p,
                "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
          }
          String encoding = part.encoding();
          Converter<?, RequestBody> converter =
              retrofit.requestBodyConverter(itemType, annotations, methodAnnotations);
          return wrapParameterHandler(type, new ParameterHandler.NamedParameterHandler<>(partName,
              new ParameterHandler.Part<>(encoding, converter))
          );
        }

      } else if (annotation instanceof PartMap) {
        if (!isMultipart) {
          throw parameterError(p, "@PartMap parameters can only be used with multipart encoding.");
        }
        gotPart = true;
        PartMap partMap = (PartMap) annotation;
        Type valueType = getMapValueItemType(type, partMap.multimap(), annotation, p);
        if (MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(valueType))) {
          throw parameterError(p, "@PartMap values cannot be MultipartBody.Part. "
              + "Use @Part List<Part> or a different value type instead.");
        }

        Converter<?, RequestBody> converter =
            retrofit.requestBodyConverter(valueType, annotations, methodAnnotations);

        return wrapMapParameterHandler(type, partMap.multimap(),
            new ParameterHandler.Part<>(partMap.encoding(), converter), "Part");

      } else if (annotation instanceof Body) {
        if (isFormEncoded || isMultipart) {
          throw parameterError(p,
              "@Body parameters cannot be used with form or multi-part encoding.");
        }
        if (gotBody) {
          throw parameterError(p, "Multiple @Body method annotations found.");
        }

        Converter<?, RequestBody> converter;
        try {
          converter = retrofit.requestBodyConverter(type, annotations, methodAnnotations);
        } catch (RuntimeException e) {
          // Wide exception range because factories are user code.
          throw parameterError(e, p, "Unable to create @Body converter for %s", type);
        }
        gotBody = true;
        return new ParameterHandler.Body<>(converter);
      }

      return null; // Not a Retrofit annotation.
    }

    private Type getItemType(Type type, int p) {
      Class<?> rawParameterType = Utils.getRawType(type);
      if (Iterable.class.isAssignableFrom(rawParameterType)) {
        if (!(type instanceof ParameterizedType)) {
          throw parameterError(p, rawParameterType.getSimpleName()
              + " must include generic type (e.g., "
              + rawParameterType.getSimpleName()
              + "<String>)");
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        return Utils.getParameterUpperBound(0, parameterizedType);
      } else if (rawParameterType.isArray()) {
        return boxIfPrimitive(rawParameterType.getComponentType());
      } else {
        return type;
      }
    }

    private Type getMapValueItemType(Type type, boolean multimap, Annotation annotation, int p) {
      String annotationName = annotation.annotationType().getSimpleName();
      Class<?> rawParameterType = Utils.getRawType(type);
      if (!Map.class.isAssignableFrom(rawParameterType)) {
        throw parameterError(p, String.format("@%1$s parameter type must be Map.",
            annotationName));
      }
      Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
      if (!(mapType instanceof ParameterizedType)) {
        throw parameterError(p, "Map must include generic types (e.g., Map<String, String>)");
      }
      ParameterizedType parameterizedType = (ParameterizedType) mapType;
      Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
      if (String.class != keyType) {
        throw parameterError(p, String.format("@%1$s keys must be of type String: %2$s",
            annotationName, keyType));
      }
      Type valueType = Utils.getParameterUpperBound(1, parameterizedType);

      if (multimap) {
        Class<?> rawValueType = Utils.getRawType(valueType);
        if (Iterable.class.isAssignableFrom(rawValueType)) {
          if (!(valueType instanceof ParameterizedType)) {
            throw parameterError(p, "@" + annotationName + " " + rawParameterType.getSimpleName()
                + " value parameter must include generic type (e.g. "
                + rawParameterType.getSimpleName() + "<String, "
                + rawValueType.getSimpleName() + "<String>>).");
          }
          return Utils.getParameterUpperBound(0, (ParameterizedType) valueType);
        } else if (rawValueType.isArray()) {
          return boxIfPrimitive(rawValueType.getComponentType());
        }
      }
      return valueType;
    }

    private ParameterHandler<?> wrapParameterHandler(Type type, ParameterHandler<?> handler) {
      Class<?> rawParameterType = Utils.getRawType(type);
      if (Iterable.class.isAssignableFrom(rawParameterType)) {
        return handler.iterable();
      } else if (rawParameterType.isArray()) {
        return handler.array();
      } else {
        return handler;
      }
    }

    private ParameterHandler<?> wrapMapParameterHandler(Type type, boolean multimap,
        ParameterHandler.NamedValuesHandler<?> handler, String handlerName) {
      Class<?> rawParameterType = Utils.getRawType(type);
      Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
      Type valueType = Utils.getParameterUpperBound(1, (ParameterizedType) mapType);
      Class<?> rawValueType = Utils.getRawType(valueType);

      ParameterHandler.NamedValuesHandler<?> wrappedHandler;
      if (multimap && Iterable.class.isAssignableFrom(rawValueType)) {
        wrappedHandler = handler.iterable();
      } else if (multimap && rawValueType.isArray()) {
        wrappedHandler = handler.array();
      } else {
        wrappedHandler = handler;
      }
      return new ParameterHandler.MapParameterHandler<>(wrappedHandler, handlerName);
    }

    private void validatePathName(int p, String name) {
      if (!PARAM_NAME_REGEX.matcher(name).matches()) {
        throw parameterError(p, "@Path parameter name must match %s. Found: %s",
            PARAM_URL_REGEX.pattern(), name);
      }
      // Verify URL replacement name is actually present in the URL path.
      if (!relativeUrlParamNames.contains(name)) {
        throw parameterError(p, "URL \"%s\" does not contain \"{%s}\".", relativeUrl, name);
      }
    }

    private Converter<ResponseBody, T> createResponseConverter() {
      Annotation[] annotations = method.getAnnotations();
      try {
        return retrofit.responseBodyConverter(responseType, annotations);
      } catch (RuntimeException e) { // Wide exception range because factories are user code.
        throw methodError(e, "Unable to create converter for %s", responseType);
      }
    }

    private RuntimeException methodError(String message, Object... args) {
      return methodError(null, message, args);
    }

    private RuntimeException methodError(Throwable cause, String message, Object... args) {
      message = String.format(message, args);
      return new IllegalArgumentException(message
          + "\n    for method "
          + method.getDeclaringClass().getSimpleName()
          + "."
          + method.getName(), cause);
    }

    private RuntimeException parameterError(
        Throwable cause, int p, String message, Object... args) {
      return methodError(cause, message + " (parameter #" + (p + 1) + ")", args);
    }

    private RuntimeException parameterError(int p, String message, Object... args) {
      return methodError(message + " (parameter #" + (p + 1) + ")", args);
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

  static Class<?> boxIfPrimitive(Class<?> type) {
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
