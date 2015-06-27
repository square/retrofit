/*
 * Copyright (C) 2013 Square, Inc.
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

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.squareup.okhttp.OkHttpClient;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

class Platform {
  private static final Platform PLATFORM = findPlatform();

  static Platform get() {
    return PLATFORM;
  }

  private static Platform findPlatform() {
    try {
      Class.forName("android.os.Build");
      if (Build.VERSION.SDK_INT != 0) {
        return new Android();
      }
    } catch (ClassNotFoundException ignored) {
    }

    return new Platform();
  }

  CallAdapter.Factory defaultCallAdapterFactory(Executor callbackExecutor) {
    if (callbackExecutor != null) {
      return new ExecutorCallAdapterFactory(callbackExecutor);
    }
    return new NothingCallAdapterFactory();
  }

  OkHttpClient defaultClient() {
    OkHttpClient client = new OkHttpClient();
    client.setConnectTimeout(15, TimeUnit.SECONDS);
    client.setReadTimeout(15, TimeUnit.SECONDS);
    client.setWriteTimeout(15, TimeUnit.SECONDS);
    return client;
  }

  /** Provides sane defaults for operation on Android. */
  static class Android extends Platform {
    CallAdapter.Factory defaultCallAdapterFactory(Executor callbackExecutor) {
      if (callbackExecutor == null) {
        callbackExecutor = new MainThreadExecutor();
      }
      return new ExecutorCallAdapterFactory(callbackExecutor);
    }

    static class MainThreadExecutor implements Executor {
      private final Handler handler = new Handler(Looper.getMainLooper());

      @Override public void execute(Runnable r) {
        handler.post(r);
      }

      @Override public String toString() {
        return "MainThreadExecutor";
      }
    }
  }

  static final class NothingCallAdapterFactory implements CallAdapter.Factory {
    @Override public CallAdapter<?> get(Type returnType) {
      if (Utils.getRawType(returnType) != Call.class) {
        return null;
      }
      Type responseType = Utils.getCallResponseType(returnType);
      return new NothingCallAdapter<>(responseType);
    }

    @Override public String toString() {
      return "Default";
    }

    static final class NothingCallAdapter<T> implements CallAdapter<T> {
      private final Type responseType;

      NothingCallAdapter(Type responseType) {
        this.responseType = responseType;
      }

      @Override public Type responseType() {
        return responseType;
      }

      @Override public Call<T> adapt(Call<T> call) {
        return call;
      }
    }
  }
}
