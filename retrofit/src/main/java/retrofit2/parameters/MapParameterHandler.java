/*
 * Copyright (C) 2016 Square, Inc.
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
package retrofit2.parameters;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import retrofit2.ParameterHandler;
import retrofit2.RequestBuilder;
import retrofit2.Utils;

/**
 * {@link ParameterHandler} passing each key-value pair from the {@code Map} argument to
 * {@link NamedParameterHandler}. Used to handle {@link retrofit2.http.HeaderMap @HeaderMap},
 * {@link retrofit2.http.PartMap @PartMap} etc. The supplied {@code Map} argument
 * may not be {@code null} and can't contain {@code null} keys of values.
 */
public class MapParameterHandler<T> implements ParameterHandler<Map<String, T>> {

  public MapParameterHandler(NamedValuesHandler<T> handler, String handlerName) {
    this.handler = handler;
    this.handlerName = handlerName;
  }

  public static Type getValueType(Type type, Annotation annotation) {
    String annotationName = annotation.annotationType().getSimpleName();
    Class<?> rawParameterType = Utils.getRawType(type);
    if (!Map.class.isAssignableFrom(rawParameterType)) {
      throw  new IllegalArgumentException(String.format("@%1$s parameter type must be Map.",
          annotationName));
    }
    Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
    if (!(mapType instanceof ParameterizedType)) {
      throw new IllegalArgumentException(
          "Map must include generic types (e.g., Map<String, String>)");
    }
    ParameterizedType parameterizedType = (ParameterizedType) mapType;
    Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
    if (String.class != keyType) {
      throw  new IllegalArgumentException(String.format("@%1$s keys must be of type String: %2$s",
          annotationName, keyType));
    }
    return Utils.getParameterUpperBound(1, parameterizedType);
  }

  private final String handlerName;
  private final NamedValuesHandler<T> handler;

  @Override
  public void apply(RequestBuilder builder, Map<String, T> value) throws IOException {
    if (value == null) {
      throw new IllegalArgumentException(handlerName + " map was null.");
    }

    for (Map.Entry<String, T> entry : value.entrySet()) {
      String entryKey = entry.getKey();
      if (entryKey == null) {
        throw new IllegalArgumentException(handlerName + " map contained null key.");
      }
      T entryValue = entry.getValue();
      if (entryValue == null) {
        throw new IllegalArgumentException(
            handlerName + " map contained null value for key '" + entryKey + "'.");
      }
      handler.apply(builder, entryKey, entryValue);
    }
  }
}
