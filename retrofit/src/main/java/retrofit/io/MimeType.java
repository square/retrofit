// Copyright 2010 Square, Inc.
package retrofit.io;

/**
 * Mime types.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public class MimeType {
  private final String typeName;
  private final String extension;

  public MimeType(String typeName, String extension) {
    this.typeName = typeName;
    this.extension = extension;
  }

  /** Returns the standard type name. */
  public String mimeName() {
    return typeName;
  }

  /** Returns the standard file extension. */
  public String extension() {
    return extension;
  }
}
