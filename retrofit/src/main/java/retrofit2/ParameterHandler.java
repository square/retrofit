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
package retrofit2;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Map;
import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import static retrofit2.Utils.checkNotNull;

abstract class ParameterHandler<T> {
  abstract void apply(RequestBuilder builder, T value) throws IOException;

  final ParameterHandler<Iterable<T>> iterable() {
    return new ParameterHandler<Iterable<T>>() {
      @Override void apply(RequestBuilder builder, Iterable<T> values) throws IOException {
        if (values == null) return; // Skip null values.

        for (T value : values) {
          ParameterHandler.this.apply(builder, value);
        }
      }
    };
  }

  final ParameterHandler<Object> array() {
    return new ParameterHandler<Object>() {
      @Override void apply(RequestBuilder builder, Object values) throws IOException {
        if (values == null) return; // Skip null values.

        for (int i = 0, size = Array.getLength(values); i < size; i++) {
          //noinspection unchecked
          ParameterHandler.this.apply(builder, (T) Array.get(values, i));
        }
      }
    };
  }

  abstract static class NamedValuesHandler<T> {
    abstract void apply(RequestBuilder builder, String name, T value) throws IOException;
  }

  static class NamedParameterHandler<T> extends ParameterHandler<T> {
    private final String name;
    private final NamedValuesHandler<T> handler;

    public NamedParameterHandler(String name, NamedValuesHandler<T> handler) {
      this.name = name;
      this.handler = handler;
    }

    @Override
    public void apply(RequestBuilder builder, T value) throws IOException {
      handler.apply(builder, name, value);
    }
  }

  static class MapParameterHandler<T> extends ParameterHandler<Map<String, T>> {

    private final String handlerName;
    private final NamedValuesHandler<T> handler;

    public MapParameterHandler(NamedValuesHandler<T> handler, String handlerName) {
      this.handler = handler;
      this.handlerName = handlerName;
    }

    @Override
    public void apply(RequestBuilder builder, Map<String, T> value) throws IOException {
      if (value == null) {
        throw new IllegalArgumentException(handlerName + " map was null.");
      }

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException(handlerName + " map contained null key.");
        }
        T entryValue = entry.getValue();
        if (entryValue == null) {
          throw new IllegalArgumentException(
              handlerName + " map contained null value for key '" + entryKey + "'.");
        }
        handler.apply(builder, entryKey, entryValue);
      }
    }
  }

  static final class RelativeUrl extends ParameterHandler<Object> {
    @Override void apply(RequestBuilder builder, Object value) {
      builder.setRelativeUrl(value);
    }
  }

  static final class Header<T> extends NamedValuesHandler<T> {
    private final Converter<T, String> valueConverter;

    Header(Converter<T, String> valueConverter) {
      this.valueConverter = valueConverter;
    }

    @Override void apply(RequestBuilder builder, String name, T value) throws IOException {
      if (value == null) return; // Skip null values.
      builder.addHeader(name, valueConverter.convert(value));
    }
  }

  static final class Path<T> extends ParameterHandler<T> {
    private final String name;
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    Path(String name, Converter<T, String> valueConverter, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void apply(RequestBuilder builder, T value) throws IOException {
      if (value == null) {
        throw new IllegalArgumentException(
            "Path parameter \"" + name + "\" value must not be null.");
      }
      builder.addPathParam(name, valueConverter.convert(value), encoded);
    }
  }

  static final class Query<T> extends NamedValuesHandler<T> {
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    Query(Converter<T, String> valueConverter, boolean encoded) {
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void apply(RequestBuilder builder, String name, T value) throws IOException {
      if (value == null) return; // Skip null values.
      builder.addQueryParam(name, valueConverter.convert(value), encoded);
    }
  }

  static final class Field<T> extends NamedValuesHandler<T> {
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    Field(Converter<T, String> valueConverter, boolean encoded) {
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void apply(RequestBuilder builder, String name, T value) throws IOException {
      if (value == null) return; // Skip null values.
      builder.addFormField(name, valueConverter.convert(value), encoded);
    }
  }

  static final class Part<T> extends NamedValuesHandler<T> {
    private final String transferEncoding;
    private final Converter<T, RequestBody> converter;

    Part(String transferEncoding, Converter<T, RequestBody> converter) {
      this.transferEncoding = transferEncoding;
      this.converter = converter;
    }

    @Override void apply(RequestBuilder builder, String name, T value) {
      if (value == null) return; // Skip null values.

      RequestBody body;
      try {
        body = converter.convert(value);
      } catch (IOException e) {
        throw new RuntimeException("Unable to convert " + value + " to RequestBody", e);
      }
      Headers headers = Headers.of(
          "Content-Disposition", "form-data; name=\"" + name + "\"",
          "Content-Transfer-Encoding", transferEncoding);
      builder.addPart(headers, body);
    }
  }

  static final class RawPart extends ParameterHandler<MultipartBody.Part> {
    @Override void apply(RequestBuilder builder, MultipartBody.Part value) throws IOException {
      if (value != null) { // Skip null values.
        builder.addPart(value);
      }
    }
  }

  static final class Body<T> extends ParameterHandler<T> {
    private final Converter<T, RequestBody> converter;

    Body(Converter<T, RequestBody> converter) {
      this.converter = converter;
    }

    @Override void apply(RequestBuilder builder, T value) {
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
