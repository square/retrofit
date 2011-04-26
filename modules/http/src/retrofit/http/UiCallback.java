// Copyright 2010 Square, Inc.
package retrofit.http;

import retrofit.core.Callback;
import retrofit.core.MainThread;

/**
 * Executes callback methods in the UI thread.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public final class UiCallback<T> implements Callback<T> {

  final Callback<T> delegate;
  final MainThread mainThread;

  UiCallback(Callback<T> delegate, MainThread mainThread) {
    this.delegate = delegate;
    this.mainThread = mainThread;
  }

  public static <T> UiCallback<T> create(Callback<T> delegate,
      MainThread mainThread) {
    return new UiCallback<T>(delegate, mainThread);
  }

  public void call(final T t) {
    mainThread.execute(new Runnable() {
      public void run() {
        delegate.call(t);
      }
    });
  }

  public void sessionExpired() {
    mainThread.execute(new Runnable() {
      public void run() {
        delegate.sessionExpired();
      }
    });
  }

  public void networkError() {
    mainThread.execute(new Runnable() {
      public void run() {
        delegate.networkError();
      }
    });
  }

  public void clientError(final T response) {
    mainThread.execute(new Runnable() {
      public void run() {
        delegate.clientError(response);
      }
    });
  }

  public void serverError(final String message) {
    mainThread.execute(new Runnable() {
      public void run() {
        delegate.serverError(message);
      }
    });
  }

  public void unexpectedError(final Throwable t) {
    mainThread.execute(new Runnable() {
      public void run() {
        delegate.unexpectedError(t);
      }
    });
  }
}
