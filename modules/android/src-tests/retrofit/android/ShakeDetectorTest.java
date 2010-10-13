// Copyright 2010 Square, Inc.
package retrofit.android;

import java.util.List;
import junit.framework.TestCase;

/** @author Eric Burke (eric@squareup.com) */
public class ShakeDetectorTest extends TestCase {
  public void testInitialShaking() {
    ShakeDetector.SampleQueue q = new ShakeDetector.SampleQueue();
    assertFalse("shaking", q.isShaking());
  }

  /** Tests LG Ally sample rate. */
  public void testShakingSampleCount3() {
    ShakeDetector.SampleQueue q = new ShakeDetector.SampleQueue();

    // These times approximate the data rate of the slowest device we've
    // found, the LG Ally.
    // on the LG Ally. The queue holds 500000000 ns (0.5ms) of samples or
    // 4 samples, whichever is greater.
    // 500000000
    q.add(1000000000L, false);
    q.add(1300000000L, false);
    q.add(1600000000L, false);
    q.add(1900000000L, false);
    assertContent(q, false, false, false, false);
    assertFalse("shaking", q.isShaking());

    // The oldest two entries will be removed.
    q.add(2200000000L, true);
    q.add(2500000000L, true);
    assertContent(q, false, false, true, true);
    assertFalse("shaking", q.isShaking());

    // Another entry should be removed, now 3 out of 4 are true.
    q.add(2800000000L, true);
    assertContent(q, false, true, true, true);
    assertTrue("shaking", q.isShaking());

    q.add(3100000000L, false);
    assertContent(q, true, true, true, false);
    assertTrue("shaking", q.isShaking());

    q.add(3400000000L, false);
    assertContent(q, true, true, false, false);
    assertFalse("shaking", q.isShaking());
  }

  private void assertContent(ShakeDetector.SampleQueue q, boolean... expected) {
    List<ShakeDetector.Sample> samples = q.asList();

    StringBuilder sb = new StringBuilder();
    for (ShakeDetector.Sample s : samples) {
      sb.append(String.format("[%b,%d] ", s.accelerating, s.timestamp));
    }

    assertEquals(sb.toString(), expected.length, samples.size());
    for (int i = 0; i < expected.length; i++) {
      assertEquals("sample[" + i + "] accelerating",
          expected[i], samples.get(i).accelerating);
    }
  }

  public void testClear() {
    ShakeDetector.SampleQueue q = new ShakeDetector.SampleQueue();
    q.add(1000000000L, true);
    q.add(1200000000L, true);
    q.add(1400000000L, true);
    assertTrue("shaking", q.isShaking());
    q.clear();
    assertFalse("shaking", q.isShaking());
  }
}
