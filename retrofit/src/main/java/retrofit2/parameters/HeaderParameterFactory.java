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

import retrofit2.Converter;
import retrofit2.ParameterHandler;
import retrofit2.RequestBuilder;
import retrofit2.Retrofit;
import retrofit2.http.Header;
import retrofit2.http.HeaderMap;

/**
 * Creates a default {@link ParameterHandler} for parameters annotated with {@link Header @Header}
 * and {@link HeaderMap @HeaderMap}.
 */
public class HeaderParameterFactory implements ParameterHandler.Factory {

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof Header) {
      Header header = (Header) annotation;
      String name = header.value();

      Type itemType = RepeatedParameterHelper.getItemType(type);
      Converter<?, String> converter = retrofit.stringConverter(itemType, annotations);
      return RepeatedParameterHelper.wrapIfRepeated(type,
          new NamedParameterHandler<>(name, new HeaderHandler<>(converter)));
    } else if (annotation instanceof HeaderMap) {
      Converter<?, String> converter =
          retrofit.stringConverter(MapParameterHandler.getValueType(type, annotation), annotations);
      return new MapParameterHandler<>(new HeaderHandler<>(converter), "Header");
    }
    return null;
  }

  static final class HeaderHandler<T> implements NamedValuesHandler<T> {
    private final Converter<T, String> valueConverter;

    HeaderHandler(Converter<T, String> valueConverter) {
      this.valueConverter = valueConverter;
    }

    @Override
    public void apply(RequestBuilder builder, String name, T value) throws IOException {
      if (value == null) return; // Skip null values.
      builder.addHeader(name, valueConverter.convert(value));
    }
  }

}
