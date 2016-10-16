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
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import retrofit2.Converter;
import retrofit2.ParameterHandler;
import retrofit2.RequestBuilder;
import retrofit2.Retrofit;
import retrofit2.http.Body;

/**
 * Creates a default {@link ParameterHandler} for parameters annotated with {@link Body @Body}.
 */
public class BodyParameterFactory implements ParameterHandler.Factory {

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof Body) {
      Converter<?, RequestBody> converter;
      try {
        converter = retrofit.requestBodyConverter(type, annotations, methodAnnotations);
      } catch (RuntimeException e) {
        // Wide exception range because factories are user code.
        throw new IllegalArgumentException(
            String.format("Unable to create @Body converter for %s", type), e);
      }
      return new BodyParameter<>(converter);
    }
    return null;
  }

  static final class BodyParameter<T> implements ParameterHandler<T> {
    private final Converter<T, RequestBody> converter;

    BodyParameter(Converter<T, RequestBody> converter) {
      this.converter = converter;
    }

    @Override
    public void apply(RequestBuilder builder, T value) {
      if (value == null) {
        throw new IllegalArgumentException("Body parameter value must not be null.");
      }
      RequestBody body;
      try {
        body = converter.convert(value);
      } catch (IOException e) {
        throw new RuntimeException("Unable to convert " + value + " to RequestBody", e);
      }
      builder.setBody(body);
    }
  }
}
