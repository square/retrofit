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
package com.example.retrofit;

import static rx.schedulers.Schedulers.io;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public final class RxJavaObserveOnMainThread {
  @SuppressWarnings("UnusedVariable")
  public static void main(String... args) {
    Scheduler observeOn = Schedulers.computation(); // Or use mainThread() for Android.

    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl("http://example.com")
            .addCallAdapterFactory(new ObserveOnMainCallAdapterFactory(observeOn))
            .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(io()))
            .build();

    // Services created with this instance that use Observable will execute on the 'io' scheduler
    // and notify their observer on the 'computation' scheduler.
  }

  static final class ObserveOnMainCallAdapterFactory extends CallAdapter.Factory {
    final Scheduler scheduler;

    ObserveOnMainCallAdapterFactory(Scheduler scheduler) {
      this.scheduler = scheduler;
    }

    @Override
    public @Nullable CallAdapter<?, ?> get(
        Type returnType, Annotation[] annotations, Retrofit retrofit) {
      if (getRawType(returnType) != Observable.class) {
        return null; // Ignore non-Observable types.
      }

      // Look up the next call adapter which would otherwise be used if this one was not present.
      //noinspection unchecked returnType checked above to be Observable.
      final CallAdapter<Object, Observable<?>> delegate =
          (CallAdapter<Object, Observable<?>>)
              retrofit.nextCallAdapter(this, returnType, annotations);

      return new CallAdapter<Object, Object>() {
        @Override
        public Object adapt(Call<Object> call) {
          // Delegate to get the normal Observable...
          Observable<?> o = delegate.adapt(call);
          // ...and change it to send notifications to the observer on the specified scheduler.
          return o.observeOn(scheduler);
        }

        @Override
        public Type responseType() {
          return delegate.responseType();
        }
      };
    }
  }
}
