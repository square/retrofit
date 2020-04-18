/*
 * Copyright (C) 2016 Square, Inc.
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
package retrofit2.converter.scalars;

import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Converter;

final class ScalarResponseBodyConverters {
  private ScalarResponseBodyConverters() {}

  static final class StringResponseBodyConverter implements Converter<ResponseBody, String> {
    static final StringResponseBodyConverter INSTANCE = new StringResponseBodyConverter();

    @Override
    public String convert(ResponseBody value) throws IOException {
      return value.string();
    }
  }

  static final class BooleanResponseBodyConverter implements Converter<ResponseBody, Boolean> {
    static final BooleanResponseBodyConverter INSTANCE = new BooleanResponseBodyConverter();

    @Override
    public Boolean convert(ResponseBody value) throws IOException {
      return Boolean.valueOf(value.string());
    }
  }

  static final class ByteResponseBodyConverter implements Converter<ResponseBody, Byte> {
    static final ByteResponseBodyConverter INSTANCE = new ByteResponseBodyConverter();

    @Override
    public Byte convert(ResponseBody value) throws IOException {
      return Byte.valueOf(value.string());
    }
  }

  static final class CharacterResponseBodyConverter implements Converter<ResponseBody, Character> {
    static final CharacterResponseBodyConverter INSTANCE = new CharacterResponseBodyConverter();

    @Override
    public Character convert(ResponseBody value) throws IOException {
      String body = value.string();
      if (body.length() != 1) {
        throw new IOException(
            "Expected body of length 1 for Character conversion but was " + body.length());
      }
      return body.charAt(0);
    }
  }

  static final class DoubleResponseBodyConverter implements Converter<ResponseBody, Double> {
    static final DoubleResponseBodyConverter INSTANCE = new DoubleResponseBodyConverter();

    @Override
    public Double convert(ResponseBody value) throws IOException {
      return Double.valueOf(value.string());
    }
  }

  static final class FloatResponseBodyConverter implements Converter<ResponseBody, Float> {
    static final FloatResponseBodyConverter INSTANCE = new FloatResponseBodyConverter();

    @Override
    public Float convert(ResponseBody value) throws IOException {
      return Float.valueOf(value.string());
    }
  }

  static final class IntegerResponseBodyConverter implements Converter<ResponseBody, Integer> {
    static final IntegerResponseBodyConverter INSTANCE = new IntegerResponseBodyConverter();

    @Override
    public Integer convert(ResponseBody value) throws IOException {
      return Integer.valueOf(value.string());
    }
  }

  static final class LongResponseBodyConverter implements Converter<ResponseBody, Long> {
    static final LongResponseBodyConverter INSTANCE = new LongResponseBodyConverter();

    @Override
    public Long convert(ResponseBody value) throws IOException {
      return Long.valueOf(value.string());
    }
  }

  static final class ShortResponseBodyConverter implements Converter<ResponseBody, Short> {
    static final ShortResponseBodyConverter INSTANCE = new ShortResponseBodyConverter();

    @Override
    public Short convert(ResponseBody value) throws IOException {
      return Short.valueOf(value.string());
    }
  }
}
