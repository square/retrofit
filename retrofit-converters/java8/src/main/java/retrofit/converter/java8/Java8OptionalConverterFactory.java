/*
 * Copyright (C) 2017 Square, Inc.
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
package retrofit.converter.java8;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import javax.annotation.Nullable;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * @deprecated Retrofit includes support for Optional. This no longer needs to be added to the
 *     Retrofit instance explicitly.
 *     <p>A {@linkplain Converter.Factory converter} for {@code Optional<T>} which delegates to
 *     another converter to deserialize {@code T} and then wraps it into {@link Optional}.
 */
@Deprecated
public final class Java8OptionalConverterFactory extends Converter.Factory {
  public static Java8OptionalConverterFactory create() {
    return new Java8OptionalConverterFactory();
  }

  private Java8OptionalConverterFactory() {}

  @Override
  public @Nullable Converter<ResponseBody, ?> responseBodyConverter(
      Type type, Annotation[] annotations, Retrofit retrofit) {
    if (getRawType(type) != Optional.class) {
      return null;
    }

    Type innerType = getParameterUpperBound(0, (ParameterizedType) type);
    Converter<ResponseBody, Object> delegate =
        retrofit.responseBodyConverter(innerType, annotations);
    return new OptionalConverter<>(delegate);
  }
}
