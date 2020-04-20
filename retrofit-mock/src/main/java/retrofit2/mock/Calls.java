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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Factory methods for creating {@link Call} instances which immediately respond or fail. */
public final class Calls {
  /**
   * Invokes {@code callable} once for the returned {@link Call} and once for each instance that is
   * obtained from {@linkplain Call#clone() cloning} the returned {@link Call}.
   */
  public static <T> Call<T> defer(Callable<Call<T>> callable) {
    return new DeferredCall<>(callable);
  }

  public static <T> Call<T> response(@Nullable T successValue) {
    return new FakeCall<>(Response.success(successValue), null);
  }

  public static <T> Call<T> response(Response<T> response) {
    return new FakeCall<>(response, null);
  }

  /** Creates a failed {@link Call} from {@code failure}. */
  public static <T> Call<T> failure(IOException failure) {
    // TODO delete this overload in Retrofit 3.0.
    return new FakeCall<>(null, failure);
  }

  /**
   * Creates a failed {@link Call} from {@code failure}.
   *
   * <p>Note: When invoking {@link Call#execute() execute()} on the returned {@link Call}, if {@code
   * failure} is a {@link RuntimeException}, {@link Error}, or {@link IOException} subtype it is
   * thrown directly. Otherwise it is "sneaky thrown" despite not being declared.
   */
  public static <T> Call<T> failure(Throwable failure) {
    return new FakeCall<>(null, failure);
  }

  private Calls() {
    throw new AssertionError("No instances.");
  }

  static final class FakeCall<T> implements Call<T> {
    private final Response<T> response;
    private final Throwable error;
    private final AtomicBoolean canceled = new AtomicBoolean();
    private final AtomicBoolean executed = new AtomicBoolean();

    FakeCall(@Nullable Response<T> response, @Nullable Throwable error) {
      if ((response == null) == (error == null)) {
        throw new AssertionError("Only one of response or error can be set.");
      }
      this.response = response;
      this.error = error;
    }

    @Override
    public Response<T> execute() throws IOException {
      if (!executed.compareAndSet(false, true)) {
        throw new IllegalStateException("Already executed");
      }
      if (canceled.get()) {
        throw new IOException("canceled");
      }
      if (response != null) {
        return response;
      }
      throw FakeCall.<Error>sneakyThrow(error);
    }

    // Intentionally abusing this feature.
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    private static <T extends Throwable> T sneakyThrow(Throwable t) throws T {
      throw (T) t;
    }

    @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
    @Override
    public void enqueue(Callback<T> callback) {
      if (callback == null) {
        throw new NullPointerException("callback == null");
      }
      if (!executed.compareAndSet(false, true)) {
        throw new IllegalStateException("Already executed");
      }
      if (canceled.get()) {
        callback.onFailure(this, new IOException("canceled"));
      } else if (response != null) {
        callback.onResponse(this, response);
      } else {
        callback.onFailure(this, error);
      }
    }

    @Override
    public boolean isExecuted() {
      return executed.get();
    }

    @Override
    public void cancel() {
      canceled.set(true);
    }

    @Override
    public boolean isCanceled() {
      return canceled.get();
    }

    @Override
    public Call<T> clone() {
      return new FakeCall<>(response, error);
    }

    @Override
    public Request request() {
      if (response != null) {
        return response.raw().request();
      }
      return new Request.Builder().url("http://localhost").build();
    }

    @Override
    public Timeout timeout() {
      return Timeout.NONE;
    }
  }

  static final class DeferredCall<T> implements Call<T> {
    private final Callable<Call<T>> callable;
    private @Nullable Call<T> delegate;

    DeferredCall(Callable<Call<T>> callable) {
      this.callable = callable;
    }

    private synchronized Call<T> getDelegate() {
      Call<T> delegate = this.delegate;
      if (delegate == null) {
        try {
          delegate = callable.call();
        } catch (Exception e) {
          delegate = failure(e);
        }
        this.delegate = delegate;
      }
      return delegate;
    }

    @Override
    public Response<T> execute() throws IOException {
      return getDelegate().execute();
    }

    @Override
    public void enqueue(Callback<T> callback) {
      getDelegate().enqueue(callback);
    }

    @Override
    public boolean isExecuted() {
      return getDelegate().isExecuted();
    }

    @Override
    public void cancel() {
      getDelegate().cancel();
    }

    @Override
    public boolean isCanceled() {
      return getDelegate().isCanceled();
    }

    @Override
    public Call<T> clone() {
      return new DeferredCall<>(callable);
    }

    @Override
    public Request request() {
      return getDelegate().request();
    }

    @Override
    public Timeout timeout() {
      return getDelegate().timeout();
    }
  }
}
