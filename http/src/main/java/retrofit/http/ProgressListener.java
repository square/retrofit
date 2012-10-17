// Copyright 2012 Square, Inc.
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
   * @param percent Float bounded by (0..1].
   */
  void hearProgress(float percent);
}
