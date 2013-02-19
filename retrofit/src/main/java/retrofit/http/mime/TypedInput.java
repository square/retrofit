// Copyright 2013 Square, Inc.
package retrofit.http.mime;

import java.io.IOException;
import java.io.InputStream;

/**
 * Binary data with an associated mime type.
 *
 * @author Jake Wharton (jw@squareup.com)
 */
public interface TypedInput {

  /** Returns the mime type. */
  String mimeType();

  /** Length in bytes. Returns {@code -1} if length is unknown. */
  long length();

  /**
   * Read bytes as stream. Unless otherwise specified, this method may only be called once. It is
   * the responsibility of the caller to close the stream.
   */
  InputStream in() throws IOException;
}
