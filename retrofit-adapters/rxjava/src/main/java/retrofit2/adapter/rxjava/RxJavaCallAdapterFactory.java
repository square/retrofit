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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import retrofit2.CallAdapter;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.Single;

/**
 * A {@linkplain CallAdapter.Factory call adapter} which uses RxJava for creating observables.
 *
 * <p>Adding this class to {@link Retrofit} allows you to return an {@link Observable}, {@link
 * Single}, or {@link Completable} from service methods.
 *
 * <pre><code>
 * interface MyService {
 *   &#64;GET("user/me")
 *   Observable&lt;User&gt; getUser()
 * }
 * </code></pre>
 *
 * There are three configurations supported for the {@code Observable} or {@code Single} type
 * parameter:
 *
 * <ul>
 *   <li>Direct body (e.g., {@code Observable<User>}) calls {@code onNext} with the deserialized
 *       body for 2XX responses and calls {@code onError} with {@link HttpException} for non-2XX
 *       responses and {@link IOException} for network errors.
 *   <li>Response wrapped body (e.g., {@code Observable<Response<User>>}) calls {@code onNext} with
 *       a {@link Response} object for all HTTP responses and calls {@code onError} with {@link
 *       IOException} for network errors
 *   <li>Result wrapped body (e.g., {@code Observable<Result<User>>}) calls {@code onNext} with a
 *       {@link Result} object for all HTTP responses and errors.
 * </ul>
 *
 * <p><em>Note:</em> Support for {@link Single} and {@link Completable} is experimental and subject
 * to backwards-incompatible changes at any time since both of these types are not considered stable
 * by RxJava.
 */
public final class RxJavaCallAdapterFactory extends CallAdapter.Factory {
  /**
   * Returns an instance which creates synchronous observables that do not operate on any scheduler
   * by default.
   */
  public static RxJavaCallAdapterFactory create() {
    return new RxJavaCallAdapterFactory(null, false);
  }

  /** Returns an instance which creates asynchronous observables. */
  public static RxJavaCallAdapterFactory createAsync() {
    return new RxJavaCallAdapterFactory(null, true);
  }

  /**
   * Returns an instance which creates synchronous observables that {@linkplain
   * Observable#subscribeOn(Scheduler) subscribe on} {@code scheduler} by default.
   */
  @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
  public static RxJavaCallAdapterFactory createWithScheduler(Scheduler scheduler) {
    if (scheduler == null) throw new NullPointerException("scheduler == null");
    return new RxJavaCallAdapterFactory(scheduler, false);
  }

  private final @Nullable Scheduler scheduler;
  private final boolean isAsync;

  private RxJavaCallAdapterFactory(@Nullable Scheduler scheduler, boolean isAsync) {
    this.scheduler = scheduler;
    this.isAsync = isAsync;
  }

  @Override
  public @Nullable CallAdapter<?, ?> get(
      Type returnType, Annotation[] annotations, Retrofit retrofit) {
    Class<?> rawType = getRawType(returnType);
    boolean isSingle = rawType == Single.class;
    boolean isCompletable = rawType == Completable.class;
    if (rawType != Observable.class && !isSingle && !isCompletable) {
      return null;
    }

    if (isCompletable) {
      return new RxJavaCallAdapter(Void.class, scheduler, isAsync, false, true, false, true);
    }

    boolean isResult = false;
    boolean isBody = false;
    Type responseType;
    if (!(returnType instanceof ParameterizedType)) {
      String name = isSingle ? "Single" : "Observable";
      throw new IllegalStateException(
          name
              + " return type must be parameterized"
              + " as "
              + name
              + "<Foo> or "
              + name
              + "<? extends Foo>");
    }

    Type observableType = getParameterUpperBound(0, (ParameterizedType) returnType);
    Class<?> rawObservableType = getRawType(observableType);
    if (rawObservableType == Response.class) {
      if (!(observableType instanceof ParameterizedType)) {
        throw new IllegalStateException(
            "Response must be parameterized" + " as Response<Foo> or Response<? extends Foo>");
      }
      responseType = getParameterUpperBound(0, (ParameterizedType) observableType);
    } else if (rawObservableType == Result.class) {
      if (!(observableType instanceof ParameterizedType)) {
        throw new IllegalStateException(
            "Result must be parameterized" + " as Result<Foo> or Result<? extends Foo>");
      }
      responseType = getParameterUpperBound(0, (ParameterizedType) observableType);
      isResult = true;
    } else {
      responseType = observableType;
      isBody = true;
    }

    return new RxJavaCallAdapter(
        responseType, scheduler, isAsync, isResult, isBody, isSingle, false);
  }
}
