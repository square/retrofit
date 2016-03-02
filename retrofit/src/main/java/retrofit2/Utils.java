/*
 * Copyright (C) 2012 Square, Inc.
 * Copyright (C) 2007 The Guava Authors
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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;

final class Utils {
  static final RequestBody EMPTY_BODY = RequestBody.create(null, new byte[0]);

  static <T> T checkNotNull(T object, String message) {
    if (object == null) {
      throw new NullPointerException(message);
    }
    return object;
  }

  /** Returns true if {@code annotations} contains an instance of {@code cls}. */
  static boolean isAnnotationPresent(Annotation[] annotations,
      Class<? extends Annotation> cls) {
    for (Annotation annotation : annotations) {
      if (cls.isInstance(annotation)) {
        return true;
      }
    }
    return false;
  }

  static ResponseBody buffer(final ResponseBody body) throws IOException {
    Buffer buffer = new Buffer();
    body.source().readAll(buffer);
    return ResponseBody.create(body.contentType(), body.contentLength(), buffer);
  }

  static <T> void validateServiceInterface(Class<T> service) {
    if (!service.isInterface()) {
      throw new IllegalArgumentException("API declarations must be interfaces.");
    }
    // Prevent API interfaces from extending other interfaces. This not only avoids a bug in
    // Android (http://b.android.com/58753) but it forces composition of API declarations which is
    // the recommended pattern.
    if (service.getInterfaces().length > 0) {
      throw new IllegalArgumentException("API interfaces must not extend other interfaces.");
    }
  }

  static Type getParameterUpperBound(int index, ParameterizedType type) {
    Type[] types = type.getActualTypeArguments();
    if (types.length <= index) {
      throw new IllegalArgumentException(
          "Expected at least " + index + " type argument(s) but got: " + Arrays.toString(types));
    }
    Type paramType = types[index];
    if (paramType instanceof WildcardType) {
      return ((WildcardType) paramType).getUpperBounds()[0];
    }
    return paramType;
  }

  static boolean hasUnresolvableType(Type type) {
    if (type instanceof Class<?>) {
      return false;
    }
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
        if (hasUnresolvableType(typeArgument)) {
          return true;
        }
      }
      return false;
    }
    if (type instanceof GenericArrayType) {
      return hasUnresolvableType(((GenericArrayType) type).getGenericComponentType());
    }
    if (type instanceof TypeVariable) {
      return true;
    }
    if (type instanceof WildcardType) {
      return true;
    }
    String className = type == null ? "null" : type.getClass().getName();
    throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
        + "GenericArrayType, but <" + type + "> is of type " + className);
  }

  static RuntimeException methodError(Method method, String message, Object... args) {
    return methodError(null, method, message, args);
  }

  static RuntimeException methodError(Throwable cause, Method method, String message,
      Object... args) {
    message = String.format(message, args);
    return new IllegalArgumentException(message
        + "\n    for method "
        + method.getDeclaringClass().getSimpleName()
        + "."
        + method.getName(), cause);
  }

  static Type getCallResponseType(Type returnType) {
    if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalArgumentException(
          "Call return type must be parameterized as Call<Foo> or Call<? extends Foo>");
    }
    return getParameterUpperBound(0, (ParameterizedType) returnType);
  }

  private Utils() {
    // No instances.
  }
}
