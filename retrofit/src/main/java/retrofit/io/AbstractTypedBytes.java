// Copyright 2010 Square, Inc.
package retrofit.io;

/**
 * Support for Typed values.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public abstract class AbstractTypedBytes implements TypedBytes {
  private final MimeType mimeType;

  /**
   * Stores the mime type.
   *
   * @throws NullPointerException if mimeType is null
   */
  public AbstractTypedBytes(MimeType mimeType) {
    if (mimeType == null) throw new NullPointerException("mimeType");
    this.mimeType = mimeType;
  }

  public MimeType mimeType() {
    return mimeType;
  }

  /** Returns the length in bytes. */
  public abstract int length();
}