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
import java.io.InterruptedIOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

final class BehaviorCall<T> implements Call<T> {
  private final NetworkBehavior behavior;
  private final ExecutorService backgroundExecutor;
  private final Executor callbackExecutor;
  private final Call<T> delegate;

  private volatile Future<?> task;
  private volatile boolean canceled;
  private volatile boolean executed;

  BehaviorCall(NetworkBehavior behavior, ExecutorService backgroundExecutor,
      Executor callbackExecutor, Call<T> delegate) {
    if (callbackExecutor == null) {
      callbackExecutor = new Executor() {
        @Override public void execute(Runnable command) {
          command.run();
        }
      };
    }
    this.behavior = behavior;
    this.backgroundExecutor = backgroundExecutor;
    this.callbackExecutor = callbackExecutor;
    this.delegate = delegate;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone") // We are a final type & this saves clearing state.
  @Override public Call<T> clone() {
    return new BehaviorCall<>(behavior, backgroundExecutor, callbackExecutor, delegate.clone());
  }

  @Override public void enqueue(final Callback<T> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already executed");
      executed = true;
    }
    task = backgroundExecutor.submit(new Runnable() {
      private boolean delaySleep() {
        long sleepMs = behavior.calculateDelay(MILLISECONDS);
        if (sleepMs > 0) {
          try {
            Thread.sleep(sleepMs);
          } catch (InterruptedException e) {
            callFailure(new InterruptedIOException("canceled"));
            return false;
          }
        }
        return true;
      }

      private void callResponse(final Response<T> response) {
        callbackExecutor.execute(new Runnable() {
          @Override public void run() {
            callback.onResponse(response);
          }
        });
      }

      private void callFailure(final Throwable throwable) {
        callbackExecutor.execute(new Runnable() {
          @Override public void run() {
            callback.onFailure(throwable);
          }
        });
      }

      @Override public void run() {
        if (canceled) {
          callFailure(new InterruptedIOException("canceled"));
        } else if (behavior.calculateIsFailure()) {
          if (delaySleep()) {
            callFailure(behavior.failureException());
          }
        } else {
          delegate.enqueue(new Callback<T>() {
            @Override public void onResponse(final Response<T> response) {
              if (delaySleep()) {
                callResponse(response);
              }
            }

            @Override public void onFailure(final Throwable t) {
              if (delaySleep()) {
                callFailure(t);
              }
            }
          });
        }
      }
    });
  }

  @Override public Response<T> execute() throws IOException {
    final AtomicReference<Response<T>> responseRef = new AtomicReference<>();
    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    enqueue(new Callback<T>() {
      @Override public void onResponse(Response<T> response) {
        responseRef.set(response);
        latch.countDown();
      }

      @Override public void onFailure(Throwable t) {
        failureRef.set(t);
        latch.countDown();
      }
    });
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new InterruptedIOException("canceled");
    }
    Response<T> response = responseRef.get();
    if (response != null) return response;
    Throwable failure = failureRef.get();
    if (failure instanceof RuntimeException) throw (RuntimeException) failure;
    if (failure instanceof IOException) throw (IOException) failure;
    throw new RuntimeException(failure);
  }

  @Override public void cancel() {
    canceled = true;
    Future<?> task = this.task;
    if (task != null) {
      task.cancel(true);
    }
  }
}
