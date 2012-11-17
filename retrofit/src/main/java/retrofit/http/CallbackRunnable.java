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
  private final Converter converter;
  private final Executor callbackExecutor;

  CallbackRunnable(Callback<R, CE, SE> callback, Converter converter, Executor callbackExecutor) {
    this.callback = callback;
    this.converter = converter;
    this.callbackExecutor = callbackExecutor;
  }

  @SuppressWarnings("unchecked")
  @Override public final void run() {
    int statusCode = -1; // Used when a ConversionException is thrown below.
    try { // For catching conversion exceptions which occur during deserialization.
      try { // For catching any exceptions which occur during HTTP call.
        final Object response = obtainResponse();
        callbackExecutor.execute(new Runnable() {
          @Override public void run() {
            callback.call((R) response);
          }
        });
      } catch (final ClientHttpException ce) {
        statusCode = ce.getStatus();
        final CE response = (CE) converter.to(ce.getResponse(), ce.getResponseType());
        callbackExecutor.execute(new Runnable() {
          @Override public void run() {
            callback.clientError(response, ce.getStatus());
          }
        });
      } catch (final ServerHttpException se) {
        statusCode = se.getStatus();
        final SE response = (SE) converter.to(se.getResponse(), se.getResponseType());
        callbackExecutor.execute(new Runnable() {
          @Override public void run() {
            callback.serverError(response, se.getStatus());
          }
        });
      } catch (final UnauthorizedHttpException ue) {
        statusCode = ue.getStatus();
        final CE response = (CE) converter.to(ue.getResponse(), ue.getResponseType());
        callbackExecutor.execute(new Runnable() {
          @Override public void run() {
            callback.sessionExpired(response);
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
    } catch (ConversionException e) {
      final int finalStatusCode = statusCode;
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          callback.serverError(null, finalStatusCode);
        }
      });
    }
  }

  public abstract Object obtainResponse();
}
