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
package retrofit2;

import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import com.thoughtworks.xstream.XStream;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * A {@linkplain Converter.Factory converter} which uses XStream Framework for XML.
 */
public class XStreamConverterFactory extends Converter.Factory {
  /** Create an instance using a default {@link XStream} instance for conversion. */
  public static XStreamConverterFactory create() {
    return create(new XStream());
  }

  /** Create an instance using {@code xStreamInstance} for conversion. */
  public static XStreamConverterFactory create(XStream xStream) {
    return new XStreamConverterFactory(xStream);
  }

  private final XStream xStream;

  private XStreamConverterFactory(XStream xStream) {
    if (xStream == null) throw new NullPointerException("xStream == null");
    this.xStream = xStream;
  }

  @Override public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
    if (!(type instanceof Class)) {
      return null;
    }
    xStream.processAnnotations((Class) type);
    return new XStreamResponseBodyConverter<>(xStream);
  }

  @Override public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
    if (!(type instanceof Class)) {
      return null;
    }
    xStream.processAnnotations((Class) type);
    return new XStreamRequestBodyConverter<>(xStream);
  }
}
