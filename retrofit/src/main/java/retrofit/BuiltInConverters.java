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
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import retrofit.http.Streaming;

import static retrofit.Utils.closeQuietly;

final class BuiltInConverters extends Converter.Factory {
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

  static final class VoidConverter implements Converter<ResponseBody, Void> {
    @Override public Void convert(ResponseBody value) throws IOException {
      value.close();
      return null;
    }
  }

  static final class OkHttpRequestBodyConverter implements Converter<RequestBody, RequestBody> {
    @Override public RequestBody convert(RequestBody value) throws IOException {
      return value;
    }
  }

  static final class OkHttpResponseBodyConverter implements Converter<ResponseBody, ResponseBody> {
    private final boolean isStreaming;

    OkHttpResponseBodyConverter(boolean isStreaming) {
      this.isStreaming = isStreaming;
    }

    @Override public ResponseBody convert(ResponseBody value) throws IOException {
      if (isStreaming) {
        return value;
      }

      // Buffer the entire body to avoid future I/O.
      try {
        return Utils.readBodyToBytesIfNecessary(value);
      } finally {
        closeQuietly(value);
      }
    }
  }
}
