// Copyright 2013 Square, Inc.
package retrofit.http.mime;

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

  /** Length in bytes. */
  long length();

  /** Writes these bytes to the given output stream. */
  void writeTo(OutputStream out) throws IOException;
}
