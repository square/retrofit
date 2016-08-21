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

import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Call;
import retrofit2.Response;
import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.exceptions.Exceptions;
import rx.plugins.RxJavaPlugins;

final class CallOnSubscribe<T> implements OnSubscribe<Response<T>> {
  private final Call<T> originalCall;

  CallOnSubscribe(Call<T> originalCall) {
    this.originalCall = originalCall;
  }

  @Override public void call(Subscriber<? super Response<T>> subscriber) {
    // Since Call is a one-shot type, clone it for each new subscriber.
    Call<T> call = originalCall.clone();
    CallArbiter<T> arbiter = new CallArbiter<>(call, subscriber);
    subscriber.add(arbiter);
    subscriber.setProducer(arbiter);
  }

  private static final class CallArbiter<T> extends AtomicBoolean
      implements Subscription, Producer {
    private final Call<T> call;
    private final Subscriber<? super Response<T>> subscriber;

    CallArbiter(Call<T> call, Subscriber<? super Response<T>> subscriber) {
      this.call = call;
      this.subscriber = subscriber;
    }

    @Override public void unsubscribe() {
      call.cancel();
    }

    @Override public boolean isUnsubscribed() {
      return call.isCanceled();
    }

    @Override public void request(long amount) {
      if (amount == 0 || !compareAndSet(false, true)) {
        return;
      }

      boolean terminated = false;
      try {
        Response<T> response = call.execute();
        if (!call.isCanceled()) {
          subscriber.onNext(response);
        }
        if (!call.isCanceled()) {
          terminated = true;
          subscriber.onCompleted();
        }
      } catch (Throwable t) {
        Exceptions.throwIfFatal(t);
        if (terminated) {
          RxJavaPlugins.getInstance().getErrorHandler().handleError(t);
        } else if (!call.isCanceled()) {
          try {
            subscriber.onError(t);
          } catch (Throwable inner) {
            Exceptions.throwIfFatal(inner);
            CompositeException composite = new CompositeException(t, inner);
            RxJavaPlugins.getInstance().getErrorHandler().handleError(composite);
          }
        }
      }
    }
  }
}
