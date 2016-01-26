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

import java.lang.reflect.Type;
import retrofit2.Call;
import retrofit2.CallAdapter;
import rx.Observable;
import rx.Single;

final class SingleHelper {
  static CallAdapter<Single<?>> makeSingle(final CallAdapter<Observable<?>> callAdapter) {
    return new CallAdapter<Single<?>>() {
      @Override public Type responseType() {
        return callAdapter.responseType();
      }

      @Override public <R> Single<?> adapt(Call<R> call) {
        Observable<?> observable = callAdapter.adapt(call);
        return observable.toSingle();
      }
    };
  }
}
