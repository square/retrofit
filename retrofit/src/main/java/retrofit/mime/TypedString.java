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

import java.io.UnsupportedEncodingException;

public class TypedString extends TypedByteArray {

  public TypedString(String string) {
    super("text/plain; charset=UTF-8", convertToBytes(string));
  }

  private static byte[] convertToBytes(String string) {
    try {
      return string.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override public String toString() {
    try {
      return "TypedString[" + new String(getBytes(), "UTF-8") + "]";
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError("Must be able to decode UTF-8");
    }
  }
}
