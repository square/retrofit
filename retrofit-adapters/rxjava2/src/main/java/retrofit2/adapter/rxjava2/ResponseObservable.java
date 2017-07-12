/*
 * Copyright (C) 2015 Square, Inc.
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
package retrofit2.adapter.rxjava2;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;
import retrofit2.Response;


/**
 * @author JSpiner (jspiner@naver.com)
 */
final class ResponseObservable<T> extends Observable<Response<T>> {
  private final Observable<Response<T>> upstream;
  ResponseObservable(Observable<Response<T>> upstream) {
    this.upstream = upstream;
  }

  @Override protected void subscribeActual(Observer<? super Response<T>> observer) {
    upstream.subscribe(new ResponseObservable.ResponseObserver<T>(observer));
  }

  private static class ResponseObserver<R> implements Observer<Response<R>> {
    private final Observer<? super Response<R>> observer;

    ResponseObserver(Observer<? super Response<R>> observer) {
      this.observer = observer;
    }

    @Override public void onSubscribe(Disposable disposable) {
      observer.onSubscribe(disposable);
    }

    @Override public void onNext(Response<R> response) {
      observer.onNext(response);
    }

    @Override public void onComplete() {
      observer.onComplete();
    }

    @Override public void onError(Throwable throwable) {
      try {
        observer.onError(throwable);
      } catch (Throwable inner) {
        Exceptions.throwIfFatal(inner);
        RxJavaPlugins.onError(new CompositeException(throwable, inner));
      }
      return;
    }
  }
}
