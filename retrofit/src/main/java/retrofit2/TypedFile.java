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
package retrofit2;

import com.squareup.okhttp.MediaType;
import java.io.File;

import static retrofit2.Utils.checkNotNull;

/**
 * Wrapper for a File, its MediaType, and optionally alternative filename.
 */
public final class TypedFile {
  private final MediaType mediaType;
  private final File file;
  private final String fileName;

  public TypedFile(MediaType mediaType, File file) {
    this(mediaType, file, file != null ? file.getName() : null);
  }

  public TypedFile(MediaType mediaType, File file, String fileName) {
    this.mediaType = checkNotNull(mediaType, "MediaType cannot be null.");
    this.file = checkNotNull(file, "File cannot be null.");
    this.fileName = checkNotNull(fileName, "Filename cannot be null.");
  }

  public MediaType mediaType() {
    return mediaType;
  }

  public File file() {
    return file;
  }

  public String fileName() {
    return fileName;
  }

  @Override public String toString() {
    return file.getAbsolutePath() + " (" + mediaType() + ")";
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof TypedFile) {
      TypedFile rhs = (TypedFile) o;
      return file.equals(rhs.file);
    }
    return false;
  }

  @Override public int hashCode() {
    return file.hashCode();
  }
}
