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
import io.reactivex.ObservableEmitter;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

final class CallEnqueueObservable {

  public static <T> Observable<Response<T>> create(Call<T> originalCall) {
    return Observable.create(emitter -> {
      // Since Call is a one-shot type, clone it for each new observer.
      Call<T> call = originalCall.clone();
      CallCallback<T> callback = new CallCallback<>(call, emitter);
      emitter.setDisposable(callback);
      if (!callback.isDisposed()) {
        call.enqueue(callback);
      }
    });
  }

  private static final class CallCallback<T> implements Disposable, Callback<T> {
    private final Call<?> call;
    private final ObservableEmitter<Response<T>> emitter;
    private volatile boolean disposed;
    boolean terminated = false;

    CallCallback(Call<?> call, ObservableEmitter<Response<T>> emitter) {
      this.call = call;
      this.emitter = emitter;
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
      if (disposed) return;

      try {
        emitter.onNext(response);

        if (!disposed) {
          terminated = true;
          emitter.onComplete();
        }
      } catch (Throwable t) {
        Exceptions.throwIfFatal(t);
        if (terminated) {
          RxJavaPlugins.onError(t);
        } else if (!disposed) {
          try {
            emitter.onError(t);
          } catch (Throwable inner) {
            Exceptions.throwIfFatal(inner);
            RxJavaPlugins.onError(new CompositeException(t, inner));
          }
        }
      }
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
      if (call.isCanceled()) return;

      try {
        emitter.onError(t);
      } catch (Throwable inner) {
        Exceptions.throwIfFatal(inner);
        RxJavaPlugins.onError(new CompositeException(t, inner));
      }
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
