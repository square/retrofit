// Copyright 2011 Square, Inc.
package retrofit.io;

import junit.framework.TestCase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * @author Paul Hawke (psh@squareup.com)
 */
public class FilesTest extends TestCase {
  public void testDelete() throws Exception {
    File tmpFile = File.createTempFile("prefix", ".tmp");
    PrintWriter pw = new PrintWriter(new FileWriter(tmpFile));
    pw.println("content");
    pw.close();

    assertTrue(tmpFile.exists());
    assertTrue(Files.delete(tmpFile));
    assertFalse(tmpFile.exists());
  }

  public void testDeleteFileThatDoesntExist() throws Exception {
    File tmpFile = new File("/foobar");
    assertFalse(tmpFile.exists());
    assertTrue(Files.delete(tmpFile));
    assertFalse(tmpFile.exists());
  }

  public void testDeleteNullFile() throws Exception {
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
