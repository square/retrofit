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

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.RequestBody;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Map;

import static retrofit.Utils.checkNotNull;

abstract class RequestBuilderAction {
  abstract void perform(RestAdapterRawRequestBuilder builder, Object value);

  static final class Url extends RequestBuilderAction {
    @Override void perform(RestAdapterRawRequestBuilder builder, Object value) {
      builder.setRelativeUrl((String) value);
    }
  }

  static final class Header extends RequestBuilderAction {
    private final String name;

    Header(String name) {
      this.name = checkNotNull(name, "name == null");
    }

    @Override void perform(RestAdapterRawRequestBuilder builder, Object value) {
      if (value == null) return; // Skip null values.

      if (value instanceof Iterable) {
        for (Object iterableValue : (Iterable<?>) value) {
          if (iterableValue != null) { // Skip null values.
            builder.addHeader(name, iterableValue.toString());
          }
        }
      } else if (value.getClass().isArray()) {
        for (int x = 0, arrayLength = Array.getLength(value); x < arrayLength; x++) {
          Object arrayValue = Array.get(value, x);
          if (arrayValue != null) { // Skip null values.
            builder.addHeader(name, arrayValue.toString());
          }
        }
      } else {
        builder.addHeader(name, value.toString());
      }
    }
  }

  static final class Path extends RequestBuilderAction {
    private final String name;
    private final boolean encoded;

    Path(String name, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.encoded = encoded;
    }

    @Override void perform(RestAdapterRawRequestBuilder builder, Object value) {
      if (value == null) {
        throw new IllegalArgumentException(
            "Path parameter \"" + name + "\" value must not be null.");
      }
      builder.addPathParam(name, value.toString(), encoded);
    }
  }

  static final class Query extends RequestBuilderAction {
    private final String name;
    private final boolean encoded;

    Query(String name, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.encoded = encoded;
    }

    @Override void perform(RestAdapterRawRequestBuilder builder, Object value) {
      if (value == null) return; // Skip null values.

      if (value instanceof Iterable) {
        for (Object iterableValue : (Iterable<?>) value) {
          if (iterableValue != null) { // Skip null values.
            builder.addQueryParam(name, iterableValue.toString(), encoded);
          }
        }
      } else if (value.getClass().isArray()) {
        for (int x = 0, arrayLength = Array.getLength(value); x < arrayLength; x++) {
          Object arrayValue = Array.get(value, x);
          if (arrayValue != null) { // Skip null values.
            builder.addQueryParam(name, arrayValue.toString(), encoded);
          }
        }
      } else {
        builder.addQueryParam(name, value.toString(), encoded);
      }
    }
  }

  static final class QueryMap extends RequestBuilderAction {
    private final boolean encoded;

    QueryMap(boolean encoded) {
      this.encoded = encoded;
    }

    @Override void perform(RestAdapterRawRequestBuilder builder, Object value) {
      if (value == null) return; // Skip null values.

      Map<?, ?> map = (Map<?, ?>) value;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        Object entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Query map contained null key.");
        }
        Object entryValue = entry.getValue();
        if (entryValue != null) { // Skip null values.
          builder.addQueryParam(entryKey.toString(), entryValue.toString(), encoded);
        }
      }
    }
  }

  static final class Field extends RequestBuilderAction {
    private final String name;
    private final boolean encoded;

    Field(String name, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.encoded = encoded;
    }

    @Override void perform(RestAdapterRawRequestBuilder builder, Object value) {
      if (value == null) return; // Skip null values.

      if (value instanceof Iterable) {
        for (Object iterableValue : (Iterable<?>) value) {
          if (iterableValue != null) { // Skip null values.
            builder.addFormField(name, iterableValue.toString(), encoded);
          }
        }
      } else if (value.getClass().isArray()) {
        for (int x = 0, arrayLength = Array.getLength(value); x < arrayLength; x++) {
          Object arrayValue = Array.get(value, x);
          if (arrayValue != null) { // Skip null values.
            builder.addFormField(name, arrayValue.toString(), encoded);
          }
        }
      } else {
        builder.addFormField(name, value.toString(), encoded);
      }
    }
  }

  static final class FieldMap extends RequestBuilderAction {
    private final boolean encoded;

    FieldMap(boolean encoded) {
      this.encoded = encoded;
    }

    @Override void perform(RestAdapterRawRequestBuilder builder, Object value) {
      if (value == null) return; // Skip null values.

      Map<?, ?> map = (Map<?, ?>) value;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        Object entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Field map contained null key.");
        }
        Object entryValue = entry.getValue();
        if (entryValue != null) { // Skip null values.
          builder.addFormField(entryKey.toString(), entryValue.toString(), encoded);
        }
      }
    }
  }

  static final class Part<T> extends RequestBuilderAction {
    private final Headers headers;
    private final Converter<T, RequestBody> converter;

    Part(Headers headers, Converter<T, RequestBody> converter) {
      this.headers = headers;
      this.converter = converter;
    }

    @Override void perform(RestAdapterRawRequestBuilder builder, Object value) {
      if (value == null) return; // Skip null values.

      RequestBody body;
      try {
        //noinspection unchecked
        body = converter.convert((T) value);
      } catch (IOException e) {
        throw new RuntimeException("Unable to convert " + value + " to RequestBody");
      }
      builder.addPart(headers, body);
    }
  }

  static final class PartMap extends RequestBuilderAction {
    private final Retrofit retrofit;
    private final String transferEncoding;
    private final Annotation[] annotations;

    PartMap(Retrofit retrofit, String transferEncoding, Annotation[] annotations) {
      this.retrofit = retrofit;
      this.transferEncoding = transferEncoding;
      this.annotations = annotations;
    }

    @Override void perform(RestAdapterRawRequestBuilder builder, Object value) {
      if (value == null) return; // Skip null values.

      Map<?, ?> map = (Map<?, ?>) value;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        Object entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Part map contained null key.");
        }
        Object entryValue = entry.getValue();
        if (entryValue == null) {
          continue; // Skip null values.
        }

        Headers headers = Headers.of(
            "Content-Disposition", "form-data; name=\"" + entryKey + "\"",
            "Content-Transfer-Encoding", transferEncoding);

        Class<?> entryClass = entryValue.getClass();
        //noinspection unchecked
        Converter<Object, RequestBody> converter =
            (Converter<Object, RequestBody>) retrofit.requestConverter(entryClass, annotations);
        RequestBody body;
        try {
          body = converter.convert(entryValue);
        } catch (IOException e) {
          throw new RuntimeException("Unable to convert " + entryValue + " to RequestBody");
        }
        builder.addPart(headers, body);
      }
    }
  }

  static final class Body<T> extends RequestBuilderAction {
    private final Converter<T, RequestBody> converter;

    Body(Converter<T, RequestBody> converter) {
      this.converter = converter;
    }

    @Override void perform(RestAdapterRawRequestBuilder builder, Object value) {
      if (value == null) {
        throw new IllegalArgumentException("Body parameter value must not be null.");
      }
      RequestBody body;
      try {
        //noinspection unchecked
        body = converter.convert((T) value);
      } catch (IOException e) {
        throw new RuntimeException("Unable to convert " + value + " to RequestBody");
      }
      builder.setBody(body);
    }
  }
}
