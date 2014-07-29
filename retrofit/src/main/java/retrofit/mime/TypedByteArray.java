/*
 * Copyright (C) 2010 Square, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Byte array and its mime type.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public class TypedByteArray implements TypedInput, TypedOutput {
  private final String mimeType;
  private final byte[] bytes;

  /**
   * Constructs a new typed byte array.  Sets mimeType to {@code application/unknown} if absent.
   *
   * @throws NullPointerException if bytes are null
   */
  public TypedByteArray(String mimeType, byte[] bytes) {
    if (mimeType == null) {
      mimeType = "application/unknown";
    }
    if (bytes == null) {
      throw new NullPointerException("bytes");
    }
    this.mimeType = mimeType;
    this.bytes = bytes;
  }

  public byte[] getBytes() {
    return bytes;
  }

  @Override public String fileName() {
    return null;
  }

  @Override public String mimeType() {
    return mimeType;
  }

  @Override public long length() {
    return bytes.length;
  }

  @Override public void writeTo(OutputStream out) throws IOException {
    out.write(bytes);
  }

  @Override public InputStream in() throws IOException {
    return new ByteArrayInputStream(bytes);
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TypedByteArray that = (TypedByteArray) o;

    if (!Arrays.equals(bytes, that.bytes)) return false;
    if (!mimeType.equals(that.mimeType)) return false;

    return true;
  }

  @Override public int hashCode() {
    int result = mimeType.hashCode();
    result = 31 * result + Arrays.hashCode(bytes);
    return result;
  }

  @Override public String toString() {
    return "TypedByteArray[length=" + length() + "]";
  }
}
