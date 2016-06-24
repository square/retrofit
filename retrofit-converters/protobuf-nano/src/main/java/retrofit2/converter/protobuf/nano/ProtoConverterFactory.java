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
package retrofit2.converter.protobuf.nano;

import com.google.protobuf.nano.MessageNano;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * A {@linkplain Converter.Factory converter} which uses Protocol Buffers Nano.
 * <p>
 * This converter only applies for types which extend from {@link MessageNano} (or one of its
 * subclasses).
 * @author Keith Banks - Architect at siilo.com
 */
public final class ProtoConverterFactory extends Converter.Factory {
  public static ProtoConverterFactory create() {
    return new ProtoConverterFactory();
  }

  private ProtoConverterFactory() { }

  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(Type type,
                                                        Annotation[] annotations,
                                                        Retrofit retrofit) {
    if (!(type instanceof Class<?>)) {
      return null;
    }
    Class<?> c = (Class<?>) type;
    if (!MessageNano.class.isAssignableFrom(c)) {
      return null;
    }

    return new ProtoResponseBodyConverter<>(getNanoProtoInstance(type));
  }

  @Override
  public Converter<?, RequestBody> requestBodyConverter(Type type,
                                                      Annotation[] parameterAnnotations,
                                                      Annotation[] methodAnnotations,
                                                      Retrofit retrofit) {
    if (!(type instanceof Class<?>)) {
      return null;
    }
    if (!MessageNano.class.isAssignableFrom((Class<?>) type)) {
      return null;
    }
    return new ProtoRequestBodyConverter<>();
  }

  private MessageNano getNanoProtoInstance(final Type type) {
    Class<?> c = (Class<?>) type;
    if (!(MessageNano.class.isAssignableFrom(c))) {
      throw new IllegalArgumentException(
                 "Expected a nanoproto message but was " + c.toString());
    }

    try {
      return (MessageNano) c.newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException("Nanoproto instantiation failed", e);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }
}

