package retrofit.io;

import java.io.File;
import java.io.IOException;

/**
 * Utilities for interacting with the file system at a low level.
 */
public class FileSystem {
  /**
   * Ensures changes to the given directory have been written to storage.
   * Returns after all pending changes have been written. Helps prevent
   * data corruption in the event of a system crash.
   * <p/>
   * Currently works on Android only. Support for other platforms is in the
   * works.
   *
   * @param directory to synchronize
   */
  public static void sync(File directory) throws IOException {
    Native.sync(directory.getPath());
  }
}

/**
 * Delays loading the native library until {@link FileSystem#sync(File)}
 * is called.
 */
class Native {
  static {
    System.loadLibrary("retrofit-android-native");
  }

  static native void sync(String path);
}
