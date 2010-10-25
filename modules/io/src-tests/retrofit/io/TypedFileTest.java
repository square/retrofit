// Copyright 2010 Square, Inc.
package retrofit.io;

import junit.framework.TestCase;

import java.io.File;

/** @author Eric Burke (eric@squareup.com) */
public class TypedFileTest extends TestCase {
  public void testNotEquals() {
    TypedFile a = new TypedFile(new File("a.png"), MimeType.PNG);
    TypedFile b = new TypedFile(new File("b.png"), MimeType.PNG);

    assertFalse("equals", a.equals(b));
    assertFalse("hash code", a.hashCode() == b.hashCode());
  }

  public void testEquals() {
    TypedFile a1 = new TypedFile(new File("a.png"), MimeType.PNG);
    TypedFile a2 = new TypedFile(new File("a.png"), MimeType.PNG);

    assertEquals(a1, a2);
    assertEquals("hash code", a1.hashCode(), a2.hashCode());
  }

  public void testToString() {
    File file = new File("/path/to/file.png");

    assertEquals(file.getAbsolutePath() + " (PNG)",
        new TypedFile(file, MimeType.PNG).toString());
  }
}