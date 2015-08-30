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

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

/** A {@linkplain Converter.Factory converter} which uses Protocol Buffers. */
public final class ProtoConverterFactory implements Converter.Factory {
  public static ProtoConverterFactory create() {
    return new ProtoConverterFactory();
  }


  /**
   * Create a converter for {@code type} provided it is a {@link MessageLite} type. Returns null
   * otherwise.
   */
  @Override public Converter<?> get(Type type, Annotation[] annotations) {
    if (!(type instanceof Class<?>)) {
      return null;
    }
    Class<?> c = (Class<?>) type;
    if (!MessageLite.class.isAssignableFrom(c)) {
      return null;
    }

    Parser<MessageLite> parser;
    try {
      Field field = c.getDeclaredField("PARSER");
      //noinspection unchecked
      parser = (Parser<MessageLite>) field.get(null);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalArgumentException(
          "Found a protobuf message but " + c.getName() + " had no PARSER field.");
    }

    return new ProtoConverter<>(parser);
  }
}
