package retrofit.http;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Contains the desired HttpMethodType and relative path specified by a
 * service method.  See also the factory method {@link #fromMethod(Method)}.
 * @author Patrick Forhan (patrick@squareup.com)
 */
final class RequestLine {
  private final String relativePath;
  private final HttpMethodType httpMethod;

  private RequestLine(HttpMethodType methodType,
      Annotation methodAnnotation) {
    relativePath = getValue(methodAnnotation);
    httpMethod = methodType;
  }

  String getRelativePath() {
    return relativePath;
  }

  HttpMethodType getHttpMethod() {
    return httpMethod;
  }

  /** Using reflection, get the value field of the specified annotation. */
  private static String getValue(Annotation annotation) {
    try {
      final Method valueMethod = annotation.annotationType()
          .getMethod("value");
      return (String) valueMethod.invoke(annotation);

    } catch (Exception ex) {
      throw new IllegalStateException("Failed to extract URI path", ex);
    }
  }

  /**
   * Looks for exactly one annotation of type {@link DELETE}, {@link GET},
   * {@link POST}, or {@link PUT} and extracts its path data.  Throws an
   * {@link IllegalStateException} if none or multiple are found.
   */
  static RequestLine fromMethod(Method method) {
    Annotation[] annotations = method.getAnnotations();
    RequestLine found = null;
    for (Annotation annotation : annotations) {
      // look for an HttpMethod annotation describing the type:
      final retrofit.http.HttpMethod typeAnnotation = annotation.annotationType()
          .getAnnotation(retrofit.http.HttpMethod.class);
      if (typeAnnotation != null) {
        if (found != null) {
          throw new IllegalStateException(
              "Method annotated with multiple HTTP method annotations: "
                + method.toString());
        }
        found = new RequestLine(typeAnnotation.value(), annotation);
      }
    }

    if (found == null) {
      throw new IllegalStateException(
          "Method not annotated with GET, POST, PUT, or DELETE: "
            + method.toString());
    }
    return found;
  }
}