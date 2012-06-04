// Copyright 2010 Square, Inc.
package retrofit.core;

import java.util.concurrent.Executor;

/**
 * Represents the UI thread. The UI thread draws the graphic on the screen
 * and executes activity callbacks.
 * <p>
 * There is an Android specific implementation in retrofit.android.AndroidMainThread.
 * To use it add a provider method like the following to your Guice config:<code><pre>
 * {@literal @}Provides @Singleton MainThread provideMainThread() {
 *   return new AndroidMainThread();
 * }
 * </pre></code>
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
   * Executes a runnable and waits until it has finished.
   *
   * @param r to execute
   */
  void executeSynchronously(Runnable r);

  /**
   * Executes a runnable immediately if we're on the main thread, otherwise
   * posts it to be run later on the main thread.
   */
  void executeOnMain(Runnable r);

  /**
   * Removes pending posts of Runnable r that are in the message queue.
   *
   * @param r to remove
   */
  void cancel(Runnable r);

}
