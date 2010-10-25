// Copyright 2010 Square, Inc.
package retrofit.core.internal;

import junit.framework.TestCase;

/** @author Eric Burke (eric@squareup.com) */
public class ObjectsTest extends TestCase {
  public void testNonNull() {
    Objects.nonNull(10, "whatever");
    try {
      Objects.nonNull(null, "fail");
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
      assertEquals("fail", expected.getMessage());
    }
  }
}
