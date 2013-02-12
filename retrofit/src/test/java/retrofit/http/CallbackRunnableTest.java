// Copyright 2013 Square, Inc.
package retrofit.http;

import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static retrofit.http.Utils.SynchronousExecutor;

public class CallbackRunnableTest {
  private Executor executor = spy(new SynchronousExecutor());
  private CallbackRunnable<Object> callbackRunnable;
  private Callback<Object> callback;

  @Before public void setUp() {
    callback = mock(Callback.class);
    callbackRunnable = spy(new CallbackRunnable<Object>(callback, executor) {
      @Override public Object obtainResponse() {
        return null; // Must be mocked.
      }
    });
  }

  @Test public void responsePassedToSuccess() {
    Object response = new Object();
    when(callbackRunnable.obtainResponse()).thenReturn(response);

    callbackRunnable.run();

    verify(executor).execute(any(Runnable.class));
    verify(callback).success(same(response));
  }

  @Test public void errorPassedToFailure() {
    RetrofitError exception = RetrofitError.unexpectedError("", null);
    when(callbackRunnable.obtainResponse()).thenThrow(exception);

    callbackRunnable.run();

    verify(executor).execute(any(Runnable.class));
    verify(callback).failure(same(exception));
  }
}
