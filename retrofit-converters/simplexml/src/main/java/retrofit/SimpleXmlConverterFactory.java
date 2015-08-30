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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/** A {@linkplain Converter.Factory converter} which uses Simple Framework for XML. */
public final class SimpleXmlConverterFactory implements Converter.Factory {
  /** Create an instance using a default {@link Persister} instance for conversion. */
  public static SimpleXmlConverterFactory create() {
    return create(new Persister());
  }

  /** Create an instance using {@code serializer} for conversion. */
  public static SimpleXmlConverterFactory create(Serializer serializer) {
    return new SimpleXmlConverterFactory(serializer, true);
  }

  /** Create an instance using a default {@link Persister} instance for non-strict conversion. */
  public static SimpleXmlConverterFactory createNonStrict() {
    return createNonStrict(new Persister());
  }

  /** Create an instance using {@code serializer} for non-strict conversion. */
  public static SimpleXmlConverterFactory createNonStrict(Serializer serializer) {
    return new SimpleXmlConverterFactory(serializer, false);
  }

  private final Serializer serializer;
  private final boolean strict;

  private SimpleXmlConverterFactory(Serializer serializer, boolean strict) {
    if (serializer == null) throw new NullPointerException("serializer == null");
    this.serializer = serializer;
    this.strict = strict;
  }

  public boolean isStrict() {
    return strict;
  }

  /** Create a converter for {@code type} provided it is a class. Returns null otherwise. */
  @Override public Converter<?> get(Type type, Annotation[] annotations) {
    if (!(type instanceof Class)) {
      return null;
    }
    Class<?> cls = (Class<?>) type;
    return new SimpleXmlConverter<>(cls, serializer, strict);
  }
}
