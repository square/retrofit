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
package retrofit;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

/**
 * TODO docs
 */
public final class ObservableCallAdapterFactory implements CallAdapter.Factory {
  /**
   * TODO
   */
  public static ObservableCallAdapterFactory create() {
    return new ObservableCallAdapterFactory();
  }

  private ObservableCallAdapterFactory() {
  }

  @Override public String toString() {
    return getClass().getSimpleName();
  }

  @Override public CallAdapter<?> get(Type returnType) {
    if (Utils.getRawType(returnType) != Observable.class) {
      return null;
    }
    if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalStateException("Observable return type must be parameterized"
          + " as Observable<Foo> or Observable<? extends Foo>");
    }

    Type observableType = Utils.getSingleParameterUpperBound((ParameterizedType) returnType);
    Class<?> rawObservableType = Utils.getRawType(observableType);

    if (rawObservableType == Response.class) {
      if (!(observableType instanceof ParameterizedType)) {
        throw new IllegalStateException("Response must be parameterized"
            + " as Response<Foo> or Response<? extends Foo>");
      }
      Type responseType = Utils.getSingleParameterUpperBound((ParameterizedType) observableType);
      return new ResponseCallAdapter<>(responseType);
    }

    if (rawObservableType == Result.class) {
      if (!(observableType instanceof ParameterizedType)) {
        throw new IllegalStateException("Result must be parameterized"
            + " as Result<Foo> or Result<? extends Foo>");
      }
      Type responseType = Utils.getSingleParameterUpperBound((ParameterizedType) observableType);
      return new ResultCallAdapter<>(responseType);
    }

    return new SimpleCallAdapter(observableType);
  }

  static final class CallOnSubcribe<T> implements Observable.OnSubscribe<Response<T>> {
    private final Call<T> originalCall;

    private CallOnSubcribe(Call<T> originalCall) {
      this.originalCall = originalCall;
    }

    @Override public void call(final Subscriber<? super Response<T>> subscriber) {
      // Since Call is a one-shot type, clone it for each new subscriber.
      final Call<T> call = originalCall.clone();

      // Attempt to cancel the call if it is still in-flight on unsubscription.
      subscriber.add(Subscriptions.create(new Action0() {
        @Override public void call() {
          call.cancel();
        }
      }));

      call.enqueue(new Callback<T>() {
        @Override public void success(Response<T> response) {
          if (subscriber.isUnsubscribed()) {
            return;
          }
          try {
            subscriber.onNext(response);
          } catch (Throwable t) {
            subscriber.onError(t);
            return;
          }
          subscriber.onCompleted();
        }

        @Override public void failure(Throwable t) {
          if (subscriber.isUnsubscribed()) {
            return;
          }
          subscriber.onError(t);
        }
      });
    }
  }

  static final class ResponseCallAdapter<T> implements CallAdapter<T> {
    private final Type responseType;

    ResponseCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override public Type responseType() {
      return responseType;
    }

    @Override public Observable<Response<T>> adapt(Call<T> call) {
      return Observable.create(new CallOnSubcribe<>(call));
    }
  }

  static final class SimpleCallAdapter<T> implements CallAdapter<T> {
    private final Type responseType;

    SimpleCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override public Type responseType() {
      return responseType;
    }

    @Override public Observable<T> adapt(Call<T> call) {
      return Observable.create(new CallOnSubcribe<>(call)) //
          .flatMap(new Func1<Response<T>, Observable<T>>() {
            @Override public Observable<T> call(Response<T> response) {
              if (response.isSuccess()) {
                return Observable.just(response.body());
              }
              return Observable.error(new IOException()); // TODO non-suck message.
            }
          });
    }
  }

  static final class ResultCallAdapter<T> implements CallAdapter<T> {
    private final Type responseType;

    ResultCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override public Type responseType() {
      return responseType;
    }

    @Override public Observable<Result<T>> adapt(Call<T> call) {
      return Observable.create(new CallOnSubcribe<>(call)) //
          .map(new Func1<Response<T>, Result<T>>() {
            @Override public Result<T> call(Response<T> response) {
              return Result.fromResponse(response);
            }
          })
          .onErrorReturn(new Func1<Throwable, Result<T>>() {
            @Override public Result<T> call(Throwable throwable) {
              return Result.fromError(throwable);
            }
          });
    }
  }
}
