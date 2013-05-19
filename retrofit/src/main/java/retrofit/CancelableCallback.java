package retrofit;

/** A {@link Callback} that can be cancelled. */
public abstract class CancelableCallback<T> implements Callback<T> {
  private CallbackRunnable<T> runnable;

  void setRunnable(CallbackRunnable<T> runnable) {
    if (this.runnable != null) {
      throw new IllegalStateException(
          "Cancelable callback must not be used in two requests at the same time.");
    }
    this.runnable = runnable;
  }

  /**
   * Cancel the request. Calls to this method will not kill connections that are in flight but will
   * free any resources associated with them.
   */
  public final void cancel() {
    if (runnable != null) {
      runnable.cancel();
      runnable = null;
      cancelled();
    }
  }

  public void cancelled() {
  }
}
