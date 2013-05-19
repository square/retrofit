// Copyright 2013 Square, Inc.
package retrofit;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import retrofit.client.Header;
import retrofit.client.Response;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class CallbackRunnableTest {
  private Response response;
  private Object responseBody;

  @Before public void setUp() {
    response = new Response(200, "OK", new ArrayList<Header>(), null);
    responseBody = new Object();
  }

  @Test public void responsePassedToSuccess() {
    Callback<Object> callback = mock(Callback.class);
    CallbackRunnable<Object> runnable =
        new CallbackRunnable<Object>(callback, new Utils.SynchronousExecutor()) {
          @Override ResponseWrapper obtainResponse() {
            return new ResponseWrapper(response, responseBody);
          }
        };

    runnable.run();

    verify(callback).success(responseBody, response);
  }

  @Test public void errorPassedToFailure() {
    final RetrofitError exception = RetrofitError.unexpectedError("", null);
    Callback<Object> callback = mock(Callback.class);
    CallbackRunnable<Object> runnable =
        new CallbackRunnable<Object>(callback, new Utils.SynchronousExecutor()) {
          @Override ResponseWrapper obtainResponse() {
            throw exception;
          }
        };

    runnable.run();

    verify(callback).failure(exception);
  }

  @Test public void recognizesCancelableCallback() throws Exception {
    CancelableCallback<Object> callback = mock(CancelableCallback.class);
    CallbackRunnable<Object> runnable = new CallbackRunnable<Object>(callback, null) {
      @Override ResponseWrapper obtainResponse() {
        return null;
      }
    };

    verify(callback).setRunnable(runnable);
  }

  @Test public void cancelDoesNotRun() throws Exception {
    CancelableCallback<Object> callback = new CancelableCallback<Object>() {
      @Override public void success(Object o, Response response) {
        throw new AssertionError();
      }

      @Override public void failure(RetrofitError error) {
        throw new AssertionError();
      }
    };
    Executor executor = mock(Executor.class);
    CallbackRunnable<Object> runnable = new CallbackRunnable<Object>(callback, executor) {
      @Override ResponseWrapper obtainResponse() {
        throw new AssertionError();
      }
    };

    callback.cancel();
    runnable.run();

    // No explicit assertion. We are verifying that obtainResponse does not get called on the above
    // runnable and that success or failure does not get called on the callback. If this happened
    // an exception would be thrown.
  }

  @Test public void cancelDuringRequestDoesNotContinue() throws Exception {
    final CancelableCallback<Object> callback = new CancelableCallback<Object>() {
      @Override public void success(Object o, Response response) {
        throw new AssertionError();
      }

      @Override public void failure(RetrofitError error) {
        throw new AssertionError();
      }
    };
    Executor executor = mock(Executor.class);
    CallbackRunnable<Object> runnable = new CallbackRunnable<Object>(callback, executor) {
      @Override ResponseWrapper obtainResponse() {
        callback.cancel();
        return null;
      }
    };

    runnable.run();

    verifyZeroInteractions(executor);

    // We are also verifying that success or failure does not get called on the above callback. If
    // this happened an exception would be thrown.
  }

  @Test public void cancelAfterResponseDoesNotInvokeCallback() throws Exception {
    final CancelableCallback<Object> callback = new CancelableCallback<Object>() {
      @Override public void success(Object o, Response response) {
        throw new AssertionError();
      }

      @Override public void failure(RetrofitError error) {
        throw new AssertionError();
      }
    };
    TestExecutor executor = new TestExecutor();
    CallbackRunnable<Object> runnable = new CallbackRunnable<Object>(callback, executor) {
      @Override ResponseWrapper obtainResponse() {
        return new ResponseWrapper(response, responseBody);
      }
    };

    runnable.run();
    assertThat(executor.runnables).hasSize(1);

    callback.cancel();
    executor.runNextRunnable();

    // No explicit assertion. We are verifying that success or failure does not get called on the
    // above callback. If this happened an exception would be thrown.
  }
}
