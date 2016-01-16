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
package retrofit2.mock;

import java.io.IOException;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        callback.onResponse(this, response);
      }

      @Override public boolean isExecuted() {
        return false;
      }

      @Override public void cancel() {
      }

      @Override public boolean isCanceled() {
        return false;
      }

      @SuppressWarnings("CloneDoesntCallSuperClone") // Immutable object.
      @Override public Call<T> clone() {
        return this;
      }

      @Override public Request request() {
        return response.raw().request();
      }
    };
  }

  public static <T> Call<T> failure(final IOException failure) {
    return new Call<T>() {
      @Override public Response<T> execute() throws IOException {
        throw failure;
      }

      @Override public void enqueue(Callback<T> callback) {
        callback.onFailure(this, failure);
      }

      @Override public boolean isExecuted() {
        return false;
      }

      @Override public void cancel() {
      }

      @Override public boolean isCanceled() {
        return false;
      }

      @SuppressWarnings("CloneDoesntCallSuperClone") // Immutable object.
      @Override public Call<T> clone() {
        return this;
      }

      @Override public Request request() {
        return new Request.Builder().url("http://localhost").build();
      }
    };
  }

  private Calls() {
    throw new AssertionError("No instances.");
  }
}
