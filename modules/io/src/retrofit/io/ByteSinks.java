// Copyright 2010 Square, Inc.
package retrofit.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility methods for dealing with byte sinks.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public class ByteSinks {

  /**
   * Creates a sink that writes to a file.
   */
  public static ByteSink.Factory forFile(final File file) {
    return new ByteSink.Factory() {
      public ByteSink newSink() throws IOException {
        final FileOutputStream out = new FileOutputStream(file);
        return new ByteSink() {
          public void write(byte[] buffer, int count) throws IOException {
            out.write(buffer, 0, count);
          }
          public void close() throws IOException {
            out.getFD().sync();
            out.close();
          }
        };
      }
    };
  }
}
