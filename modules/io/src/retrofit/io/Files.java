// Copyright 2010 Square, Inc.
package retrofit.io;

import java.io.*;

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

  /** Write the byte array to the file. */
  public static void writeFile(File file, byte[] bytes) throws IOException {
    FileOutputStream out = new FileOutputStream(file);
    try {
      out.write(bytes);
    } finally {
      out.close();
    }
  }

  /**
   * Create the indicated directory, if it doesn't already exist.
   * @throws IllegalStateException if there is an error creating the directory.
   * @throws IllegalArgumentException if param represents a file instead
   * of a directory.
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
    for (String part : parts) {
      file = new File(file, part);
    }
    return file;
  }
}
