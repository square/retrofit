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

import retrofit2.Response;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.exceptions.CompositeException;
import rx.exceptions.Exceptions;
import rx.plugins.RxJavaPlugins;

final class BodyOnSubscribe<T> implements OnSubscribe<T> {
  private final OnSubscribe<Response<T>> upstream;

  BodyOnSubscribe(OnSubscribe<Response<T>> upstream) {
    this.upstream = upstream;
  }

  @Override public void call(Subscriber<? super T> subscriber) {
    upstream.call(new BodySubscriber<T>(subscriber));
  }

  private static class BodySubscriber<R> extends Subscriber<Response<R>> {
    private final Subscriber<? super R> subscriber;
    /** Indicates whether a terminal event has been sent to {@link #subscriber}. */
    private boolean subscriberTerminated;

    BodySubscriber(Subscriber<? super R> subscriber) {
      super(subscriber);
      this.subscriber = subscriber;
    }

    @Override public void onNext(Response<R> response) {
      if (response.isSuccessful()) {
        subscriber.onNext(response.body());
      } else {
        subscriberTerminated = true;
        Throwable t = new HttpException(response);
        try {
          subscriber.onError(t);
        } catch (Throwable inner) {
          Exceptions.throwIfFatal(inner);
          CompositeException composite = new CompositeException(t, inner);
          RxJavaPlugins.getInstance().getErrorHandler().handleError(composite);
        }
      }
    }

    @Override public void onError(Throwable throwable) {
      if (!subscriberTerminated) {
        subscriber.onError(throwable);
      } else {
        // This should never happen! onNext handles and forwards errors automatically.
        Throwable broken = new AssertionError(
            "This should never happen! Report as a Retrofit bug with the full stacktrace.");
        //noinspection UnnecessaryInitCause Two-arg AssertionError constructor is 1.7+ only.
        broken.initCause(throwable);
        RxJavaPlugins.getInstance().getErrorHandler().handleError(broken);
      }
    }

    @Override public void onCompleted() {
      if (!subscriberTerminated) {
        subscriber.onCompleted();
      }
    }
  }
}
