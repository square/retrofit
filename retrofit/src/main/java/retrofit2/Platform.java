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
package retrofit2;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
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

  @Nullable Executor defaultCallbackExecutor() {
    return null;
  }

  CallAdapter.Factory defaultCallAdapterFactory(@Nullable Executor callbackExecutor) {
    if (callbackExecutor != null) {
      return new ExecutorCallAdapterFactory(callbackExecutor);
    }
    return DefaultCallAdapterFactory.INSTANCE;
  }

  boolean isDefaultMethod(Method method) {
    return false;
  }

  @Nullable Object invokeDefaultMethod(Method method, Class<?> declaringClass, Object object,
      @Nullable Object... args) throws Throwable {
    throw new UnsupportedOperationException();
  }

  @IgnoreJRERequirement // Only classloaded and used on Java 8.
  static class Java8 extends Platform {

    private final Map<CacheKey, MethodHandle> methodHandleCache = new ConcurrentHashMap<>();

    private static class CacheKey {
      private final Method method;
      private final Class<?> declaringClass;
      private final Object object;

      CacheKey(Method method, Class<?> declaringClass, Object object) {
        this.method = method;
        this.declaringClass = declaringClass;
        this.object = object;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) {
          return true;
        }
        if (o == null || getClass() != o.getClass()) {
          return false;
        }

        CacheKey cacheKey = (CacheKey) o;

        if (!method.equals(cacheKey.method)) {
          return false;
        }
        if (!declaringClass.equals(cacheKey.declaringClass)) {
          return false;
        }
        return object == cacheKey.object;
      }

      @Override
      public int hashCode() {
        int result = method.hashCode();
        result = 31 * result + declaringClass.hashCode();
        result = 31 * result + object.hashCode();
        return result;
      }
    }

    @Override boolean isDefaultMethod(Method method) {
      return method.isDefault();
    }

    @Override Object invokeDefaultMethod(Method method, Class<?> declaringClass, Object object,
        @Nullable Object... args) throws Throwable {
      CacheKey key = new CacheKey(method, declaringClass, object);
      MethodHandle result = methodHandleCache.get(key);
      if (result == null) {
        synchronized (methodHandleCache) {
          result = methodHandleCache.get(key);
          if (result == null) {
            // Because the service interface might not be public, we need to use a MethodHandle
            // lookup that ignores the visibility of the declaringClass.
            Constructor<Lookup> constructor =
                    Lookup.class.getDeclaredConstructor(Class.class, int.class);
            constructor.setAccessible(true);
            result = constructor.newInstance(declaringClass, -1 /* trusted */)
                    .unreflectSpecial(method, declaringClass)
                    .bindTo(object);
            methodHandleCache.put(key, result);
          }
        }
      }
      return result.invokeWithArguments(args);
    }
  }

  static class Android extends Platform {
    @Override public Executor defaultCallbackExecutor() {
      return new MainThreadExecutor();
    }

    @Override CallAdapter.Factory defaultCallAdapterFactory(@Nullable Executor callbackExecutor) {
      if (callbackExecutor == null) throw new AssertionError();
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
