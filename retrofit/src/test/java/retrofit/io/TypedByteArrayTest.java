// Copyright 2010 Square, Inc.
package retrofit.io;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

/** @author Eric Burke (eric@squareup.com) */
public class TypedByteArrayTest {
  private static final MimeType GIF = new MimeType("image/gif", "gif");

  @Test public void testEquals() {
    TypedByteArray a1 = new TypedByteArray(new byte[] { 10, 20 }, GIF);
    TypedByteArray a2 = new TypedByteArray(new byte[] { 10, 20 }, GIF);
    TypedByteArray b = new TypedByteArray(new byte[] { 8, 12 }, GIF);

    assertThat(a1).isEqualTo(a2);
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
    assertThat(a1).isNotEqualTo(b);
    assertThat(a1.hashCode()).isNotEqualTo(b.hashCode());
  }
}
