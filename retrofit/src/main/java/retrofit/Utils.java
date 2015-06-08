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
package retrofit;

import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.concurrent.Executor;
import okio.Buffer;
import okio.BufferedSource;
import okio.Source;

final class Utils {
  static <T> T checkNotNull(T object, String message) {
    if (object == null) {
      throw new NullPointerException(message);
    }
    return object;
  }

  /**
   * Replace a {@link Response} with an identical copy whose body is backed by a
   * {@link Buffer} rather than a {@link Source}.
   */
  static ResponseBody readBodyToBytesIfNecessary(final ResponseBody body) throws IOException {
    if (body == null) {
      return null;
    }

    BufferedSource source = body.source();
    final Buffer buffer = new Buffer();
    buffer.writeAll(source);
    source.close();

    return ResponseBody.create(body.contentType(), body.contentLength(), buffer);
  }

  static <T> void validateServiceClass(Class<T> service) {
    if (!service.isInterface()) {
      throw new IllegalArgumentException("Only interface endpoint definitions are supported.");
    }
    // Prevent API interfaces from extending other interfaces. This not only avoids a bug in
    // Android (http://b.android.com/58753) but it forces composition of API declarations which is
    // the recommended pattern.
    if (service.getInterfaces().length > 0) {
      throw new IllegalArgumentException("Interface definitions must not extend other interfaces.");
    }
  }

  public static Type getSingleParameterUpperBound(ParameterizedType type) {
    Type[] types = type.getActualTypeArguments();
    if (types.length != 1) {
      throw new IllegalArgumentException(
          "Expected one type argument but got: " + Arrays.toString(types));
    }
    Type paramType = types[0];
    if (paramType instanceof WildcardType) {
      return ((WildcardType) paramType).getUpperBounds()[0];
    }
    return paramType;
  }

  public static boolean hasUnresolvableType(Type type) {
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

  // This method is copyright 2008 Google Inc. and is taken from Gson under the Apache 2.0 license.
  public static Class<?> getRawType(Type type) {
    if (type instanceof Class<?>) {
      // Type is a normal class.
      return (Class<?>) type;

    } else if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;

      // I'm not exactly sure why getRawType() returns Type instead of Class. Neal isn't either but
      // suspects some pathological case related to nested classes exists.
      Type rawType = parameterizedType.getRawType();
      if (!(rawType instanceof Class)) throw new IllegalArgumentException();
      return (Class<?>) rawType;

    } else if (type instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) type).getGenericComponentType();
      return Array.newInstance(getRawType(componentType), 0).getClass();

    } else if (type instanceof TypeVariable) {
      // We could use the variable's bounds, but that won't work if there are multiple. Having a raw
      // type that's more general than necessary is okay.
      return Object.class;

    } else if (type instanceof WildcardType) {
      return getRawType(((WildcardType) type).getUpperBounds()[0]);

    } else {
      String className = type == null ? "null" : type.getClass().getName();
      throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
          + "GenericArrayType, but <" + type + "> is of type " + className);
    }
  }

  static class SynchronousExecutor implements Executor {
    @Override public void execute(Runnable runnable) {
      runnable.run();
    }
  }

  private Utils() {
    // No instances.
  }
}
