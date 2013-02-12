/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit.http.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Utility methods for working with streams. */
class Streams {
  private static final int BUFFER_SIZE = 0x1000;

  /**
   * Creates a {@code byte[]} from reading the entirety of an {@link InputStream}. May return an
   * empty array but never {@code null}.
   */
  static byte[] readFully(InputStream stream) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    if (stream != null) {
      byte[] buf = new byte[BUFFER_SIZE];
      int r;
      while ((r = stream.read(buf)) != -1) {
        baos.write(buf, 0, r);
      }
    }
    return baos.toByteArray();
  }
}
