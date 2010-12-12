// Copyright 2010 Square, Inc.
package retrofit.io;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

  public void testLength() throws IOException {
    File tempFile = File.createTempFile("foo", ".tmp");
    try {
      TypedFile typedFile = new TypedFile(tempFile, MimeType.PNG);
      assertEquals("length", 0, typedFile.length());

      writeToFile(tempFile, new byte[]{0, 1, 2, 3, 4});

      assertEquals("file length", 5, tempFile.length());
      assertEquals("typed file length", 5, typedFile.length());

    } finally {
      //noinspection ResultOfMethodCallIgnored
      tempFile.delete();
    }
  }

  private void writeToFile(File file, byte[] data) throws IOException {
    FileOutputStream fos = new FileOutputStream(file);
    try {
      fos.write(data);
    } finally {
      fos.close();
    }
  }
}