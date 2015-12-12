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
package retrofit2;

import com.squareup.okhttp.ResponseBody;
import com.thoughtworks.xstream.XStream;

import java.io.IOException;
import java.io.InputStream;

final class XStreamResponseBodyConverter<T> implements Converter<ResponseBody, T> {
  private final XStream xStreamSerializer;

  XStreamResponseBodyConverter(XStream xStream) {
    xStreamSerializer = xStream;
  }

  @Override public T convert(ResponseBody value) throws IOException {
    InputStream inputStream = value.byteStream();
    try {
      return (T) xStreamSerializer.fromXML(inputStream);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException ignored) {
        }
      }
    }
  }
}
