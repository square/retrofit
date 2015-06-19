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
package retrofit;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import okio.Buffer;
import org.simpleframework.xml.Serializer;

final class SimpleXmlConverter<T> implements Converter<T> {
  private static final String CHARSET = "UTF-8";
  private static final MediaType MEDIA_TYPE =
      MediaType.parse("application/xml; charset=" + CHARSET);

  private final Class<T> cls;
  private final Serializer serializer;
  private final boolean strict;

  SimpleXmlConverter(Class<T> cls, Serializer serializer, boolean strict) {
    this.cls = cls;
    this.serializer = serializer;
    this.strict = strict;
  }

  @Override public T fromBody(ResponseBody body) throws IOException {
    InputStream is = body.byteStream();
    try {
      T read = serializer.read(cls, is, strict);
      if (read == null) {
        throw new IllegalStateException("Could not deserialize body as " + cls);
      }
      return read;
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        is.close();
      } catch (IOException ignored) {
      }
    }
  }

  @Override public RequestBody toBody(T value) {
    Buffer buffer = new Buffer();
    try {
      OutputStreamWriter osw = new OutputStreamWriter(buffer.outputStream(), CHARSET);
      serializer.write(value, osw);
      osw.flush();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
  }
}
