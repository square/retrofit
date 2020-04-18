/*
 * Copyright (C) 2016 Jake Wharton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2.adapter.rxjava;

import java.util.concurrent.atomic.AtomicInteger;
import retrofit2.Call;
import retrofit2.Response;
import rx.Producer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.exceptions.Exceptions;
import rx.exceptions.OnCompletedFailedException;
import rx.exceptions.OnErrorFailedException;
import rx.exceptions.OnErrorNotImplementedException;
import rx.plugins.RxJavaPlugins;

final class CallArbiter<T> extends AtomicInteger implements Subscription, Producer {
  private static final int STATE_WAITING = 0;
  private static final int STATE_REQUESTED = 1;
  private static final int STATE_HAS_RESPONSE = 2;
  private static final int STATE_TERMINATED = 3;

  private final Call<T> call;
  private final Subscriber<? super Response<T>> subscriber;

  private volatile boolean unsubscribed;
  private volatile Response<T> response;

  CallArbiter(Call<T> call, Subscriber<? super Response<T>> subscriber) {
    super(STATE_WAITING);

    this.call = call;
    this.subscriber = subscriber;
  }

  @Override
  public void unsubscribe() {
    unsubscribed = true;
    call.cancel();
  }

  @Override
  public boolean isUnsubscribed() {
    return unsubscribed;
  }

  @Override
  public void request(long amount) {
    if (amount == 0) {
      return;
    }
    while (true) {
      int state = get();
      switch (state) {
        case STATE_WAITING:
          if (compareAndSet(STATE_WAITING, STATE_REQUESTED)) {
            return;
          }
          break; // State transition failed. Try again.

        case STATE_HAS_RESPONSE:
          if (compareAndSet(STATE_HAS_RESPONSE, STATE_TERMINATED)) {
            deliverResponse(response);
            return;
          }
          break; // State transition failed. Try again.

        case STATE_REQUESTED:
        case STATE_TERMINATED:
          return; // Nothing to do.

        default:
          throw new IllegalStateException("Unknown state: " + state);
      }
    }
  }

  void emitResponse(Response<T> response) {
    while (true) {
      int state = get();
      switch (state) {
        case STATE_WAITING:
          this.response = response;
          if (compareAndSet(STATE_WAITING, STATE_HAS_RESPONSE)) {
            return;
          }
          break; // State transition failed. Try again.

        case STATE_REQUESTED:
          if (compareAndSet(STATE_REQUESTED, STATE_TERMINATED)) {
            deliverResponse(response);
            return;
          }
          break; // State transition failed. Try again.

        case STATE_HAS_RESPONSE:
        case STATE_TERMINATED:
          throw new AssertionError();

        default:
          throw new IllegalStateException("Unknown state: " + state);
      }
    }
  }

  private void deliverResponse(Response<T> response) {
    try {
      if (!isUnsubscribed()) {
        subscriber.onNext(response);
      }
    } catch (OnCompletedFailedException
        | OnErrorFailedException
        | OnErrorNotImplementedException e) {
      RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
      return;
    } catch (Throwable t) {
      Exceptions.throwIfFatal(t);
      try {
        subscriber.onError(t);
      } catch (OnCompletedFailedException
          | OnErrorFailedException
          | OnErrorNotImplementedException e) {
        RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
      } catch (Throwable inner) {
        Exceptions.throwIfFatal(inner);
        CompositeException composite = new CompositeException(t, inner);
        RxJavaPlugins.getInstance().getErrorHandler().handleError(composite);
      }
      return;
    }
    try {
      if (!isUnsubscribed()) {
        subscriber.onCompleted();
      }
    } catch (OnCompletedFailedException
        | OnErrorFailedException
        | OnErrorNotImplementedException e) {
      RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
    } catch (Throwable t) {
      Exceptions.throwIfFatal(t);
      RxJavaPlugins.getInstance().getErrorHandler().handleError(t);
    }
  }

  void emitError(Throwable t) {
    set(STATE_TERMINATED);

    if (!isUnsubscribed()) {
      try {
        subscriber.onError(t);
      } catch (OnCompletedFailedException
          | OnErrorFailedException
          | OnErrorNotImplementedException e) {
        RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
      } catch (Throwable inner) {
        Exceptions.throwIfFatal(inner);
        CompositeException composite = new CompositeException(t, inner);
        RxJavaPlugins.getInstance().getErrorHandler().handleError(composite);
      }
    }
  }
}
