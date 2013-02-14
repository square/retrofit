// Copyright 2010 Square, Inc.
package retrofit.io;

/**
 * Support for Typed values.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public abstract class AbstractTypedBytes implements TypedBytes {
  private final String mimeType;

  /**
   * Stores the mime type.
   *
   * @throws NullPointerException if mimeType is null
   */
  public AbstractTypedBytes(String mimeType) {
    if (mimeType == null) throw new NullPointerException("mimeType");
    this.mimeType = mimeType;
  }

  public String mimeType() {
    return mimeType;
  }

  /** Returns the length in bytes. */
  public abstract int length();
}