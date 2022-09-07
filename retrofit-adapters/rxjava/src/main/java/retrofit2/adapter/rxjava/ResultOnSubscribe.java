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
import rx.exceptions.OnCompletedFailedException;
import rx.exceptions.OnErrorFailedException;
import rx.exceptions.OnErrorNotImplementedException;
import rx.plugins.RxJavaPlugins;

final class ResultOnSubscribe<T> implements OnSubscribe<Result<T>> {
  private final OnSubscribe<Response<T>> upstream;

  ResultOnSubscribe(OnSubscribe<Response<T>> upstream) {
    this.upstream = upstream;
  }

  @Override
  public void call(Subscriber<? super Result<T>> subscriber) {
    upstream.call(new ResultSubscriber<T>(subscriber));
  }

  private static class ResultSubscriber<R> extends Subscriber<Response<R>> {
    private final Subscriber<? super Result<R>> subscriber;

    ResultSubscriber(Subscriber<? super Result<R>> subscriber) {
      super(subscriber);
      this.subscriber = subscriber;
    }

    @Override
    public void onNext(Response<R> response) {
      subscriber.onNext(Result.response(response));
    }

    @Override
    public void onError(Throwable throwable) {
      try {
        subscriber.onNext(Result.error(throwable));
      } catch (Throwable t) {
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
      subscriber.onCompleted();
    }

    @Override
    public void onCompleted() {
      subscriber.onCompleted();
    }
  }
}
