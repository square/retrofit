// Copyright 2010 Square, Inc.
package retrofit.io;

import retrofit.core.internal.Objects;

import java.io.Serializable;

/**
 * Support for Typed values.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public abstract class AbstractTypedBytes implements TypedBytes, Serializable {
  private static final long serialVersionUID = 0;

  private final MimeType mimeType;
  private final int length;

  /**
   * Stores the mime type.
   *
   * @throws NullPointerException if mimeType is null
   */
  public AbstractTypedBytes(MimeType mimeType, int length) {
    this.length = length;
    this.mimeType = Objects.nonNull(mimeType, "mimeType");
  }

  public MimeType mimeType() {
    return mimeType;
  }

  public int length() {
    return length;
  }
}