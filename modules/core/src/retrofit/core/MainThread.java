// Copyright 2010 Square, Inc.
package retrofit.core;

import java.util.concurrent.Executor;

/**
 * Represents the UI thread. The UI thread draws the graphic on the screen
 * and executes activity callbacks.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public interface MainThread extends Executor {

  /**
   * Executes a runnable at a later time.
   *
   * @param r to execute
   * @param delay in ms
   */
  void executeDelayed(Runnable r, long delay);

  /**
   * Removes pending posts of Runnable r that are in the message queue.
   *
   * @param r to remove
   */
  void cancel(Runnable r);

}
