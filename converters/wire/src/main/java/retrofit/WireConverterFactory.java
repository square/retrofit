/*
 * Copyright (C) 2013 Square, Inc.
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
import com.squareup.wire.Message;
import com.squareup.wire.Wire;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * A {@linkplain Converter.Factory converter} that uses Wire for protocol buffers.
 * <p>
 * This converter only applies for types which extend from {@link Message} (or one of its
 * subclasses).
 */
public final class WireConverterFactory extends Converter.Factory {
  /** Create an instance using a default {@link Wire} instance for conversion. */
  public static WireConverterFactory create() {
    return create(new Wire());
  }

  /** Create an instance using {@code wire} for conversion. */
  public static WireConverterFactory create(Wire wire) {
    return new WireConverterFactory(wire);
  }

  private final Wire wire;

  /** Create a converter using the supplied {@link Wire} instance. */
  private WireConverterFactory(Wire wire) {
    if (wire == null) throw new NullPointerException("wire == null");
    this.wire = wire;
  }

  @Override
  public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
    if (!(type instanceof Class<?>)) {
      return null;
    }
    Class<?> c = (Class<?>) type;
    if (!Message.class.isAssignableFrom(c)) {
      return null;
    }
    //noinspection unchecked
    return new WireResponseBodyConverter<>(wire, (Class<? extends Message>) c);
  }

  @Override public Converter<?, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
    if (!(type instanceof Class<?>)) {
      return null;
    }
    if (!Message.class.isAssignableFrom((Class<?>) type)) {
      return null;
    }
    return new WireRequestBodyConverter<>();
  }
}
