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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import retrofit.mime.MimeUtil;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * A {@link Converter} which uses GSON for serialization and deserialization of entities.
 *
 * @author Jake Wharton (jw@squareup.com)
 */
public class GsonConverter implements Converter {
  private final Gson gson;

  public GsonConverter(Gson gson) {
    this.gson = gson;
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
      return new JsonTypedOutput(gson.toJson(object).getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  private static class JsonTypedOutput implements TypedOutput {
    private final byte[] jsonBytes;

    JsonTypedOutput(byte[] jsonBytes) {
      this.jsonBytes = jsonBytes;
    }

    @Override public String fileName() {
      return null;
    }

    @Override public String mimeType() {
      return "application/json; charset=UTF-8";
    }

    @Override public long length() {
      return jsonBytes.length;
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      out.write(jsonBytes);
    }
  }
}
