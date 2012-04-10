// Copyright 2010 Square, Inc.
package retrofit.io;

import java.io.IOException;

/**
 * An interface equivalent to OutputStream.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public interface ByteSink {

  /**
   * Writes the specified number of bytes from buffer.
   */
  void write(byte[] buffer, int count) throws IOException;

  /**
   * Closes the sink.
   */
  void close() throws IOException;

  /**
   * Constructs a new sink.
   */
  public interface Factory {

    /**
     * Constructs a new sink.
     */
    ByteSink newSink() throws IOException;
  }
}
