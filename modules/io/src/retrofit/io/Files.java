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
}
