/*
 * Copyright (C) 2010 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A reliable, efficient, file-based, FIFO queue. Additions and removals are
 * O(1). All operations are atomic. Writes are synchronous; data will be written
 * to disk before an operation returns. The underlying file is structured to
 * survive process and even system crashes. If an I/O exception is thrown during
 * a mutating change, the change is aborted. It is safe to continue to use a
 * {@code QueueFile} instance after an exception.
 * <p/>
 * <p>All operations are synchronized. In a traditional queue, the remove
 * operation returns an element. In this queue, {@link #peek} and {@link
 * #remove} are used in conjunction. Use {@code peek} to retrieve the first
 * element, and then {@code remove} to remove it after successful processing. If
 * the system crashes after {@code peek} and during processing, the element will
 * remain in the queue, to be processed when the system restarts.
 * <p/>
 * <p><b><font color="red">NOTE:</font></b> The current implementation is built
 * for file systems that support atomic segment writes (like YAFFS). Most
 * conventional file systems don't support this; if the power goes out while
 * writing a segment, the segment will contain garbage and the file will be
 * corrupt. We'll add journaling support so this class can be used with more
 * file systems later.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public class QueueFile {
  private static final Logger logger =
      Logger.getLogger(QueueFile.class.getName());

  /** Initial file size in bytes. */
  private static final int INITIAL_LENGTH = 4096; // one file system block

  /** Length of header in bytes. */
  static final int HEADER_LENGTH = 16;

  /**
   * The underlying file. Uses a ring buffer to store entries. Designed so that
   * a modification isn't committed or visible until we write the header. The
   * header is much smaller than a segment. So long as the underlying file
   * system supports atomic segment writes, changes to the queue are atomic.
   * Storing the file length ensures we can recover from a failed expansion
   * (i.e. if setting the file length succeeds but the process dies before the
   * data can be copied).
   * <p/>
   * <pre>
   *   Format:
   *     Header              (16 bytes)
   *     Element Ring Buffer (File Length - 16 bytes)
   * <p/>
   *   Header:
   *     File Length            (4 bytes)
   *     Element Count          (4 bytes)
   *     First Element Position (4 bytes, =0 if null)
   *     Last Element Position  (4 bytes, =0 if null)
   * <p/>
   *   Element:
   *     Length (4 bytes)
   *     Data   (Length bytes)
   * </pre>
   */
  private final RandomAccessFile raf;

  /** Cached file length. Always a power of 2. */
  int fileLength;

  /** Number of elements. */
  private int elementCount;

  /** Pointer to first (or eldest) element. */
  private Element first;

  /** Pointer to last (or newest) element. */
  private Element last;

  /** In-memory buffer. Big enough to hold the header. */
  private final byte[] buffer = new byte[16];

  /**
   * Constructs a new queue backed by the given file. Only one {@code QueueFile}
   * instance should access a given file at a time.
   */
  public QueueFile(File file) throws IOException {
    if (!file.exists()) initialize(file);
    raf = open(file);
    readHeader();
  }

  /** For testing. */
  QueueFile(RandomAccessFile raf) throws IOException {
    this.raf = raf;
    readHeader();
  }

  /**
   * Stores int in buffer. The behavior is equivalent to calling {@link
   * RandomAccessFile#writeInt}.
   */
  private static void writeInt(byte[] buffer, int offset, int value) {
    buffer[offset] = (byte) (value >> 24);
    buffer[offset + 1] = (byte) (value >> 16);
    buffer[offset + 2] = (byte) (value >> 8);
    buffer[offset + 3] = (byte) value;
  }

  /**
   * Stores int values in buffer. The behavior is equivalent to calling {@link
   * RandomAccessFile#writeInt} for each value.
   */
  private static void writeInts(byte[] buffer, int... values) {
    int offset = 0;
    for (int value : values) {
      writeInt(buffer, offset, value);
      offset += 4;
    }
  }

  /** Reads an int from a byte[]. */
  private static int readInt(byte[] buffer, int offset) {
    return ((buffer[offset] & 0xff) << 24)
        + ((buffer[offset + 1] & 0xff) << 16)
        + ((buffer[offset + 2] & 0xff) << 8)
        + (buffer[offset + 3] & 0xff);
  }

  /** Reads the header. */
  private void readHeader() throws IOException {
    raf.seek(0);
    raf.readFully(buffer);
    fileLength = readInt(buffer, 0);
    if (fileLength > raf.length()) {
      throw new IOException("File is truncated. Expected length: "
          + fileLength + ", Actual length: " + raf.length());
    }
    elementCount = readInt(buffer, 4);
    int firstOffset = readInt(buffer, 8);
    int lastOffset = readInt(buffer, 12);
    first = readElement(firstOffset);
    last = readElement(lastOffset);
  }

  /**
   * Writes header atomically. The arguments contain the updated values. The
   * class member fields should not have changed yet. This only updates the
   * state in the file. It's up to the caller to update the class member
   * variables *after* this call succeeds. Assumes segment writes are atomic in
   * the underlying file system.
   */
  private void writeHeader(int fileLength, int elementCount, int firstPosition,
                           int lastPosition) throws IOException {
    writeInts(buffer, fileLength, elementCount, firstPosition, lastPosition);
    raf.seek(0);
    raf.write(buffer);
  }

  /** Returns the Element for the given offset. */
  private Element readElement(int position) throws IOException {
    if (position == 0) return Element.NULL;
    raf.seek(position);
    return new Element(position, raf.readInt());
  }

  /** Atomically initializes a new file. */
  private static void initialize(File file) throws IOException {
    // Use a temp file so we don't leave a partially-initialized file.
    File tempFile = new File(file.getPath() + ".tmp");
    RandomAccessFile raf = open(tempFile);
    try {
      raf.setLength(INITIAL_LENGTH);
      raf.seek(0);
      byte[] headerBuffer = new byte[16];
      writeInts(headerBuffer, INITIAL_LENGTH, 0, 0, 0);
      raf.write(headerBuffer);
    } finally {
      raf.close();
    }

    // A rename is atomic.
    if (!tempFile.renameTo(file)) throw new IOException("Rename failed!");
  }

  /** Opens a random access file that writes synchronously. */
  private static RandomAccessFile open(File file) throws FileNotFoundException {
    return new RandomAccessFile(file, "rwd");
  }

  /** Wraps the position if it exceeds the end of the file. */
  private int wrapPosition(int position) {
    return position < fileLength ? position
        : HEADER_LENGTH + position - fileLength;
  }

  /**
   * Writes count bytes from buffer to position in file. Automatically wraps
   * write if position is past the end of the file or if buffer overlaps it.
   *
   * @param position in file to write to
   * @param buffer   to write from
   * @param count    # of bytes to write
   */
  private void ringWrite(int position, byte[] buffer, int offset, int count)
      throws IOException {
    position = wrapPosition(position);
    if (position + count <= fileLength) {
      raf.seek(position);
      raf.write(buffer, offset, count);
    } else {
      // The write overlaps the EOF.
      // # of bytes to write before the EOF.
      int beforeEof = fileLength - position;
      raf.seek(position);
      raf.write(buffer, offset, beforeEof);
      raf.seek(HEADER_LENGTH);
      raf.write(buffer, offset + beforeEof, count - beforeEof);
    }
  }

  /**
   * Reads count bytes into buffer from file. Wraps if necessary.
   *
   * @param position in file to read from
   * @param buffer   to read into
   * @param count    # of bytes to read
   */
  private void ringRead(int position, byte[] buffer, int offset, int count)
      throws IOException {
    position = wrapPosition(position);
    if (position + count <= fileLength) {
      raf.seek(position);
      raf.readFully(buffer, 0, count);
    } else {
      // The read overlaps the EOF.
      // # of bytes to read before the EOF.
      int beforeEof = fileLength - position;
      raf.seek(position);
      raf.readFully(buffer, offset, beforeEof);
      raf.seek(HEADER_LENGTH);
      raf.readFully(buffer, offset + beforeEof, count - beforeEof);
    }
  }

  /**
   * Adds an element to the end of the queue.
   *
   * @param data to copy bytes from
   */
  public void add(byte[] data) throws IOException {
    add(data, 0, data.length);
  }

  /**
   * Adds an element to the end of the queue.
   *
   * @param data   to copy bytes from
   * @param offset to start from in buffer
   * @param count  number of bytes to copy
   * @throws IndexOutOfBoundsException if {@code offset < 0} or {@code count <
   *                                   0}, or if {@code offset + count} is
   *                                   bigger than the length of {@code
   *                                   buffer}.
   */
  public synchronized void add(byte[] data, int offset, int count)
      throws IOException {
    nonNull(data, "buffer");
    if ((offset | count) < 0 || count > data.length - offset) {
      throw new IndexOutOfBoundsException();
    }

    expandIfNecessary(count);

    // Insert a new element after the current last element.
    boolean wasEmpty = isEmpty();
    int position = wasEmpty ? HEADER_LENGTH : wrapPosition(
        last.position + Element.HEADER_LENGTH + last.length);
    Element newLast = new Element(position, count);

    // Write length.
    writeInt(buffer, 0, count);
    ringWrite(newLast.position, buffer, 0, Element.HEADER_LENGTH);

    // Write data.
    ringWrite(newLast.position + Element.HEADER_LENGTH, data, offset, count);

    // Commit the addition. If wasEmpty, first == last.
    int firstPosition = wasEmpty ? newLast.position : first.position;
    writeHeader(fileLength, elementCount + 1, firstPosition, newLast.position);
    last = newLast;
    elementCount++;
    if (wasEmpty) first = last; // first element
  }

  /** Returns the number of used bytes. */
  private int usedBytes() {
    if (elementCount == 0) return HEADER_LENGTH;

    if (last.position >= first.position) {
      // Contiguous queue.
      return (last.position - first.position)   // all but last entry
          + Element.HEADER_LENGTH + last.length // last entry
          + HEADER_LENGTH;
    } else {
      // tail < head. The queue wraps.
      return last.position                      // buffer front + header
          + Element.HEADER_LENGTH + last.length // last entry
          + fileLength - first.position;        // buffer end
    }
  }

  /** Returns number of unused bytes. */
  private int remainingBytes() {
    return fileLength - usedBytes();
  }

  /** Returns true if this queue contains no entries. */
  public synchronized boolean isEmpty() {
    return elementCount == 0;
  }

  /**
   * If necessary, expands the file to accommodate an additional element of the
   * given length.
   *
   * @param dataLength length of data being added
   */
  private void expandIfNecessary(int dataLength) throws IOException {
    int elementLength = Element.HEADER_LENGTH + dataLength;
    int remainingBytes = remainingBytes();
    if (remainingBytes >= elementLength) return;

    // Expand.
    int previousLength = fileLength;
    int newLength;
    // Double the length until we can fit the new data.
    do {
      remainingBytes += previousLength;
      newLength = previousLength << 1;
      previousLength = newLength;
    } while (remainingBytes < elementLength);

    setLength(newLength);

    // Calculate the position of the tail end of the data in the ring buffer
    int endOfLastElement = wrapPosition(
        last.position + Element.HEADER_LENGTH + last.length);

    // If the buffer is split, we need to make it contiguous
    if (endOfLastElement < first.position) {
      FileChannel channel = raf.getChannel();
      channel.position(fileLength); // destination position
      int count = endOfLastElement - Element.HEADER_LENGTH;
      if (channel.transferTo(HEADER_LENGTH, count, channel) != count) {
        throw new AssertionError("Copied insufficient number of bytes!");
      }
    }

    // Commit the expansion.
    if (last.position < first.position) {
      int newLastPosition = fileLength + last.position - HEADER_LENGTH;
      writeHeader(newLength, elementCount, first.position, newLastPosition);
      last = new Element(newLastPosition, last.length);
    } else {
      writeHeader(newLength, elementCount, first.position, last.position);
    }

    fileLength = newLength;
  }

  /** Sets the length of the file. */
  private void setLength(int newLength) throws IOException {
    // Set new file length (considered metadata) and sync it to storage.
    raf.setLength(newLength);
    raf.getChannel().force(true);
  }

  /** Reads the eldest element. Returns null if the queue is empty. */
  public synchronized byte[] peek() throws IOException {
    if (isEmpty()) return null;
    int length = first.length;
    byte[] data = new byte[length];
    ringRead(first.position + Element.HEADER_LENGTH, data, 0, length);
    return data;
  }

  /** Invokes reader with the eldest element, if an element is available. */
  public synchronized void peek(ElementReader reader) throws IOException {
    if (elementCount > 0) {
      reader.read(new ElementInputStream(first), first.length);
    }
  }

  /**
   * Invokes the given reader once for each element in the queue, from eldest to
   * most recently added.
   */
  public synchronized void forEach(ElementReader reader) throws IOException {
    int position = first.position;
    for (int i = 0; i < elementCount; i++) {
      Element current = readElement(position);
      reader.read(new ElementInputStream(current), current.length);
      position = wrapPosition(current.position + Element.HEADER_LENGTH
          + current.length);
    }
  }

  /**
   * Returns t unless it's null.
   *
   * @throws NullPointerException if t is null
   */
  private static <T> T nonNull(T t, String name) {
    if (t == null) throw new NullPointerException(name);
    return t;
  }

  /** Reads a single element. */
  private class ElementInputStream extends InputStream {
    private int position;
    private int remaining;

    private ElementInputStream(Element element) {
      position = wrapPosition(element.position + Element.HEADER_LENGTH);
      remaining = element.length;
    }

    @Override public int read(byte[] buffer, int offset, int length)
        throws IOException {
      nonNull(buffer, "buffer");
      if ((offset | length) < 0 || length > buffer.length - offset) {
        throw new ArrayIndexOutOfBoundsException();
      }
      if (length > remaining) length = remaining;
      ringRead(position, buffer, offset, length);
      position = wrapPosition(position + length);
      remaining -= length;
      return length;
    }

    @Override public int read() throws IOException {
      if (remaining == 0) return -1;
      raf.seek(position);
      int b = raf.read();
      position = wrapPosition(position + 1);
      remaining--;
      return b;
    }
  }

  /** Returns the number of elements in this queue. */
  public synchronized int size() {
    return elementCount;
  }

  /**
   * Removes the eldest element.
   *
   * @throws NoSuchElementException if the queue is empty
   */
  public synchronized void remove() throws IOException {
    if (isEmpty()) throw new NoSuchElementException();
    if (elementCount == 1) {
      clear();
    } else {
      // assert elementCount > 1
      int newFirstPosition = wrapPosition(first.position
          + Element.HEADER_LENGTH + first.length);
      ringRead(newFirstPosition, buffer, 0, Element.HEADER_LENGTH);
      int length = readInt(buffer, 0);
      writeHeader(fileLength, elementCount - 1, newFirstPosition,
          last.position);
      elementCount--;
      first = new Element(newFirstPosition, length);
    }
  }

  /** Clears this queue. Truncates the file to the initial size. */
  public synchronized void clear() throws IOException {
    writeHeader(INITIAL_LENGTH, 0, 0, 0);
    elementCount = 0;
    first = last = Element.NULL;
    fileLength = INITIAL_LENGTH;
    if (fileLength > INITIAL_LENGTH) setLength(INITIAL_LENGTH);
  }

  /** Closes the underlying file. */
  public synchronized void close() throws IOException {
    raf.close();
  }

  @Override public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append(getClass().getSimpleName()).append('[');
    builder.append("fileLength=").append(fileLength);
    builder.append(", size=").append(elementCount);
    builder.append(", first=").append(first);
    builder.append(", last=").append(last);
    builder.append(", element lengths=[");
    try {
      forEach(new ElementReader() {
        boolean first = true;

        @Override public void read(InputStream in, int length) throws IOException {
          if (first) {
            first = false;
          } else {
            builder.append(", ");
          }
          builder.append(length);
        }
      });
    } catch (IOException e) {
      logger.log(Level.WARNING, "read error", e);
    }
    builder.append("]]");
    return builder.toString();
  }

  /** A pointer to an element. */
  static class Element {

    /** Length of element header in bytes. */
    static final int HEADER_LENGTH = 4;

    /** Null element. */
    static final Element NULL = new Element(0, 0);

    /** Position in file. */
    final int position;

    /** The length of the data. */
    final int length;

    /**
     * Constructs a new element.
     *
     * @param position within file
     * @param length   of data
     */
    Element(int position, int length) {
      this.position = position;
      this.length = length;
    }

    @Override public String toString() {
      return getClass().getSimpleName() + "["
          + "position = " + position
          + ", length = " + length + "]";
    }
  }

  /**
   * Reads queue elements. Enables partial reads as opposed to reading all of
   * the bytes into a byte[].
   */
  public interface ElementReader {

    /*
     * TODO: Support remove() call from read().
     */

    /**
     * Called once per element.
     *
     * @param in     stream of element data. Reads as many bytes as requested,
     *               unless fewer than the request number of bytes remains, in
     *               which case it reads all the remaining bytes. Not buffered.
     * @param length of element data in bytes
     */
    public void read(InputStream in, int length) throws IOException;
  }
}
