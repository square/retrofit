// Copyright 2010 Square, Inc.
package retrofit.mime;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TypedByteArrayTest {
  private static final String GIF = "image/gif";

  @Test public void objectEquals() {
    TypedByteArray a1 = new TypedByteArray(GIF, new byte[] { 10, 20 });
    TypedByteArray a2 = new TypedByteArray(GIF, new byte[] { 10, 20 });
    TypedByteArray b = new TypedByteArray(GIF, new byte[] { 8, 12 });

    assertThat(a1).isEqualTo(a2);
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
    assertThat(a1).isNotEqualTo(b);
    assertThat(a1.hashCode()).isNotEqualTo(b.hashCode());
  }
}
