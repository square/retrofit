package retrofit2;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

public class ErrorsUtils {
  static RuntimeException methodError(Method method, String message, Object... args) {
    return methodError(method, null, message, args);
  }

  @SuppressWarnings("AnnotateFormatMethod")
  static RuntimeException methodError(
    Method method, @Nullable Throwable cause, String message, Object... args) {
    message = String.format(message, args);
    return new IllegalArgumentException(
      message
        + "\n    for method "
        + method.getDeclaringClass().getSimpleName()
        + "."
        + method.getName(),
      cause);
  }

  static RuntimeException parameterError(
    Method method, Throwable cause, int p, String message, Object... args) {
    return methodError(method, cause, message + " (parameter #" + (p + 1) + ")", args);
  }

  static RuntimeException parameterError(Method method, int p, String message, Object... args) {
    return methodError(method, message + " (parameter #" + (p + 1) + ")", args);
  }
}
