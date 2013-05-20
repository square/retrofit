package retrofit;

import org.junit.Test;
import retrofit.client.Response;

import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class CancelableCallbackTest {
  @Test public void cancelCallbackOnlyInvokedOnce() {
    CallbackRunnable runnable = mock(CallbackRunnable.class);
    CancelableCallback callback = new CancelableCallback() {
      @Override public void success(Object o, Response response) {
        throw new AssertionError();
      }

      @Override public void failure(RetrofitError error) {
        throw new AssertionError();
      }

      boolean first = true;

      @Override public void cancelled() {
        if (first) {
          first = false;
        } else {
          fail("Cancelled called twice.");
        }
      }
    };

    callback.setRunnable(runnable);
    callback.cancel();
    callback.cancel();
  }

  @Test public void callbackCanOnlyBeUsedOnce() {
    CallbackRunnable runnable1= mock(CallbackRunnable.class);
    CallbackRunnable runnable2 = mock(CallbackRunnable.class);
    CancelableCallback callback = new CancelableCallback() {
      @Override public void success(Object o, Response response) {
      }

      @Override public void failure(RetrofitError error) {
      }
    };

    callback.setRunnable(runnable1);

    try {
      callback.setRunnable(runnable2);
      fail("Cancelable callback can not be used in two simultaneous requests.");
    } catch (IllegalStateException expected) {
    }
  }
}
