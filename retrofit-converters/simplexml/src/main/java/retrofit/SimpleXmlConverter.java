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
package retrofit;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import okio.Buffer;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * A {@link Converter} which uses SimpleXML for reading and writing entities.
 *
 * @author Fabien Ric (fabien.ric@gmail.com)
 */
public class SimpleXmlConverter implements Converter {
  private static final boolean DEFAULT_STRICT = true;
  private static final String CHARSET = "UTF-8";
  private static final MediaType MEDIA_TYPE =
      MediaType.parse("application/xml; charset=" + CHARSET);

  private final Serializer serializer;

  private final boolean strict;

  public SimpleXmlConverter() {
    this(DEFAULT_STRICT);
  }

  public SimpleXmlConverter(boolean strict) {
    this(new Persister(), strict);
  }

  public SimpleXmlConverter(Serializer serializer) {
    this(serializer, DEFAULT_STRICT);
  }

  public SimpleXmlConverter(Serializer serializer, boolean strict) {
    this.serializer = serializer;
    this.strict = strict;
  }

  @Override public Object fromBody(ResponseBody body, Type type) throws IOException {
    InputStream is = body.byteStream();
    try {
      Object read = serializer.read((Class<?>) type, is, strict);
      if (read == null) {
        throw new IllegalStateException("Could not deserialize body as " + type);
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

  @Override public RequestBody toBody(Object source, Type type) {
    Buffer buffer = new Buffer();
    try {
      OutputStreamWriter osw = new OutputStreamWriter(buffer.outputStream(), CHARSET);
      serializer.write(source, osw);
      osw.flush();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
  }

  public boolean isStrict() {
    return strict;
  }
}
