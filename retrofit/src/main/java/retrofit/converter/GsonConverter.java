/*
 * Copyright (C) 2012 Square, Inc.
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
package retrofit.converter;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import retrofit.mime.MimeUtil;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * A {@link Converter} which uses GSON for serialization and deserialization of entities.
 *
 * @author Jake Wharton (jw@squareup.com)
 */
public class GsonConverter implements LoggingConverter {
  private final Gson gson;
  private String encoding;

  /**
   * Create an instance using the supplied {@link Gson} object for conversion. Encoding to JSON and
   * decoding from JSON (when no charset is specified by a header) will use UTF-8.
   */
  public GsonConverter(Gson gson) {
    this(gson, "UTF-8");
  }

  /**
   * Create an instance using the supplied {@link Gson} object for conversion. Encoding to JSON and
   * decoding from JSON (when no charset is specified by a header) will use the specified encoding.
   */
  public GsonConverter(Gson gson, String encoding) {
    this.gson = gson;
    this.encoding = encoding;
  }

  @Override public Object fromBody(TypedInput body, Type type) throws ConversionException {
    String charset = "UTF-8";
    if (body.mimeType() != null) {
      charset = MimeUtil.parseCharset(body.mimeType());
    }
    InputStreamReader isr = null;
    try {
      isr = new InputStreamReader(body.in(), charset);
      return gson.fromJson(isr, type);
    } catch (IOException e) {
      throw new ConversionException(e);
    } catch (JsonParseException e) {
      throw new ConversionException(e);
    } finally {
      if (isr != null) {
        try {
          isr.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  @Override public TypedOutput toBody(Object object) {
    try {
      return new JsonTypedOutput(gson.toJson(object).getBytes(encoding), encoding);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  @Override public String bodyToLogString(TypedInput body, Type type) {
    String charset = "UTF-8";
    if (body.mimeType() != null) {
      charset = MimeUtil.parseCharset(body.mimeType());
    }

    try {
      byte[] data;
      if (body instanceof TypedByteArray) {
        data = ((TypedByteArray) body).getBytes();
      } else {
        InputStream in = body.in();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = in.read(buffer)) != -1) {
          baos.write(buffer, 0, count);
        }
        data = baos.toByteArray();
      }
      return new String(data, charset);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override public String bodyToLogString(Object object) {
    return gson.toJson(object);
  }

  private static class JsonTypedOutput implements TypedOutput {
    private final byte[] jsonBytes;
    private final String mimeType;

    JsonTypedOutput(byte[] jsonBytes, String encode) {
      this.jsonBytes = jsonBytes;
      this.mimeType = "application/json; charset=" + encode;
    }

    @Override public String fileName() {
      return null;
    }

    @Override public String mimeType() {
      return mimeType;
    }

    @Override public long length() {
      return jsonBytes.length;
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      out.write(jsonBytes);
    }
  }
}
