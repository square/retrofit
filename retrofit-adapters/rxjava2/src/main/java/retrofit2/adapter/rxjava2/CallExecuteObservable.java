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
package retrofit2.adapter.rxjava2;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;
import retrofit2.Call;
import retrofit2.Response;

final class CallExecuteObservable {

  static <T> Observable<Response<T>> create(Call<T> originalCall) {
    return Observable.create(emitter -> {
      // Since Call is a one-shot type, clone it for each new observer.
      Call<T> call = originalCall.clone();
      CallDisposable disposable = new CallDisposable(call);
      emitter.setDisposable(disposable);
      if (disposable.isDisposed()) {
        return;
      }

      boolean terminated = false;
      try {
        Response<T> response = call.execute();
        if (!disposable.isDisposed()) {
          emitter.onNext(response);
        }
        if (!disposable.isDisposed()) {
          terminated = true;
          emitter.onComplete();
        }
      } catch (Throwable t) {
        Exceptions.throwIfFatal(t);
        if (terminated) {
          RxJavaPlugins.onError(t);
        } else if (!disposable.isDisposed()) {
          try {
            emitter.onError(t);
          } catch (Throwable inner) {
            Exceptions.throwIfFatal(inner);
            RxJavaPlugins.onError(new CompositeException(t, inner));
          }
        }
      }
    });
  }

  private static final class CallDisposable implements Disposable {
    private final Call<?> call;
    private volatile boolean disposed;

    CallDisposable(Call<?> call) {
      this.call = call;
    }

    @Override
    public void dispose() {
      disposed = true;
      call.cancel();
    }

    @Override
    public boolean isDisposed() {
      return disposed;
    }
  }
}
