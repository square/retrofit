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
import java.io.OutputStream;

/**
 * Binary data with an associated mime type.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public interface TypedOutput {
  /** Original filename.
   *
   * Used only for multipart requests, may be null. */
  String fileName();

  /** Returns the mime type. */
  String mimeType();

  /** Length in bytes or -1 if unknown. */
  long length();

  /** Writes these bytes to the given output stream. */
  void writeTo(OutputStream out) throws IOException;
}
