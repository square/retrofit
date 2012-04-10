// Copyright 2010 Square, Inc.
package retrofit.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** @author Eric Burke (eric@squareup.com) */
public class TypedByteArrayTest {
  public void testEquals() {
    TypedByteArray a1 = new TypedByteArray(new byte[]{10, 20}, MimeType.GIF);
    TypedByteArray a2 = new TypedByteArray(new byte[]{10, 20}, MimeType.GIF);
    TypedByteArray b = new TypedByteArray(new byte[]{8, 12}, MimeType.GIF);

    assertEquals("equals", a1, a2);
    assertEquals("hashCode", a1.hashCode(), a2.hashCode());
    assertFalse("equals", a1.equals(b));
    assertFalse("hashCode", a1.hashCode() == b.hashCode());
  }
}
