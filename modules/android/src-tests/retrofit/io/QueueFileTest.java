// Copyright 2010 Square, Inc.
package retrofit.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import junit.framework.ComparisonFailure;
import junit.framework.TestCase;

/**
 * Tests for QueueFile.
 *
 * @author Bob Lee (bob@squareup.com)
 */
@SuppressWarnings({"ResultOfMethodCallIgnored"})
public class QueueFileTest extends TestCase {
  private static final Logger logger =
      Logger.getLogger(QueueFileTest.class.getName());

  /**
   * Takes up 33401 bytes in the queue (N*(N+1)/2+4*N). Picked 254 instead of
   * 255 so that the number of bytes isn't a multiple of 4.
   */
  private static int N = 254; //
  private static byte[][] values = new byte[N][];

  static {
    for (int i = 0; i < N; i++) {
      byte[] value = new byte[i];
      // Example: values[3] = { 3, 2, 1 }
      for (int ii = 0; ii < i; ii++) value[ii] = (byte) (i - ii);
      values[i] = value;
    }
  }

  private File file;

  @Override protected void setUp() throws Exception {
    file = File.createTempFile("test.queue", null);
    file.delete();
  }

  @Override protected void tearDown() throws Exception {
    file.delete();
  }

  public void testAddOneElement() throws IOException {
    // This test ensures that we update 'first' correctly.
    QueueFile queue = new QueueFile(file);
    byte[] expected = values[253];
    queue.add(expected);
    assertEquals(expected, queue.peek());
    queue.close();
    queue = new QueueFile(file);
    assertEquals(expected, queue.peek());
  }

  public void testAddAndRemoveElements() throws IOException {
    long start = System.nanoTime();

    Queue<byte[]> expected = new LinkedList<byte[]>();

    for (int round = 0; round < 5; round++) {
      QueueFile queue = new QueueFile(file);
      for (int i = 0; i < N; i++) {
        queue.add(values[i]);
        expected.add(values[i]);
      }

      // Leave N elements in round N, 15 total for 5 rounds. Removing all the
      // elements would be like starting with an empty queue.
      for (int i = 0; i < N - round - 1; i++) {
        assertEquals(expected.remove(), queue.peek());
        queue.remove();
      }
      queue.close();
    }

    // Remove and validate remaining 15 elements.
    QueueFile queue = new QueueFile(file);
    assertEquals(15, queue.size());
    assertEquals(expected.size(), queue.size());
    while (!expected.isEmpty()) {
      assertEquals(expected.remove(), queue.peek());
      queue.remove();
    }
    queue.close();

    // length() returns 0, but I checked the size w/ 'ls', and it is correct.
    // assertEquals(65536, file.length());

    logger.info("Ran in " + ((System.nanoTime() - start) / 1000000) + "ms.");
  }

  /** Tests queue expansion when the data crosses EOF. */
  public void testSplitExpansion() throws IOException {
    // This should result in 3560 bytes.
    int max = 80;

    Queue<byte[]> expected = new LinkedList<byte[]>();
    QueueFile queue = new QueueFile(file);

    for (int i = 0; i < max; i++) {
      expected.add(values[i]);
      queue.add(values[i]);
    }

    // Remove all but 1.
    for (int i = 1; i < max; i++) {
      assertEquals(expected.remove(), queue.peek());
      queue.remove();
    }

    // This should wrap around before expanding.
    for (int i = 0; i < N; i++) {
      expected.add(values[i]);
      queue.add(values[i]);
    }

    while (!expected.isEmpty()) {
      assertEquals(expected.remove(), queue.peek());
      queue.remove();
    }

    queue.close();
  }

  public void testFailedAdd() throws IOException {
    QueueFile queueFile = new QueueFile(file);
    queueFile.add(values[253]);
    queueFile.close();

    final BrokenRandomAccessFile braf = new BrokenRandomAccessFile(file, "rwd");
    queueFile = new QueueFile(braf);

    try {
      queueFile.add(values[252]);
      fail();
    } catch (IOException e) { /* expected */ }

    braf.rejectCommit = false;

    // Allow a subsequent add to succeed.
    queueFile.add(values[251]);

    queueFile.close();

    queueFile = new QueueFile(file);
    assertEquals(2, queueFile.size());
    assertEquals(values[253], queueFile.peek());
    queueFile.remove();
    assertEquals(values[251], queueFile.peek());
  }

  public void testFailedRemoval() throws IOException {
    QueueFile queueFile = new QueueFile(file);
    queueFile.add(values[253]);
    queueFile.close();

    final BrokenRandomAccessFile braf = new BrokenRandomAccessFile(file, "rwd");
    queueFile = new QueueFile(braf);

    try {
      queueFile.remove();
      fail();
    } catch (IOException e) { /* expected */ }

    queueFile.close();

    queueFile = new QueueFile(file);
    assertEquals(1, queueFile.size());
    assertEquals(values[253], queueFile.peek());

    queueFile.add(values[99]);
    queueFile.remove();
    assertEquals(values[99], queueFile.peek());
  }

  public void testFailedExpansion() throws IOException {
    QueueFile queueFile = new QueueFile(file);
    queueFile.add(values[253]);
    queueFile.close();

    final BrokenRandomAccessFile braf = new BrokenRandomAccessFile(file, "rwd");
    queueFile = new QueueFile(braf);

    try {
      // This should trigger an expansion which should fail.
      queueFile.add(new byte[8000]);
      fail();
    } catch (IOException e) { /* expected */ }

    queueFile.close();

    queueFile = new QueueFile(file);

    assertEquals(1, queueFile.size());
    assertEquals(values[253], queueFile.peek());
    assertEquals(4096, queueFile.fileLength);

    queueFile.add(values[99]);
    queueFile.remove();
    assertEquals(values[99], queueFile.peek());
  }

  public void testPeakWithElementReader() throws IOException {
    QueueFile queueFile = new QueueFile(file);
    final byte[] a = {1, 2};
    queueFile.add(a);
    final byte[] b = {3, 4, 5};
    queueFile.add(b);

    queueFile.peek(new QueueFile.ElementReader() {
      @Override public void read(InputStream in, int length) throws IOException {
        assertEquals(length, 2);
        byte[] actual = new byte[length];
        in.read(actual);
        assertEquals(a, actual);
      }
    });

    queueFile.peek(new QueueFile.ElementReader() {
      @Override public void read(InputStream in, int length) throws IOException {
        assertEquals(length, 2);
        assertEquals(1, in.read());
        assertEquals(2, in.read());
        assertEquals(-1, in.read());
      }
    });

    queueFile.remove();

    queueFile.peek(new QueueFile.ElementReader() {
      @Override public void read(InputStream in, int length) throws IOException {
        assertEquals(length, 3);
        byte[] actual = new byte[length];
        in.read(actual);
        assertEquals(b, actual);
      }
    });

    assertEquals(b, queueFile.peek());
    assertEquals(1, queueFile.size());
  }

  public void testForEach() throws IOException {
    QueueFile queueFile = new QueueFile(file);

    final byte[] a = {1, 2};
    queueFile.add(a);
    final byte[] b = {3, 4, 5};
    queueFile.add(b);

    final int[] iteration = new int[]{0};
    QueueFile.ElementReader elementReader = new QueueFile.ElementReader() {
      @Override public void read(InputStream in, int length) throws IOException {
        if (iteration[0] == 0) {
          assertEquals(length, 2);
          byte[] actual = new byte[length];
          in.read(actual);
          assertEquals(a, actual);
        } else if (iteration[0] == 1) {
          assertEquals(length, 3);
          byte[] actual = new byte[length];
          in.read(actual);
          assertEquals(b, actual);
        } else {
          fail();
        }
        iteration[0]++;
      }
    };

    queueFile.forEach(elementReader);

    assertEquals(a, queueFile.peek());
    assertEquals(2, iteration[0]);
  }

  /** Compares two byte[]s for equality. */
  private static void assertEquals(byte[] expected, byte[] actual) {
    if (!Arrays.equals(expected, actual)) {
      throw new ComparisonFailure(null, Arrays.toString(expected),
          Arrays.toString(actual));
    }
  }

  /**
   * Exercise a bug where wrapped elements were getting corrupted when the
   * QueueFile was forced to expand in size and a portion of the final Element
   * had been wrapped into space at the beginning of the file.
   */
  public void testFileExpansionDoesntCorruptWrappedElements()
      throws IOException {
    QueueFile queue = new QueueFile(file);

    // Create test data - 1k blocks marked consecutively 1, 2, 3, 4 and 5.
    byte[][] values = new byte[5][];
    for (int blockNum = 0; blockNum < values.length; blockNum++) {
      values[blockNum] = new byte[1024];
      for (int i = 0; i < values[blockNum].length; i++) {
        values[blockNum][i] = (byte) (blockNum + 1);
      }
    }

    // First, add the first two blocks to the queue, remove one leaving a
    // 1K space at the beginning of the buffer.
    queue.add(values[0]);
    queue.add(values[1]);
    queue.remove();

    // The trailing end of block "4" will be wrapped to the start of the buffer.
    queue.add(values[2]);
    queue.add(values[3]);

    // Cause buffer to expand as there isn't space between the end of block "4"
    // and the start of block "2".  Internally the queue should cause block "4"
    // to be contiguous, but there was a bug where that wasn't happening.
    queue.add(values[4]);

    // Make sure values are not corrupted, specifically block "4" that wasn't
    // being made contiguous in the version with the bug.
    for (int blockNum = 1; blockNum < values.length; blockNum++) {
      byte[] value = queue.peek();
      queue.remove();

      for (int i = 0; i < value.length; i++) {
        assertEquals(
            "Block " + (blockNum + 1) + " corrupted at byte index " + i,
            (byte) (blockNum + 1), value[i]);
      }
    }

    queue.close();
  }

  /**
   * Exercise a bug where wrapped elements were getting corrupted when the
   * QueueFile was forced to expand in size and a portion of the final Element
   * had been wrapped into space at the beginning of the file - if multiple
   * Elements have been written to empty buffer space at the start does the
   * expansion correctly update all their positions?
   */
  public void testFileExpansionCorrectlyMovesElements() throws IOException {
    QueueFile queue = new QueueFile(file);

    // Create test data - 1k blocks marked consecutively 1, 2, 3, 4 and 5.
    byte[][] values = new byte[5][];
    for (int blockNum = 0; blockNum < values.length; blockNum++) {
      values[blockNum] = new byte[1024];
      for (int i = 0; i < values[blockNum].length; i++) {
        values[blockNum][i] = (byte) (blockNum + 1);
      }
    }

    // smaller data elements
    byte[][] smaller = new byte[3][];
    for (int blockNum = 0; blockNum < smaller.length; blockNum++) {
      smaller[blockNum] = new byte[256];
      for (int i = 0; i < smaller[blockNum].length; i++) {
        smaller[blockNum][i] = (byte) (blockNum + 6);
      }
    }

    // First, add the first two blocks to the queue, remove one leaving a
    // 1K space at the beginning of the buffer.
    queue.add(values[0]);
    queue.add(values[1]);
    queue.remove();

    // The trailing end of block "4" will be wrapped to the start of the buffer.
    queue.add(values[2]);
    queue.add(values[3]);

    // Now fill in some space with smaller blocks, none of which will cause
    // an expansion.
    queue.add(smaller[0]);
    queue.add(smaller[1]);
    queue.add(smaller[2]);

    // Cause buffer to expand as there isn't space between the end of the
    // smaller block "8" and the start of block "2".  Internally the queue
    // should cause all of tbe smaller blocks, and the trailing end of
    // block "5" to be moved to the end of the file.
    queue.add(values[4]);

    byte[] expectedBlockNumbers = {2, 3, 4, 6, 7, 8,};

    // Make sure values are not corrupted, specifically block "4" that wasn't
    // being made contiguous in the version with the bug.
    for (byte expectedBlockNumber : expectedBlockNumbers) {
      byte[] value = queue.peek();
      queue.remove();

      for (int i = 0; i < value.length; i++) {
        assertEquals("Block " + (expectedBlockNumber) +
            " corrupted at byte index " + i,
            expectedBlockNumber, value[i]);
      }
    }

    queue.close();
  }

  /**
   * A RandomAccessFile that can break when you go to write the COMMITTED
   * status.
   */
  static class BrokenRandomAccessFile extends RandomAccessFile {
    boolean rejectCommit = true;

    BrokenRandomAccessFile(File file, String mode)
        throws FileNotFoundException {
      super(file, mode);
    }

    @Override public void write(byte[] buffer) throws IOException {
      if (rejectCommit && getFilePointer() == 0) {
        throw new IOException("No commit for you!");
      }
      super.write(buffer);
    }
  }
}
