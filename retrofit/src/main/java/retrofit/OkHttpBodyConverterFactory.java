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

final class OkHttpBodyConverterFactory implements Converter.Factory {
  @Override public Converter<?> get(Type type, Annotation[] annotations) {
    if (!(type instanceof Class)) {
      return null;
    }
    Class<?> cls = (Class<?>) type;
    if (RequestBody.class.isAssignableFrom(cls)) {
      return new OkHttpRequestBodyConverter();
    }
    if (cls == ResponseBody.class) {
      boolean streaming = Utils.isAnnotationPresent(annotations, Streaming.class);
      return new OkHttpResponseBodyConverter(streaming);
    }
    return null;
  }
}
