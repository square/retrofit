// Copyright 2010 Square, Inc.
package retrofit.internal;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

/** @author Eric Burke (eric@squareup.com) */
public class ObjectsTest {
  @Test public void testNonNull() {
    Objects.nonNull(10, "whatever");
    String message = "fail";
    try {
      Objects.nonNull(null, message);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
      assertThat(expected.getMessage()).isEqualTo(message);
    }
  }
}
