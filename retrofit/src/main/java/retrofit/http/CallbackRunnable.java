// Copyright 2012 Square, Inc.
package retrofit.http;

import retrofit.http.RestException.ClientHttpException;
import retrofit.http.RestException.NetworkException;
import retrofit.http.RestException.ServerHttpException;
import retrofit.http.RestException.UnauthorizedHttpException;
import retrofit.http.RestException.UnexpectedException;

import java.util.concurrent.Executor;

/**
 * A {@link Runnable} executed on a background thread to invoke {@link #obtainResponse()} which performs an HTTP
 * request. The response of the request, whether it be an object or exception, is then marshaled to the supplied
 * {@link Executor} in the form of a method call on a {@link Callback}.
 */
abstract class CallbackRunnable<R, CE, SE> implements Runnable {
  private final Callback<R, CE, SE> callback;
  private final Executor callbackExecutor;

  CallbackRunnable(Callback<R, CE, SE> callback, Executor callbackExecutor) {
    this.callback = callback;
    this.callbackExecutor = callbackExecutor;
  }

  @SuppressWarnings("unchecked")
  @Override public final void run() {
    try {
      final Object response = obtainResponse();
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          callback.call((R) response);
        }
      });
    } catch (final ClientHttpException ce) {
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          callback.clientError((CE) ce.getResponse(), ce.getStatus());
        }
      });
    } catch (final ServerHttpException se) {
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          callback.serverError((SE) se.getResponse(), se.getStatus());
        }
      });
    } catch (final UnauthorizedHttpException ue) {
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          callback.sessionExpired((CE) ue.getResponse());
        }
      });
    } catch (final NetworkException ne) {
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          callback.networkError();
        }
      });
    } catch (final UnexpectedException ue) {
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          callback.unexpectedError(ue.getCause());
        }
      });
    } catch (final Throwable t) {
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          callback.unexpectedError(t);
        }
      });
    }
  }

  public abstract Object obtainResponse();
}
