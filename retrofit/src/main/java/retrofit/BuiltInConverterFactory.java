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
import retrofit.http.Streaming;

final class BuiltInConverterFactory extends Converter.Factory {
  @Override
  public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
    if (ResponseBody.class.equals(type)) {
      boolean isStreaming = Utils.isAnnotationPresent(annotations, Streaming.class);
      return new OkHttpResponseBodyConverter(isStreaming);
    }
    if (Void.class.equals(type)) {
      return new VoidConverter();
    }
    return null;
  }

  @Override public Converter<?, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
    if (type instanceof Class && RequestBody.class.isAssignableFrom((Class<?>) type)) {
      return new OkHttpRequestBodyConverter();
    }
    return null;
  }
}
