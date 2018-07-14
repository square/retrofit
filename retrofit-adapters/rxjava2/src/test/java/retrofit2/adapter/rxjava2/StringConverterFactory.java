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
package retrofit2.adapter.rxjava2;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

final class StringConverterFactory extends Converter.Factory {
  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
      Retrofit retrofit) {
    return new Converter<ResponseBody, String>() {
      @Override public String convert(ResponseBody value) throws IOException {
        return value.string();
      }
    };
  }

  @Override public Converter<?, RequestBody> requestBodyConverter(Type type,
      Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
    return new Converter<String, RequestBody>() {
      @Override public RequestBody convert(String value) throws IOException {
        return RequestBody.create(MediaType.get("text/plain"), value);
      }
    };
  }
}
