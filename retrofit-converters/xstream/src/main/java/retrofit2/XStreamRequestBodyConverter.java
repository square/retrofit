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

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.thoughtworks.xstream.XStream;
import okio.Buffer;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class XStreamRequestBodyConverter<T> implements Converter<T, RequestBody> {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/xml; charset=UTF-8");
  private static final String CHARSET = "UTF-8";

  private final XStream xStreamSerializer;

  XStreamRequestBodyConverter(XStream xStreamSerializer) {
    this.xStreamSerializer = xStreamSerializer;
  }

  @Override public RequestBody convert(T value) throws IOException {
    Buffer buffer = new Buffer();
    OutputStreamWriter osw = new OutputStreamWriter(buffer.outputStream(), CHARSET);
    xStreamSerializer.toXML(value, osw);
    osw.flush();
    return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
  }
}
