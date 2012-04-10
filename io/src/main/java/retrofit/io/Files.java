// Copyright 2010 Square, Inc.
package retrofit.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * File utilities.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public class Files {

  /**
   * Copies input stream to given file. Closes input stream when finished.
   */
  public static void copy(InputStream in, File file) throws IOException {
    byte[] buffer = new byte[4096];
    try {
      FileOutputStream out = new FileOutputStream(file);
      try {
        int read;
        while ((read = in.read(buffer)) > -1) out.write(buffer, 0, read);
        out.getFD().sync();
      } finally {
        out.close();
      }
    } finally {
      in.close();
    }
  }

  /**
   * Copies file to given output stream.
   */
  public static void copy(File file, OutputStream out) throws IOException {
    byte[] buffer = new byte[4096];
    FileInputStream in = new FileInputStream(file);
    try {
      int read;
      while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
    } finally {
      in.close();
    }
  }

  /**
   * Create the indicated directory, if it doesn't already exist.
   *
   * @throws IllegalStateException if there is an error creating the directory.
   * @throws IllegalArgumentException if param represents a file instead
   *  of a directory.
   */
  public static void makeDirectory(File directory) {
    if (!directory.exists()) {
      if (!directory.mkdirs()) {
        throw new IllegalStateException("Error creating " + directory + ".");
      }
    } else {
      if (!directory.isDirectory()) {
        throw new IllegalArgumentException("File " + directory +
            " is not a directory");
      }
    }
  }

  /**
   * Build a File object from the given parts, appending each path part to
   * the preceding part.
   */
  public static File build(File baseFile, String... parts) {
    File file = baseFile;
    for (String part : parts) file = new File(file, part);
    return file;
  }

  /**
   * Delete the given file, returning <code>true</code> if the file is gone
   * (that is, if the delete succeeds, or was never there in the first place).
   * A return value of <code>false</code> indicates that the delete failed.
   */
  public static boolean delete(File file) {
    if (file == null) {
      throw new IllegalArgumentException("Cannot delete a null file.");
    }
    return !file.exists() || file.delete();
  }

  /**
   * Ensures changes to the given directory have been written to storage.
   * Returns after all pending changes have been written. Helps prevent
   * data corruption in the event of a system crash.
   *
   * <p>Currently works on Android only. Support for other platforms is in the
   * works.
   *
   * @param directory to synchronize
   */
  public static void sync(File directory) throws IOException {
    Native.sync(directory.getPath());
  }
}

/**
 * Delays loading the native library until {@link Files#sync(java.io.File)}
 * is called.
 */
class Native {
  static {
    System.loadLibrary("retrofit");
  }

  static native void sync(String path);
}
