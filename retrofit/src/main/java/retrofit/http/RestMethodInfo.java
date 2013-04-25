// Copyright 2013 Square, Inc.
package retrofit.http;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import retrofit.http.mime.TypedOutput;

/** Cached details about an interface method. */
final class RestMethodInfo {
  static final int NO_BASE_URL = -1;
  static final int NO_SINGLE_ENTITY = -1;
  private static final Pattern PATH_PARAMETERS = Pattern.compile("\\{([a-z_-]+)\\}");

  final Method method;
  final boolean isSynchronous;

  boolean loaded = false;

  Type type;
  RestMethod restMethod;
  String path;
  Set<String> pathParams;
  QueryParam[] pathQueryParams;
  String[] namedParams;
  int singleEntityArgumentIndex = NO_SINGLE_ENTITY;
  int baseUrlArgumentIndex = NO_BASE_URL;
  boolean isMultipart = false;

  RestMethodInfo(Method method) {
    this.method = method;
    isSynchronous = parseResponseType();
  }

  synchronized void init() {
    if (loaded) return;

    parseMethodAnnotations();
    parseParameters();

    loaded = true;
  }

  /**
   * Loads {@link #restMethod}, {@link #path}, {@link #pathParams}, {@link #pathQueryParams}, and
   * {@link #isMultipart}.
   */
  private void parseMethodAnnotations() {
    for (Annotation methodAnnotation : method.getAnnotations()) {
      Class<? extends Annotation> annotationType = methodAnnotation.annotationType();
      RestMethod methodInfo = null;
      for (Annotation innerAnnotation : annotationType.getAnnotations()) {
        if (RestMethod.class == innerAnnotation.annotationType()) {
          methodInfo = (RestMethod) innerAnnotation;
          break;
        }
      }
      if (methodInfo != null) {
        if (restMethod != null) {
          throw new IllegalArgumentException("Method contains multiple HTTP annotations.");
        }
        try {
          path = (String) annotationType.getMethod("value").invoke(methodAnnotation);
        } catch (Exception e) {
          throw new RuntimeException("Failed to extract URI path.", e);
        }
        if (!path.startsWith("/")) {
          throw new IllegalArgumentException("URL path must be prefixed with '/'.");
        }
        pathParams = parsePathParameters(path);
        restMethod = methodInfo;
      } else if (annotationType == QueryParams.class) {
        if (pathQueryParams != null) {
          throw new IllegalStateException(
              "QueryParam and QueryParams annotations are mutually exclusive.");
        }
        pathQueryParams = ((QueryParams) methodAnnotation).value();
        if (pathQueryParams.length == 0) {
          throw new IllegalStateException("QueryParams annotation was empty.");
        }
      } else if (annotationType == QueryParam.class) {
        if (pathQueryParams != null) {
          throw new IllegalStateException(
              "QueryParam and QueryParams annotations are mutually exclusive.");
        }
        pathQueryParams = new QueryParam[] { (QueryParam) methodAnnotation };
      } else if (annotationType == Multipart.class) {
        isMultipart = true;
      }
    }

    if (restMethod == null) {
      throw new IllegalStateException(
          "Method " + method + " not annotated with request type (e.g., GET, POST).");
    }
    if (!restMethod.hasBody() && isMultipart) {
      throw new IllegalStateException(
          "Multipart can only be specific on HTTP methods with request body (e.g., POST).");
    }
    if (pathQueryParams == null) {
      pathQueryParams = new QueryParam[0];
    } else {
      for (QueryParam pathQueryParam : pathQueryParams) {
        if (pathParams.contains(pathQueryParam.name())) {
          throw new IllegalStateException("Query parameters cannot be present in URL.");
        }
      }
    }
  }

  /** Loads {@link #type}. Returns true if the method is synchronous. */
  private boolean parseResponseType() {
    // Synchronous methods have a non-void return type.
    Type returnType = method.getGenericReturnType();

    // Asynchronous methods should have a Callback type as the last argument.
    Type lastArgType = null;
    Class<?> lastArgClass = null;
    Type[] parameterTypes = method.getGenericParameterTypes();
    if (parameterTypes.length > 0) {
      Type typeToCheck = parameterTypes[parameterTypes.length - 1];
      lastArgType = typeToCheck;
      if (typeToCheck instanceof ParameterizedType) {
        typeToCheck = ((ParameterizedType) typeToCheck).getRawType();
      }
      if (typeToCheck instanceof Class) {
        lastArgClass = (Class<?>) typeToCheck;
      }
    }

    boolean hasReturnType = returnType != void.class;
    boolean hasCallback = lastArgClass != null && Callback.class.isAssignableFrom(lastArgClass);

    // Check for invalid configurations.
    if (hasReturnType && hasCallback) {
      throw new IllegalArgumentException(
          "Method may only have return type or Callback as last argument, not both.");
    }
    if (!hasReturnType && !hasCallback) {
      throw new IllegalArgumentException(
          "Method must have either a return type or Callback as last argument.");
    }

    if (hasReturnType) {
      type = returnType;
      return true;
    }

    lastArgType = Types.getSupertype(lastArgType, Types.getRawType(lastArgType), Callback.class);
    if (lastArgType instanceof ParameterizedType) {
      Type[] types = ((ParameterizedType) lastArgType).getActualTypeArguments();
      for (int i = 0; i < types.length; i++) {
        Type type = types[i];
        if (type instanceof WildcardType) {
          types[i] = ((WildcardType) type).getUpperBounds()[0];
        }
      }
      type = types[0];
      return false;
    }
    throw new IllegalArgumentException(
        String.format("Last parameter of %s must be of type Callback<X> or Callback<? super X>.",
            method));
  }

  /**
   * Loads {@link #namedParams}, {@link #singleEntityArgumentIndex},
   * {@link #baseUrlArgumentIndex}.
   * Must be called after {@link #parseMethodAnnotations()}}.
   */
  private void parseParameters() {
    Class<?>[] parameterTypes = method.getParameterTypes();
    Annotation[][] parameterAnnotationArrays = method.getParameterAnnotations();
    int count = parameterAnnotationArrays.length;
    if (!isSynchronous) {
      count -= 1; // Callback is last argument when not a synchronous method.
    }

    String[] namedParams = new String[count];
    for (int i = 0; i < count; i++) {
      Class<?> parameterType = parameterTypes[i];
      Annotation[] parameterAnnotations = parameterAnnotationArrays[i];
      if (parameterAnnotations == null || parameterAnnotations.length == 0) {
        throw new IllegalStateException("Argument " + i + " lacks annotation.");
      }
      for (Annotation parameterAnnotation : parameterAnnotations) {
        Class<? extends Annotation> annotationType = parameterAnnotation.annotationType();
        if (annotationType == Name.class) {
          String name = ((Name) parameterAnnotation).value();
          namedParams[i] = name;
          boolean isPathParam = pathParams.contains(name);
          if (parameterType == TypedOutput.class && (isPathParam || !restMethod.hasBody())) {
            throw new IllegalStateException("TypedOutput cannot be used as URL parameter.");
          }
          if (!isPathParam && !isMultipart && restMethod.hasBody()) {
            throw new IllegalStateException(
                "Non-path params can only be used in multipart request.");
          }
        } else if (annotationType == BaseUrl.class) {
            if (baseUrlArgumentIndex != NO_BASE_URL) {
                throw new IllegalStateException(
                        "Method annotated with multiple BaseEntity method annotations: " + method);
            }
            if (parameterType != String.class) {
                throw new IllegalStateException("BaseUrl should be of the Type String.");
            }
            baseUrlArgumentIndex = i;
        } else if (annotationType == SingleEntity.class) {
          if (isMultipart) {
            throw new IllegalStateException("SingleEntity cannot be used with multipart request.");
          }
          if (singleEntityArgumentIndex != NO_SINGLE_ENTITY) {
            throw new IllegalStateException(
                "Method annotated with multiple SingleEntity method annotations: " + method);
          }
          singleEntityArgumentIndex = i;
        } else {
          throw new IllegalStateException(
              "Argument " + i + " has invalid annotation " + annotationType + ": " + method);
        }
      }
    }
    // Check for single entity + non-path parameters.
    if (singleEntityArgumentIndex != NO_SINGLE_ENTITY) {
      for (String namedParam : namedParams) {
        if (namedParam != null && !pathParams.contains(namedParam)) {
          throw new IllegalStateException(
              "Single entity and non-path parameters cannot both be present.");
        }
      }
    }
    if (!restMethod.hasBody() && (isMultipart || singleEntityArgumentIndex != NO_SINGLE_ENTITY)) {
      throw new IllegalStateException(
          "Non-body HTTP method cannot contain @SingleEntity or @TypedOutput.");
    }
    this.namedParams = namedParams;
  }

  /**
   * Gets the set of unique path parameters used in the given URI. If a parameter is used twice
   * in the URI, it will only show up once in the set.
   */
  static Set<String> parsePathParameters(String path) {
    Matcher m = PATH_PARAMETERS.matcher(path);
    Set<String> patterns = new LinkedHashSet<String>();
    while (m.find()) {
      patterns.add(m.group(1));
    }
    return patterns;
  }
}
