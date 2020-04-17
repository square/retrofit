/*
 * Copyright (C) 2017 Square, Inc.
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
package retrofit2.adapter.scala;

import java.lang.reflect.Type;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import scala.concurrent.Future;
import scala.concurrent.Promise;

final class ResponseCallAdapter<T> implements CallAdapter<T, Future<Response<T>>> {
  private final Type responseType;

  ResponseCallAdapter(Type responseType) {
    this.responseType = responseType;
  }

  @Override
  public Type responseType() {
    return responseType;
  }

  @Override
  public Future<Response<T>> adapt(Call<T> call) {
    Promise<Response<T>> promise = Promise.apply();

    call.enqueue(
        new Callback<T>() {
          @Override
          public void onResponse(Call<T> call, Response<T> response) {
            promise.success(response);
          }

          @Override
          public void onFailure(Call<T> call, Throwable t) {
            promise.failure(t);
          }
        });

    return promise.future();
  }
}
