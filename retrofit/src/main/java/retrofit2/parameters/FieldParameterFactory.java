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
import retrofit2.http.Field;
import retrofit2.http.FieldMap;

public class FieldParameterFactory implements ParameterHandler.Factory {

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof Field) {
      Field field = (Field) annotation;
      String name = field.value();
      boolean encoded = field.encoded();

      Type itemType = RepeatedParameterHelper.getItemType(type);
      Converter<?, String> converter = retrofit.stringConverter(itemType, annotations);
      return RepeatedParameterHelper.wrapIfRepeated(type,
          new NamedParameterHandler<>(name, new FieldHandler<>(converter, encoded)));
    } else if (annotation instanceof FieldMap) {
      FieldMap fieldMap = (FieldMap) annotation;
      Converter<?, String> converter =
          retrofit.stringConverter(MapParameterHandler.getValueType(type, annotation), annotations);
      return new MapParameterHandler<>(new FieldHandler<>(converter, fieldMap.encoded()), "Field");
    }
    return null;
  }

  static final class FieldHandler<T> implements NamedValuesHandler<T> {
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    FieldHandler(Converter<T, String> valueConverter, boolean encoded) {
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override
    public void apply(RequestBuilder builder, String name, T value) throws IOException {
      if (value == null) return; // Skip null values.
      builder.addFormField(name, valueConverter.convert(value), encoded);
    }
  }
}
