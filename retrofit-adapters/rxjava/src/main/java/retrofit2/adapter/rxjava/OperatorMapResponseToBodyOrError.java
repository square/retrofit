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

import retrofit2.Response;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * A version of {@link Observable#map(Func1)} which lets us trigger {@code onError} without having
 * to use {@link Observable#flatMap(Func1)} which breaks producer requests from propagating.
 */
final class OperatorMapResponseToBodyOrError<T> implements Operator<T, Response<T>> {
  private static final OperatorMapResponseToBodyOrError<Object> INSTANCE =
      new OperatorMapResponseToBodyOrError<>();

  @SuppressWarnings("unchecked") // Safe because of erasure.
  static <R> OperatorMapResponseToBodyOrError<R> instance() {
    return (OperatorMapResponseToBodyOrError<R>) INSTANCE;
  }

  @Override public Subscriber<? super Response<T>> call(final Subscriber<? super T> child) {
    return new Subscriber<Response<T>>(child) {
      @Override public void onNext(Response<T> response) {
        if (response.isSuccessful()) {
          child.onNext(response.body());
        } else {
          child.onError(new HttpException(response));
        }
      }

      @Override public void onCompleted() {
        child.onCompleted();
      }

      @Override public void onError(Throwable e) {
        child.onError(e);
      }
    };
  }
}
