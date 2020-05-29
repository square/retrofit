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
import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * A {@linkplain Converter.Factory converter} which uses JAXB. All validation events are ignored.
 */
public final class JaxbConverterFactory extends Converter.Factory {
  static final MediaType XML = MediaType.get("application/xml; charset=utf-8");
  static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  /**
   * Create an instance using a default {@link JAXBContext} instance for conversion, with XML
   * content-type by default.
   */
  public static JaxbConverterFactory create() {
    return new JaxbConverterFactory(null, XML);
  }

  /** Create an instance using {@code context} for conversion, with XML content-type by default. */
  @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
  public static JaxbConverterFactory create(JAXBContext context) {
    if (context == null) throw new NullPointerException("context == null");
    return new JaxbConverterFactory(context, XML);
  }

  /**
   * Create an instance using a default {@link JAXBContext} instance for conversion with custom
   * content type.
   */
  @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
  public static JaxbConverterFactory create(MediaType contentType) {
    if (contentType == null) throw new NullPointerException("contentType == null");
    return new JaxbConverterFactory(null, contentType);
  }

  /** Create an instance using {@code context} for conversion with custom content type. */
  @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
  public static JaxbConverterFactory create(JAXBContext context, MediaType contentType) {
    if (context == null) throw new NullPointerException("context == null");
    if (contentType == null) throw new NullPointerException("contentType == null");
    return new JaxbConverterFactory(context, contentType);
  }

  /** If null, a new JAXB context will be created for each type to be converted. */
  private final @Nullable JAXBContext context;

  private final MediaType contentType;

  private JaxbConverterFactory(JAXBContext context, MediaType contentType) {
    this.context = context;
    this.contentType = contentType;
  }

  @Override
  public @Nullable Converter<?, RequestBody> requestBodyConverter(
      Type type,
      Annotation[] parameterAnnotations,
      Annotation[] methodAnnotations,
      Retrofit retrofit) {
    JAXBContext context = contextForType((Class<?>) type);
    if (contentType.equals(XML)) {
      if (((Class<?>) type).isAnnotationPresent(XmlRootElement.class)) {
        return new JaxbXmlRequestConverter<>(context, (Class<?>) type);
      }
    } else {
      return new JaxbGenericRequestConverter<>(context, contentType, (Class<?>) type);
    }
    return null;
  }

  @Override
  public @Nullable Converter<ResponseBody, ?> responseBodyConverter(
      Type type, Annotation[] annotations, Retrofit retrofit) {
    JAXBContext context = contextForType((Class<?>) type);
    if (contentType.equals(XML)) {
      if (((Class<?>) type).isAnnotationPresent(XmlRootElement.class)) {
        return new JaxbXmlResponseConverter<>(context, (Class<?>) type);
      }
    } else {
      return new JaxbGenericResponseConverter<>(context, (Class<?>) type);
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
