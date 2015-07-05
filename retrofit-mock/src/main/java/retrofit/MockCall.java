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
import java.io.InterruptedIOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

final class MockCall<T> implements Call<T> {
  private final MockRetrofit mockRetrofit;
  private final ExecutorService backgroundExecutor;
  private final Executor callbackExecutor;
  private final Response<T> response;
  private final IOException failure;
  private final boolean isFailure;

  private volatile Future<?> task;
  private volatile boolean canceled;
  private volatile boolean executed;

  MockCall(MockRetrofit mockRetrofit, ExecutorService backgroundExecutor, Executor callbackExecutor,
      Response<T> response, IOException failure) {
    if (callbackExecutor == null) {
      callbackExecutor = new Executor() {
        @Override public void execute(Runnable command) {
          command.run();
        }
      };
    }
    this.mockRetrofit = mockRetrofit;
    this.backgroundExecutor = backgroundExecutor;
    this.callbackExecutor = callbackExecutor;
    this.response = response;
    this.failure = failure;
    this.isFailure = failure != null;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone") // We are a final type & this saves clearing state.
  @Override public Call<T> clone() {
    return new MockCall<>(mockRetrofit, backgroundExecutor, callbackExecutor, response, failure);
  }

  private Response<T> getResponse() throws IOException, InterruptedException {
    if (mockRetrofit.calculateIsFailure() || isFailure) {
      Thread.sleep(mockRetrofit.calculateDelayForError());
      throw isFailure ? failure : new IOException("Mock exception");
    }
    Thread.sleep(mockRetrofit.calculateDelayForCall());
    return response;
  }

  @Override public void enqueue(final Callback<T> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already executed");
      executed = true;
    }
    task = backgroundExecutor.submit(new Runnable() {
      private void callFailure(final Throwable throwable) {
        callbackExecutor.execute(new Runnable() {
          @Override public void run() {
            try {
              callback.onFailure(throwable);
            } catch (Throwable t) {
              Thread thread = Thread.currentThread();
              thread.getUncaughtExceptionHandler().uncaughtException(thread, t);
            }
          }
        });
      }

      private void callSuccess(final Response<T> response) {
        callbackExecutor.execute(new Runnable() {
          @Override public void run() {
            try {
              callback.onResponse(response);
            } catch (Throwable t) {
              Thread thread = Thread.currentThread();
              thread.getUncaughtExceptionHandler().uncaughtException(thread, t);
            }
          }
        });
      }

      @Override public void run() {
        Response<T> response;
        try {
          response = getResponse();
        } catch (IOException e) {
          callFailure(e);
          return;
        } catch (InterruptedException e) {
          callFailure(new InterruptedIOException("canceled"));
          return;
        }
        callSuccess(response);
      }
    });
  }

  @Override public Response<T> execute() throws IOException {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already executed");
      executed = true;
    }
    if (canceled) {
      throw new InterruptedIOException("canceled");
    }

    Future<Response<T>> task = backgroundExecutor.submit(new Callable<Response<T>>() {
      @Override public Response<T> call() throws Exception {
        return getResponse();
      }
    });
    this.task = task;

    try {
      return task.get();
    } catch (CancellationException | InterruptedException e) {
      throw new InterruptedIOException("canceled");
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      if (cause instanceof IOException) {
        throw (IOException) cause;
      }
      throw new IllegalStateException(cause);
    }
  }

  @Override public void cancel() {
    canceled = true;
    Future<?> task = this.task;
    if (task != null) {
      task.cancel(true);
    }
  }
}
