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
package retrofit2.converter.simplexml;

import java.io.IOException;
import okhttp3.ResponseBody;
import org.simpleframework.xml.Serializer;
import retrofit2.Converter;

final class SimpleXmlResponseBodyConverter<T> implements Converter<ResponseBody, T> {
  private final Class<T> cls;
  private final Serializer serializer;
  private final boolean strict;

  SimpleXmlResponseBodyConverter(Class<T> cls, Serializer serializer, boolean strict) {
    this.cls = cls;
    this.serializer = serializer;
    this.strict = strict;
  }

  @Override
  public T convert(ResponseBody value) throws IOException {
    try {
      T read = serializer.read(cls, value.charStream(), strict);
      if (read == null) {
        throw new IllegalStateException("Could not deserialize body as " + cls);
      }
      return read;
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      value.close();
    }
  }
}
