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

import android.net.Uri;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.Map;
import okhttp3.Headers;
import okhttp3.RequestBody;

import static retrofit2.Utils.checkNotNull;

abstract class RequestAction<T> {
  abstract void perform(RequestBuilder builder, T value) throws IOException;

  final RequestAction<Iterable<T>> iterable() {
    return new RequestAction<Iterable<T>>() {
      @Override void perform(RequestBuilder builder, Iterable<T> values) throws IOException {
        if (values == null) return; // Skip null values.

        for (T value : values) {
          RequestAction.this.perform(builder, value);
        }
      }
    };
  }

  final RequestAction<Object> array() {
    return new RequestAction<Object>() {
      @Override void perform(RequestBuilder builder, Object values) throws IOException {
        if (values == null) return; // Skip null values.

        for (int i = 0, size = Array.getLength(values); i < size; i++) {
          //noinspection unchecked
          RequestAction.this.perform(builder, (T) Array.get(values, i));
        }
      }
    };
  }

  static final class StringUrl extends RequestAction<String> {
    @Override void perform(RequestBuilder builder, String value) {
      builder.setRelativeUrl(value);
    }
  }

  static final class JavaUriUrl extends RequestAction<URI> {
    @Override void perform(RequestBuilder builder, URI value) {
      builder.setRelativeUrl(value.toString());
    }
  }

  static final class AndroidUriUrl extends RequestAction<Uri> {
    @Override void perform(RequestBuilder builder, Uri value) {
      builder.setRelativeUrl(value.toString());
    }
  }

  static final class Header<T> extends RequestAction<T> {
    private final String name;
    private final Converter<T, String> valueConverter;

    Header(String name, Converter<T, String> valueConverter) {
      this.name = checkNotNull(name, "name == null");
      this.valueConverter = valueConverter;
    }

    @Override void perform(RequestBuilder builder, T value) throws IOException {
      if (value == null) return; // Skip null values.
      builder.addHeader(name, valueConverter.convert(value));
    }
  }

  static final class Path<T> extends RequestAction<T> {
    private final String name;
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    Path(String name, Converter<T, String> valueConverter, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void perform(RequestBuilder builder, T value) throws IOException {
      if (value == null) {
        throw new IllegalArgumentException(
            "Path parameter \"" + name + "\" value must not be null.");
      }
      builder.addPathParam(name, valueConverter.convert(value), encoded);
    }
  }

  static final class Query<T> extends RequestAction<T> {
    private final String name;
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    Query(String name, Converter<T, String> valueConverter, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void perform(RequestBuilder builder, T value) throws IOException {
      if (value == null) return; // Skip null values.
      builder.addQueryParam(name, valueConverter.convert(value), encoded);
    }
  }

  static final class QueryMap<T> extends RequestAction<Map<String, T>> {
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    QueryMap(Converter<T, String> valueConverter, boolean encoded) {
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void perform(RequestBuilder builder, Map<String, T> value) throws IOException {
      if (value == null) return; // Skip null values.

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Query map contained null key.");
        }
        T entryValue = entry.getValue();
        if (entryValue != null) { // Skip null values.
          builder.addQueryParam(entryKey, valueConverter.convert(entryValue), encoded);
        }
      }
    }
  }

  static final class Field<T> extends RequestAction<T> {
    private final String name;
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    Field(String name, Converter<T, String> valueConverter, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void perform(RequestBuilder builder, T value) throws IOException {
      if (value == null) return; // Skip null values.
      builder.addFormField(name, valueConverter.convert(value), encoded);
    }
  }

  static final class FieldMap<T> extends RequestAction<Map<String, T>> {
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    FieldMap(Converter<T, String> valueConverter, boolean encoded) {
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void perform(RequestBuilder builder, Map<String, T> value) throws IOException {
      if (value == null) return; // Skip null values.

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Field map contained null key.");
        }
        T entryValue = entry.getValue();
        if (entryValue != null) { // Skip null values.
          builder.addFormField(entryKey, valueConverter.convert(entryValue), encoded);
        }
      }
    }
  }

  static final class Part<T> extends RequestAction<T> {
    private final boolean ignoreNull;
    private final Headers headers;
    private final Converter<T, RequestBody> converter;

    Part(boolean ignoreNull, Headers headers, Converter<T, RequestBody> converter) {
      this.ignoreNull = ignoreNull;
      this.headers = headers;
      this.converter = converter;
    }

    @Override void perform(RequestBuilder builder, T value) {
      if (ignoreNull && value == null) return; // Skip null values.

      RequestBody body;
      try {
        body = converter.convert(value);
      } catch (IOException e) {
        throw new RuntimeException("Unable to convert " + value + " to RequestBody", e);
      }
      builder.addPart(headers, body);
    }
  }

  static final class PartMap<T> extends RequestAction<Map<String, T>> {
    private final boolean ignoreNull;
    private final Converter<T, RequestBody> valueConverter;
    private final String transferEncoding;

    PartMap(boolean ignoreNull, Converter<T, RequestBody> valueConverter, String transferEncoding) {
      this.ignoreNull = ignoreNull;
      this.valueConverter = valueConverter;
      this.transferEncoding = transferEncoding;
    }

    @Override void perform(RequestBuilder builder, Map<String, T> value) throws IOException {
      if (value == null) {
        throw new IllegalArgumentException("Part map must not be null.");
      }

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Part map contained null key.");
        }
        T entryValue = entry.getValue();
        if (ignoreNull && entryValue == null) {
          continue; // Skip null values.
        }

        Headers headers = Headers.of(
            "Content-Disposition", "form-data; name=\"" + entryKey + "\"",
            "Content-Transfer-Encoding", transferEncoding);

        builder.addPart(headers, valueConverter.convert(entryValue));
      }
    }
  }

  static final class Body<T> extends RequestAction<T> {
    private final boolean ignoreNull;
    private final Converter<T, RequestBody> converter;

    Body(boolean ignoreNull, Converter<T, RequestBody> converter) {
      this.ignoreNull = ignoreNull;
      this.converter = converter;
    }

    @Override void perform(RequestBuilder builder, T value) {
      RequestBody body;
      if (ignoreNull && value == null) {
        body = Utils.EMPTY_BODY;
      } else {
        try {
          body = converter.convert(value);
        } catch (IOException e) {
          throw new RuntimeException("Unable to convert " + value + " to RequestBody", e);
        }
      }
      builder.setBody(body);
    }
  }
}
