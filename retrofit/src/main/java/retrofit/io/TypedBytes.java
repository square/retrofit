// Copyright 2010 Square, Inc.
package retrofit.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Binary data with an associated mime type.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public interface TypedBytes {

  /** Returns the mime type. */
  MimeType mimeType();

  /** Length in bytes. */
  int length();

  /** Writes these bytes to the given output stream. */
  void writeTo(OutputStream out) throws IOException;
}
