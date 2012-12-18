// Copyright 2010 Square, Inc.
package retrofit.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * File and its mime type.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public class TypedFile extends AbstractTypedBytes {
  private static final long serialVersionUID = 0;

  private final File file;

  /**
   * Constructs a new typed file.
   *
   * @throws NullPointerException if file or mimeType is null
   */
  public TypedFile(File file, MimeType mimeType) {
    super(mimeType);
    if (file == null) throw new NullPointerException("file");
    this.file = file;
  }

  /** Returns the file. */
  public File file() {
    return file;
  }

  public void writeTo(OutputStream out) throws IOException {
    byte[] buffer = new byte[4096];
    FileInputStream in = new FileInputStream(file);
    try {
      int read;
      while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
    } finally {
      in.close();
    }
  }

  /**
   * Atomically moves the contents of this file to a new location.
   *
   * @param destination file
   * @throws java.io.IOException if the move fails
   */
  public void moveTo(TypedFile destination) throws IOException {
    if (mimeType() != destination.mimeType()) {
      throw new IOException("Type mismatch.");
    }
    if (!file.renameTo(destination.file())) {
      throw new IOException("Rename failed!");
    }
  }

  @Override public String toString() {
    return file.getAbsolutePath() + " (" + mimeType() + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof TypedFile) {
      TypedFile rhs = (TypedFile) o;
      return file.equals(rhs.file);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return file.hashCode();
  }

  @Override public int length() {
    return (int) file.length();
  }
}
