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
package retrofit.mime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

public final class FormUrlEncodedTypedOutput implements TypedOutput {
  final ByteArrayOutputStream content = new ByteArrayOutputStream();

  public void addField(String name, String value) {
    addField(name, true, value, true);
  }

  public void addField(String name, boolean encodeName, String value, boolean encodeValue) {
    if (name == null) {
      throw new NullPointerException("name");
    }
    if (value == null) {
      throw new NullPointerException("value");
    }
    if (content.size() > 0) {
      content.write('&');
    }
    try {
      if (encodeName) {
        name = URLEncoder.encode(name, "UTF-8");
      }
      if (encodeValue) {
        value = URLEncoder.encode(value, "UTF-8");
      }

      content.write(name.getBytes("UTF-8"));
      content.write('=');
      content.write(value.getBytes("UTF-8"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override public String fileName() {
    return null;
  }

  @Override public String mimeType() {
    return "application/x-www-form-urlencoded; charset=UTF-8";
  }

  @Override public long length() {
    return content.size();
  }

  @Override public void writeTo(OutputStream out) throws IOException {
    out.write(content.toByteArray());
  }
}
