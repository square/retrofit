// Copyright 2010 Square, Inc.
package retrofit.http;

/**
 * Listens for progress.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public interface ProgressListener {

  /**
   * Hears a progress update.
   *
   * @param percent 0-100
   */
  void hearProgress(int percent);
}
