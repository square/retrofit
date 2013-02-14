// Copyright 2010 Square, Inc.
package retrofit.io;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class TypedByteArrayTest {
  private static final String GIF = "image/gif";

  @Test public void objectEquals() {
    TypedByteArray a1 = new TypedByteArray(new byte[] { 10, 20 }, GIF);
    TypedByteArray a2 = new TypedByteArray(new byte[] { 10, 20 }, GIF);
    TypedByteArray b = new TypedByteArray(new byte[] { 8, 12 }, GIF);

    assertThat(a1).isEqualTo(a2);
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
    assertThat(a1).isNotEqualTo(b);
    assertThat(a1.hashCode()).isNotEqualTo(b.hashCode());
  }
}
