// Copyright 2011 Square, Inc.
package retrofit.io;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

/**
 * @author Paul Hawke (psh@squareup.com)
 */
public class FilesTest {
  @Test public void testDelete() throws Exception {
    File tmpFile = File.createTempFile("prefix", ".tmp");
    PrintWriter pw = new PrintWriter(new FileWriter(tmpFile));
    pw.println("content");
    pw.close();

    assertThat(tmpFile).exists();
    assertThat(Files.delete(tmpFile)).isTrue();
    assertThat(tmpFile).doesNotExist();
  }

  @Test public void testDeleteFileThatDoesntExist() throws Exception {
    File tmpFile = File.createTempFile("foobar", ".tmp");
    assertThat(tmpFile.delete()).as("unable to delete temporary file").isTrue();

    assertThat(tmpFile).doesNotExist();
    assertThat(Files.delete(tmpFile)).isTrue();
    assertThat(tmpFile).doesNotExist();
  }

  @Test public void testDeleteNullFile() throws Exception {
    try {
      Files.delete(null);
      fail("Expected an IAE");
    } catch (IllegalArgumentException e) {
      // expect this
    } catch (Throwable t) {
      t.printStackTrace();
      fail("Expected an IAE");
    }
  }
}
