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
import android.util.Log;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

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
    try {
      Class.forName("java.util.Optional");
      return new Java8();
    } catch (ClassNotFoundException ignored) {
    }
    return new Platform();
  }

  void log(String message) {
    System.out.println(message);
  }

  CallAdapter.Factory defaultCallAdapterFactory(Executor callbackExecutor) {
    if (callbackExecutor != null) {
      return new ExecutorCallAdapterFactory(callbackExecutor);
    }
    return DefaultCallAdapter.FACTORY;
  }

  boolean isDefaultMethod(Method method) {
    return false;
  }

  Object invokeDefaultMethod(Method method, Class<?> declaringClass, Object object, Object... args)
      throws Throwable {
    throw new UnsupportedOperationException();
  }

  @IgnoreJRERequirement // Only classloaded and used on Java 8.
  static class Java8 extends Platform {
    @Override boolean isDefaultMethod(Method method) {
      return method.isDefault();
    }

    @Override Object invokeDefaultMethod(Method method, Class<?> declaringClass, Object object,
        Object... args) throws Throwable {
      return MethodHandles.lookup()
          .in(declaringClass)
          .unreflectSpecial(method, declaringClass)
          .bindTo(object)
          .invokeWithArguments(args);
    }
  }

  static class Android extends Platform {
    private static final int MAX_LOG_LENGTH = 4000;

    @Override void log(String message) {
      // Split by line, then ensure each line can fit into Log's maximum length.
      for (int i = 0, length = message.length(); i < length; i++) {
        int newline = message.indexOf('\n', i);
        newline = newline != -1 ? newline : length;
        do {
          int end = Math.min(newline, i + MAX_LOG_LENGTH);
          Log.d("Retrofit", message.substring(i, end));
          i = end;
        } while (i < newline);
      }
    }

    @Override CallAdapter.Factory defaultCallAdapterFactory(Executor callbackExecutor) {
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
    }
  }
}
