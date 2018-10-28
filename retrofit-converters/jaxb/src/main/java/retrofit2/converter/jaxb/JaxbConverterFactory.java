/*
 * Copyright (C) 2018 Square, Inc.
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
package retrofit2.converter.jaxb;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * A {@linkplain Converter.Factory converter} which uses JAXB for XML. All validation events are
 * ignored.
 */
public final class JaxbConverterFactory extends Converter.Factory {
  static final MediaType XML = MediaType.get("application/xml; charset=utf-8");

  /** Create an instance using a default {@link JAXBContext} instance for conversion. */
  public static JaxbConverterFactory create() {
    return new JaxbConverterFactory(null);
  }

  /** Create an instance using {@code context} for conversion. */
  @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
  public static JaxbConverterFactory create(JAXBContext context) {
    if (context == null) throw new NullPointerException("context == null");
    return new JaxbConverterFactory(context);
  }

  /** Create an instance using {@code context}, {@code marshaller}, and {@code unmarshaller} for conversion. */
  @SuppressWarnings("ConstantConditions")
  public static JaxbConverterFactory create(JAXBContext context, Map<String, Object> marshallerProperties,
       Map<String, Object> unmarshallerProperties) {
    if (context == null) throw new NullPointerException("context == null");
    if (marshallerProperties == null) throw new NullPointerException("marshallerProperties == null");
    if (unmarshallerProperties == null) throw new NullPointerException("unmarshallerProperties == null");
    return new JaxbConverterFactory(context, marshallerProperties, unmarshallerProperties);
  }

  /** If null, a new JAXB context will be created for each type to be converted. */
  private final @Nullable JAXBContext context;
  private final @Nullable Map<String, Object> marshallerProperties;
  private final @Nullable Map<String, Object> unmarshallerProperties;

  private JaxbConverterFactory(@Nullable JAXBContext context) {
    this.context = context;
    this.marshallerProperties = new HashMap<>();
    this. unmarshallerProperties = new HashMap<>();
  }

  private JaxbConverterFactory(@Nullable JAXBContext context, @Nullable Map<String, Object> marshallerProperties,
      @Nullable Map<String, Object> unmarshallerProperties) {
    this.context = context;
    this.marshallerProperties = marshallerProperties;
    this.unmarshallerProperties = unmarshallerProperties;
  }

  @Override public @Nullable Converter<?, RequestBody> requestBodyConverter(Type type,
      Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
    if (type instanceof Class && ((Class<?>) type).isAnnotationPresent(XmlRootElement.class)) {
      return new JaxbRequestConverter<>(contextForType((Class<?>) type), (Class<?>) type, marshallerProperties);
    }
    return null;
  }

  @Override public @Nullable Converter<ResponseBody, ?> responseBodyConverter(
      Type type, Annotation[] annotations, Retrofit retrofit) {
    if (type instanceof Class && ((Class<?>) type).isAnnotationPresent(XmlRootElement.class)) {
      return new JaxbResponseConverter<>(contextForType((Class<?>) type), (Class<?>) type, unmarshallerProperties);
    }
    return null;
  }

  private JAXBContext contextForType(Class<?> type) {
    try {
      return context != null ? context : JAXBContext.newInstance(type);
    } catch (JAXBException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
