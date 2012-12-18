// Copyright 2010 Square, Inc.
package retrofit.io;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.fest.assertions.api.Assertions.assertThat;

/** @author Eric Burke (eric@squareup.com) */
public class TypedFileTest {
  @Test public void testNotEquals() {
    TypedFile a = new TypedFile(new File("a.png"), MimeType.PNG);
    TypedFile b = new TypedFile(new File("b.png"), MimeType.PNG);

    assertThat(a).isNotEqualTo(b);
    assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
  }

  @Test public void testEquals() {
    TypedFile a1 = new TypedFile(new File("a.png"), MimeType.PNG);
    TypedFile a2 = new TypedFile(new File("a.png"), MimeType.PNG);

    assertThat(a1).isEqualTo(a2);
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
  }

  @Test public void testToString() {
    File file = new File("/path/to/file.png");

    assertThat(new TypedFile(file, MimeType.PNG).toString()) //
        .isEqualTo(file.getAbsolutePath() + " (PNG)");
  }

  @Test public void testLength() throws IOException {
    File tempFile = File.createTempFile("foo", ".tmp");
    try {
      TypedFile typedFile = new TypedFile(tempFile, MimeType.PNG);
      assertThat(typedFile.length()).isZero();

      writeToFile(tempFile, new byte[] { 0, 1, 2, 3, 4 });

      assertThat(tempFile.length()).isEqualTo(5);
      assertThat(typedFile.length()).isEqualTo(5);
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
