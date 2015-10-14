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
package retrofit;

import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * A {@linkplain Converter.Factory converter} which uses fastjson for JSON
 */
public class FastjsonConverterFactory extends Converter.Factory {

  public static FastjsonConverterFactory create() {
    return new FastjsonConverterFactory();
  }

  @Override
  public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
    if (!(type instanceof Class)) {
      return null;
    }
    return new FastjsonResponseConverter<>((Class<?>) type);
  }

  @Override
  public Converter<?, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
    return new FastjsonRequestBodyConverter<>();
  }
}
