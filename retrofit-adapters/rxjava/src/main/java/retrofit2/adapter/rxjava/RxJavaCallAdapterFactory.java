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
package retrofit2.adapter.rxjava;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

/**
 * A {@linkplain CallAdapter.Factory call adapter} which uses RxJava for creating observables.
 * <p>
 * Adding this class to {@link Retrofit} allows you to return {@link Observable} from service
 * methods.
 * <pre>{@code
 * interface MyService {
 *   &#64;GET("user/me")
 *   Observable<User> getUser()
 * }
 * }</pre>
 */
public final class RxJavaCallAdapterFactory extends CallAdapter.Factory {
  /**
   * Returns an instance which creates synchronous observables that do not operate on any scheduler
   * by default.
   */
  public static RxJavaCallAdapterFactory create() {
    return new RxJavaCallAdapterFactory(null);
  }

  /**
   * Returns an instance which creates synchronous observables that
   * {@linkplain Observable#subscribeOn(Scheduler) subscribe on} {@code scheduler} by default.
   */
  public static RxJavaCallAdapterFactory createWithScheduler(Scheduler scheduler) {
    if (scheduler == null) throw new NullPointerException("scheduler == null");
    return new RxJavaCallAdapterFactory(scheduler);
  }

  private final Scheduler scheduler;

  private RxJavaCallAdapterFactory(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  @Override
  public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
    Class<?> rawType = getRawType(returnType);
    String canonicalName = rawType.getCanonicalName();
    boolean isSingle = "rx.Single".equals(canonicalName);
    boolean isCompletable = "rx.Completable".equals(canonicalName);
    if (rawType != Observable.class && !isSingle && !isCompletable) {
      return null;
    }
    if (!isCompletable && !(returnType instanceof ParameterizedType)) {
      String name = isSingle ? "Single" : "Observable";
      throw new IllegalStateException(name + " return type must be parameterized"
          + " as " + name + "<Foo> or " + name + "<? extends Foo>");
    }

    if (isCompletable) {
      // Add Completable-converter wrapper from a separate class. This defers classloading such that
      // regular Observable operation can be leveraged without relying on this unstable RxJava API.
      // Note that this has to be done separately since Completable doesn't have a parametrized
      // type.
      return CompletableHelper.createCallAdapter(scheduler);
    }

    CallAdapter<Observable<?>> callAdapter = getCallAdapter(returnType, scheduler);
    if (isSingle) {
      // Add Single-converter wrapper from a separate class. This defers classloading such that
      // regular Observable operation can be leveraged without relying on this unstable RxJava API.
      return SingleHelper.makeSingle(callAdapter);
    }
    return callAdapter;
  }

  private CallAdapter<Observable<?>> getCallAdapter(Type returnType, Scheduler scheduler) {
    Type observableType = getParameterUpperBound(0, (ParameterizedType) returnType);
    Class<?> rawObservableType = getRawType(observableType);
    if (rawObservableType == Response.class) {
      if (!(observableType instanceof ParameterizedType)) {
        throw new IllegalStateException("Response must be parameterized"
            + " as Response<Foo> or Response<? extends Foo>");
      }
      Type responseType = getParameterUpperBound(0, (ParameterizedType) observableType);
      return new ResponseCallAdapter(responseType, scheduler);
    }

    if (rawObservableType == Result.class) {
      if (!(observableType instanceof ParameterizedType)) {
        throw new IllegalStateException("Result must be parameterized"
            + " as Result<Foo> or Result<? extends Foo>");
      }
      Type responseType = getParameterUpperBound(0, (ParameterizedType) observableType);
      return new ResultCallAdapter(responseType, scheduler);
    }

    return new SimpleCallAdapter(observableType, scheduler);
  }

  static final class CallOnSubscribe<T> implements Observable.OnSubscribe<Response<T>> {
    private final Call<T> originalCall;

    CallOnSubscribe(Call<T> originalCall) {
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

      try {
        Response<T> response = call.execute();
        if (!subscriber.isUnsubscribed()) {
          subscriber.onNext(response);
        }
      } catch (Throwable t) {
        Exceptions.throwIfFatal(t);
        if (!subscriber.isUnsubscribed()) {
          subscriber.onError(t);
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
    private final Scheduler scheduler;

    ResponseCallAdapter(Type responseType, Scheduler scheduler) {
      this.responseType = responseType;
      this.scheduler = scheduler;
    }

    @Override public Type responseType() {
      return responseType;
    }

    @Override public <R> Observable<Response<R>> adapt(Call<R> call) {
      Observable<Response<R>> observable = Observable.create(new CallOnSubscribe<>(call));
      if (scheduler != null) {
        return observable.subscribeOn(scheduler);
      }
      return observable;
    }
  }

  static final class SimpleCallAdapter implements CallAdapter<Observable<?>> {
    private final Type responseType;
    private final Scheduler scheduler;

    SimpleCallAdapter(Type responseType, Scheduler scheduler) {
      this.responseType = responseType;
      this.scheduler = scheduler;
    }

    @Override public Type responseType() {
      return responseType;
    }

    @Override public <R> Observable<R> adapt(Call<R> call) {
      Observable<R> observable = Observable.create(new CallOnSubscribe<>(call)) //
          .flatMap(new Func1<Response<R>, Observable<R>>() {
            @Override public Observable<R> call(Response<R> response) {
              if (response.isSuccessful()) {
                return Observable.just(response.body());
              }
              return Observable.error(new HttpException(response));
            }
          });
      if (scheduler != null) {
        return observable.subscribeOn(scheduler);
      }
      return observable;
    }
  }

  static final class ResultCallAdapter implements CallAdapter<Observable<?>> {
    private final Type responseType;
    private final Scheduler scheduler;

    ResultCallAdapter(Type responseType, Scheduler scheduler) {
      this.responseType = responseType;
      this.scheduler = scheduler;
    }

    @Override public Type responseType() {
      return responseType;
    }

    @Override public <R> Observable<Result<R>> adapt(Call<R> call) {
      Observable<Result<R>> observable = Observable.create(new CallOnSubscribe<>(call)) //
          .map(new Func1<Response<R>, Result<R>>() {
            @Override public Result<R> call(Response<R> response) {
              return Result.response(response);
            }
          }).onErrorReturn(new Func1<Throwable, Result<R>>() {
            @Override public Result<R> call(Throwable throwable) {
              return Result.error(throwable);
            }
          });
      if (scheduler != null) {
        return observable.subscribeOn(scheduler);
      }
      return observable;
    }
  }
}
