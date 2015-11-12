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
package retrofit.mock;

import java.io.IOException;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;

/** Factory methods for creating {@link Call} instances which immediately respond or fail. */
public final class Calls {
  public static <T> Call<T> response(T successValue) {
    return response(Response.success(successValue));
  }

  public static <T> Call<T> response(final Response<T> response) {
    return new Call<T>() {
      @Override public Response<T> execute() throws IOException {
        return response;
      }

      @Override public void enqueue(Callback<T> callback) {
        callback.onResponse(response);
      }

      @Override public void cancel() {
      }

      @SuppressWarnings("CloneDoesntCallSuperClone") // Immutable object.
      @Override public Call<T> clone() {
        return this;
      }
    };
  }

  public static <T> Call<T> failure(final IOException failure) {
    return new Call<T>() {
      @Override public Response<T> execute() throws IOException {
        throw failure;
      }

      @Override public void enqueue(Callback<T> callback) {
        callback.onFailure(failure);
      }

      @Override public void cancel() {
      }

      @SuppressWarnings("CloneDoesntCallSuperClone") // Immutable object.
      @Override public Call<T> clone() {
        return this;
      }
    };
  }

  private Calls() {
    throw new AssertionError("No instances.");
  }
}
