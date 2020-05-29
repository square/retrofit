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
import java.util.Collections;
import java.util.Map;
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
  static final MediaType JSON = MediaType.get("application/xml; charset=utf-8");

  /**
   * Create an instance using a default {@link JAXBContext} instance for conversion, with XML
   * content-type by default.
   */
  public static JaxbConverterFactory create() {
    return new JaxbConverterFactory(null, XML, Collections.emptyMap(), Collections.emptyMap());
  }

  /** Create an instance using {@code context} for conversion, with XML content-type by default. */
  @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
  public static JaxbConverterFactory create(JAXBContext context) {
    if (context == null) throw new NullPointerException("context == null");
    return new JaxbConverterFactory(context, XML, Collections.emptyMap(), Collections.emptyMap());
  }

  /**
   * Create an instance using a default {@link JAXBContext} instance for conversion with custom
   * content type.
   */
  @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
  public static JaxbConverterFactory create(MediaType contentType) {
    if (contentType == null) throw new NullPointerException("contentType == null");
    return new JaxbConverterFactory(
        null, contentType, Collections.emptyMap(), Collections.emptyMap());
  }

  /** Create an instance using {@code context} for conversion with custom content type. */
  @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
  public static JaxbConverterFactory create(JAXBContext context, MediaType contentType) {
    if (context == null) throw new NullPointerException("context == null");
    if (contentType == null) throw new NullPointerException("contentType == null");
    return new JaxbConverterFactory(
        context, contentType, Collections.emptyMap(), Collections.emptyMap());
  }

  /**
   * Create an instance using a default {@link JAXBContext} instance for conversion with custom
   * (un)marshaller properties.
   */
  @SuppressWarnings("ConstantConditions")
  public static JaxbConverterFactory create(
      Map<String, Object> marshalProps, Map<String, Object> unmarshalProps) {
    if (marshalProps == null) throw new NullPointerException("marshalProps == null");
    if (unmarshalProps == null) throw new NullPointerException("marshalProps == null");
    return new JaxbConverterFactory(null, XML, marshalProps, unmarshalProps);
  }

  /**
   * Create an instance using a default {@link JAXBContext} instance for conversion with custom
   * media type and (un)marshaller properties.
   */
  @SuppressWarnings("ConstantConditions")
  public static JaxbConverterFactory create(
      MediaType contentType, Map<String, Object> marshalProps, Map<String, Object> unmarshalProps) {
    if (contentType == null) throw new NullPointerException("convertingMediaType == null");
    if (marshalProps == null) throw new NullPointerException("marshalProps == null");
    if (unmarshalProps == null) throw new NullPointerException("unmarshalProps == null");
    return new JaxbConverterFactory(null, contentType, marshalProps, unmarshalProps);
  }

  /**
   * Create an instance using {@code context} for conversion with custom (un)marshaller properties.
   */
  @SuppressWarnings("ConstantConditions")
  public static JaxbConverterFactory create(
      JAXBContext context, Map<String, Object> marshalProps, Map<String, Object> unmarshalProps) {
    if (context == null) throw new NullPointerException("context == null");
    if (marshalProps == null) throw new NullPointerException("marshalProps == null");
    if (unmarshalProps == null) throw new NullPointerException("unmarshalProps == null");
    return new JaxbConverterFactory(context, XML, marshalProps, unmarshalProps);
  }

  /**
   * Create an instance using {@code context} for conversion with custom media type and
   * (un)marshaller properties.
   */
  @SuppressWarnings("ConstantConditions")
  public static JaxbConverterFactory create(
      JAXBContext context,
      MediaType contentType,
      Map<String, Object> marshalProps,
      Map<String, Object> unmarshalProps) {
    if (context == null) throw new NullPointerException("context == null");
    if (contentType == null) throw new NullPointerException("convertingMediaType == null");
    if (marshalProps == null) throw new NullPointerException("marshalProps == null");
    if (unmarshalProps == null) throw new NullPointerException("unmarshalProps == null");
    return new JaxbConverterFactory(context, contentType, marshalProps, unmarshalProps);
  }

  /** If null, a new JAXB context will be created for each type to be converted. */
  private final @Nullable JAXBContext context;

  private final MediaType contentType;
  private final Map<String, Object> marshalProps;
  private final Map<String, Object> unmarshalProps;

  private JaxbConverterFactory(
      JAXBContext context,
      MediaType contentType,
      Map<String, Object> marshalProps,
      Map<String, Object> unmarshalProps) {
    this.context = context;
    this.contentType = contentType;
    this.marshalProps = marshalProps;
    this.unmarshalProps = unmarshalProps;
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
        return new JaxbXmlRequestConverter<>(context, marshalProps, (Class<?>) type);
      }
    } else {
      return new JaxbGenericRequestConverter<>(context, contentType, marshalProps, (Class<?>) type);
    }
    return null;
  }

  @Override
  public @Nullable Converter<ResponseBody, ?> responseBodyConverter(
      Type type, Annotation[] annotations, Retrofit retrofit) {
    JAXBContext context = contextForType((Class<?>) type);
    if (contentType.equals(XML)) {
      if (((Class<?>) type).isAnnotationPresent(XmlRootElement.class)) {
        return new JaxbXmlResponseConverter<>(context, unmarshalProps, (Class<?>) type);
      }
    } else {
      return new JaxbGenericResponseConverter<>(context, unmarshalProps, (Class<?>) type);
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
