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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.ScalarResponseBodyConverters.BooleanResponseBodyConverter;
import retrofit2.ScalarResponseBodyConverters.ByteResponseBodyConverter;
import retrofit2.ScalarResponseBodyConverters.CharacterResponseBodyConverter;
import retrofit2.ScalarResponseBodyConverters.DoubleResponseBodyConverter;
import retrofit2.ScalarResponseBodyConverters.FloatResponseBodyConverter;
import retrofit2.ScalarResponseBodyConverters.IntegerResponseBodyConverter;
import retrofit2.ScalarResponseBodyConverters.LongResponseBodyConverter;
import retrofit2.ScalarResponseBodyConverters.ShortResponseBodyConverter;
import retrofit2.ScalarResponseBodyConverters.StringResponseBodyConverter;

/**
 * A {@linkplain Converter.Factory converter} for strings and both primitives and their boxed types
 * to {@code text/plain} bodies.
 */
public final class ScalarsConverterFactory extends Converter.Factory {
  public static ScalarsConverterFactory create() {
    return new ScalarsConverterFactory();
  }

  private ScalarsConverterFactory() {
  }

  @Override
  public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] annotations,
      Retrofit retrofit) {
    if (type == String.class
        || type == boolean.class
        || type == Boolean.class
        || type == byte.class
        || type == Byte.class
        || type == char.class
        || type == Character.class
        || type == double.class
        || type == Double.class
        || type == float.class
        || type == Float.class
        || type == int.class
        || type == Integer.class
        || type == long.class
        || type == Long.class
        || type == short.class
        || type == Short.class) {
      return ScalarRequestBodyConverter.INSTANCE;
    }
    return null;
  }

  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
      Retrofit retrofit) {
    if (type == String.class) {
      return StringResponseBodyConverter.INSTANCE;
    }
    if (type == Boolean.class) {
      return BooleanResponseBodyConverter.INSTANCE;
    }
    if (type == Byte.class) {
      return ByteResponseBodyConverter.INSTANCE;
    }
    if (type == Character.class) {
      return CharacterResponseBodyConverter.INSTANCE;
    }
    if (type == Double.class) {
      return DoubleResponseBodyConverter.INSTANCE;
    }
    if (type == Float.class) {
      return FloatResponseBodyConverter.INSTANCE;
    }
    if (type == Integer.class) {
      return IntegerResponseBodyConverter.INSTANCE;
    }
    if (type == Long.class) {
      return LongResponseBodyConverter.INSTANCE;
    }
    if (type == Short.class) {
      return ShortResponseBodyConverter.INSTANCE;
    }
    return null;
  }
}
