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

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

/**
 * TODO docs
 */
public final class RxJavaCallAdapterFactory implements CallAdapter.Factory {
  /**
   * TODO
   */
  public static RxJavaCallAdapterFactory create() {
    return new RxJavaCallAdapterFactory();
  }

  private RxJavaCallAdapterFactory() {
  }

  @Override
  public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
    Class<?> rawType = Utils.getRawType(returnType);
    boolean isSingle = "rx.Single".equals(rawType.getCanonicalName());
    if (rawType != Observable.class && !isSingle) {
      return null;
    }
    if (!(returnType instanceof ParameterizedType)) {
      String name = isSingle ? "Single" : "Observable";
      throw new IllegalStateException(name + " return type must be parameterized"
          + " as " + name + "<Foo> or " + name + "<? extends Foo>");
    }

    CallAdapter<Observable<?>> callAdapter = getCallAdapter(returnType);
    if (isSingle) {
      // Add Single-converter wrapper from a separate class. This defers classloading such that
      // regular Observable operation can be leveraged without relying on this unstable RxJava API.
      return SingleHelper.makeSingle(callAdapter);
    }
    return callAdapter;
  }

  private CallAdapter<Observable<?>> getCallAdapter(Type returnType) {
    Type observableType = Utils.getSingleParameterUpperBound((ParameterizedType) returnType);
    Class<?> rawObservableType = Utils.getRawType(observableType);
    if (rawObservableType == Response.class) {
      if (!(observableType instanceof ParameterizedType)) {
        throw new IllegalStateException("Response must be parameterized"
            + " as Response<Foo> or Response<? extends Foo>");
      }
      Type responseType = Utils.getSingleParameterUpperBound((ParameterizedType) observableType);
      return new ResponseCallAdapter(responseType);
    }

    if (rawObservableType == Result.class) {
      if (!(observableType instanceof ParameterizedType)) {
        throw new IllegalStateException("Result must be parameterized"
            + " as Result<Foo> or Result<? extends Foo>");
      }
      Type responseType = Utils.getSingleParameterUpperBound((ParameterizedType) observableType);
      return new ResultCallAdapter(responseType);
    }

    return new SimpleCallAdapter(observableType);
  }

  static final class CallOnSubscribe<T> implements Observable.OnSubscribe<Result<T>> {
    private final Call<T> originalCall;
    private final Retrofit retrofit;

    private CallOnSubscribe(Call<T> originalCall, Retrofit retrofit) {
      this.originalCall = originalCall;
      this.retrofit = retrofit;
    }

    @Override public void call(final Subscriber<? super Result<T>> subscriber) {
      // Since Call is a one-shot type, clone it for each new subscriber.
      final Call<T> call = originalCall.clone();

      // Attempt to cancel the call if it is still in-flight on unsubscription.
      subscriber.add(Subscriptions.create(new Action0() {
        @Override public void call() {
          call.cancel();
        }
      }));

      try {
        Response<T> response = call.execute();
        if (!subscriber.isUnsubscribed()) {
          subscriber.onNext(Result.response(response, retrofit));
        }
      } catch (Throwable t) {
        Exceptions.throwIfFatal(t);
        if (!subscriber.isUnsubscribed()) {
          subscriber.onNext(Result.<T>error(t));
        }
        return;
      }

      if (!subscriber.isUnsubscribed()) {
        subscriber.onCompleted();
      }
    }
  }

  static final class ResponseCallAdapter implements CallAdapter<Observable<?>> {
    private final Type responseType;

    ResponseCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override public Type responseType() {
      return responseType;
    }

    @Override public <R> Observable<Response<R>> adapt(Call<R> call, Retrofit retrofit) {
      return Observable.create(new CallOnSubscribe<>(call, retrofit))
          .flatMap(new Func1<Result<R>, Observable<Response<R>>>() {
            @Override public Observable<Response<R>> call(Result<R> result) {
              if (result.isError()) {
                return Observable.error(result.error());
              }

              return Observable.just(result.response());
            }
          });
    }
  }

  static final class SimpleCallAdapter implements CallAdapter<Observable<?>> {
    private final Type responseType;

    SimpleCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override public Type responseType() {
      return responseType;
    }

    @Override public <R> Observable<R> adapt(Call<R> call, Retrofit retrofit) {
      return Observable.create(new CallOnSubscribe<>(call, retrofit)) //
          .flatMap(new Func1<Result<R>, Observable<R>>() {
            @Override public Observable<R> call(Result<R> result) {
              if (result.isError()) {
                return Observable.error(result.error());
              }

              Response<R> response = result.response();
              if (response.isSuccess()) {
                return Observable.just(response.body());
              }

              return Observable.error(new HttpException(response));
            }
          });
    }
  }

  static final class ResultCallAdapter implements CallAdapter<Observable<?>> {
    private final Type responseType;

    ResultCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override public Type responseType() {
      return responseType;
    }

    @Override public <R> Observable<Result<R>> adapt(Call<R> call, Retrofit retrofit) {
      return Observable.create(new CallOnSubscribe<>(call, retrofit));
    }
  }
}
