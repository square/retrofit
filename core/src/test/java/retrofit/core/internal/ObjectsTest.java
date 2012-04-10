// Copyright 2010 Square, Inc.
package retrofit.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** @author Eric Burke (eric@squareup.com) */
public class ObjectsTest {
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
