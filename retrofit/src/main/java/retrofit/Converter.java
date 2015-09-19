/*
 * Copyright (C) 2012 Square, Inc.
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

/**
 * Convert objects to and from their representation as HTTP bodies. Register a converter with
 * Retrofit using {@link Retrofit.Builder#addConverterFactory(Factory)}.
 */
public interface Converter<F, T> {
  T convert(F value) throws IOException;

  abstract class Factory {
    /**
     * Create a {@link Converter} for converting an HTTP response body to {@code type} or null if it
     * cannot be handled by this factory.
     */
    public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
      return null;
    }

    /**
     * Create a {@link Converter} for converting {@code type} to an HTTP request body or null if it
     * cannot be handled by this factory.
     */
    public Converter<?, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
      return null;
    }
  }
}
