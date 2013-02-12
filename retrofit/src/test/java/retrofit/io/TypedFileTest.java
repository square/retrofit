// Copyright 2010 Square, Inc.
package retrofit.io;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.fest.assertions.api.Assertions.assertThat;

public class TypedFileTest {
  private static final MimeType PNG = new MimeType("image/png", "png");

  @Test public void objectEquals() {
    TypedFile a1 = new TypedFile(new File("a.png"), PNG);
    TypedFile a2 = new TypedFile(new File("a.png"), PNG);
    TypedFile b = new TypedFile(new File("b.png"), PNG);

    assertThat(a1).isNotEqualTo(b);
    assertThat(a1.hashCode()).isNotEqualTo(b.hashCode());
    assertThat(a1).isEqualTo(a2);
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
  }

  @Test public void objectToString() {
    File file = new File("/path/to/file.png");

    assertThat(new TypedFile(file, PNG).toString()) //
        .isEqualTo(file.getAbsolutePath() + " (image/png)");
  }

  @Test public void length() throws IOException {
    File tempFile = File.createTempFile("foo", ".tmp");
    try {
      TypedFile typedFile = new TypedFile(tempFile, PNG);
      assertThat(typedFile.length()).isZero();

      writeToFile(tempFile, new byte[] { 0, 1, 2, 3, 4 });

      assertThat(tempFile.length()).isEqualTo(5);
      assertThat(typedFile.length()).isEqualTo(5);
    } finally {
      tempFile.delete();
    }
  }

  private static void writeToFile(File file, byte[] data) throws IOException {
    FileOutputStream fos = new FileOutputStream(file);
    try {
      fos.write(data);
    } finally {
      fos.close();
    }
  }
}
