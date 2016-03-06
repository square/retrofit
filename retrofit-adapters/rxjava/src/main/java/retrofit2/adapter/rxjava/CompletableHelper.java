/*
 * Copyright (C) 2016 Square, Inc.
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

import java.lang.reflect.Type;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import rx.Completable;
import rx.Completable.CompletableOnSubscribe;
import rx.Completable.CompletableSubscriber;
import rx.Scheduler;
import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

final class CompletableHelper {
  static CallAdapter<Completable> createCallAdapter(Scheduler scheduler) {
    return new CompletableCallAdapter(scheduler);
  }

  private static final class CompletableCallOnSubscribe implements CompletableOnSubscribe {
    private final Call originalCall;

    CompletableCallOnSubscribe(Call originalCall) {
      this.originalCall = originalCall;
    }

    @Override public void call(CompletableSubscriber subscriber) {
      // Since Call is a one-shot type, clone it for each new subscriber.
      final Call call = originalCall.clone();

      // Attempt to cancel the call if it is still in-flight on unsubscription.
      Subscription subscription = Subscriptions.create(new Action0() {
        @Override public void call() {
          call.cancel();
        }
      });
      subscriber.onSubscribe(subscription);

      try {
        Response response = call.execute();
        if (!subscription.isUnsubscribed()) {
          if (response.isSuccessful()) {
            subscriber.onCompleted();
          } else {
            subscriber.onError(new HttpException(response));
          }
        }
      } catch (Throwable t) {
        Exceptions.throwIfFatal(t);
        if (!subscription.isUnsubscribed()) {
          subscriber.onError(t);
        }
      }
    }
  }

  static class CompletableCallAdapter implements CallAdapter<Completable> {
    private final Scheduler scheduler;

    CompletableCallAdapter(Scheduler scheduler) {
      this.scheduler = scheduler;
    }

    @Override public Type responseType() {
      return Void.class;
    }

    @Override public Completable adapt(Call call) {
      Completable completable = Completable.create(new CompletableCallOnSubscribe(call));
      if (scheduler != null) {
        return completable.subscribeOn(scheduler);
      }
      return completable;
    }
  }
}
