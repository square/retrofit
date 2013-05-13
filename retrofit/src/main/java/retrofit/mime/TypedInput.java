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

import java.io.IOException;
import java.io.InputStream;

/**
 * Binary data with an associated mime type.
 *
 * @author Jake Wharton (jw@squareup.com)
 */
public interface TypedInput {

  /** Returns the mime type. */
  String mimeType();

  /** Length in bytes. Returns {@code -1} if length is unknown. */
  long length();

  /**
   * Read bytes as stream. Unless otherwise specified, this method may only be called once. It is
   * the responsibility of the caller to close the stream.
   */
  InputStream in() throws IOException;
}
