// Copyright 2012 Square, Inc.
package retrofit.http;

/**
 * Executes callback methods in the UI thread.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public final class UiCallback<T, CE, SE> implements Callback<T, CE, SE> {

  final Callback<T, CE, SE> delegate;
  final MainThread mainThread;

  UiCallback(Callback<T, CE, SE> delegate, MainThread mainThread) {
    this.delegate = delegate;
    this.mainThread = mainThread;
  }

  public static <T, CE, SE> UiCallback<T, CE, SE> create(Callback<T, CE, SE> delegate, MainThread mainThread) {
    return new UiCallback<T, CE, SE>(delegate, mainThread);
  }

  public void call(final T response) {
    mainThread.execute(new Runnable() {
      public void run() {
        delegate.call(response);
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

  public void clientError(final CE response, final int statusCode) {
    mainThread.execute(new Runnable() {
      public void run() {
        delegate.clientError(response, statusCode);
      }
    });
  }

  public void serverError(final SE response, final int statusCode) {
    mainThread.execute(new Runnable() {
      public void run() {
        delegate.serverError(response, statusCode);
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
