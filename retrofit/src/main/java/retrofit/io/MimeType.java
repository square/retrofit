// Copyright 2010 Square, Inc.
package retrofit.io;

/**
 * Mime types.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public enum MimeType {

  JSON("application/json", "json"),
  GIF("image/gif", "gif"),
  PNG("image/png", "png"),
  JPEG("image/jpeg", "jpg");

  private final String typeName;
  private final String extension;

  MimeType(String typeName, String extension) {
    this.typeName = typeName;
    this.extension = extension;
  }

  /**
   * Returns the standard type name.
   */
  public String mimeName() {
    return typeName;
  }

  /**
   * Returns the standard file extension.
   */
  public String extension() {
    return extension;
  }
}
