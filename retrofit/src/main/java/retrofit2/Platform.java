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
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

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
    return new Platform(true);
  }

  private final boolean hasJava8Types;

  Platform(boolean hasJava8Types) {
    this.hasJava8Types = hasJava8Types;
  }

  @Nullable Executor defaultCallbackExecutor() {
    return null;
  }

  List<? extends CallAdapter.Factory> defaultCallAdapterFactories(
      @Nullable Executor callbackExecutor) {
    DefaultCallAdapterFactory executorFactory = new DefaultCallAdapterFactory(callbackExecutor);
    return hasJava8Types
        ? asList(CompletableFutureCallAdapterFactory.INSTANCE, executorFactory)
        : singletonList(executorFactory);
  }

  int defaultCallAdapterFactoriesSize() {
    return hasJava8Types ? 2 : 1;
  }

  List<? extends Converter.Factory> defaultConverterFactories() {
    return hasJava8Types
        ? singletonList(OptionalConverterFactory.INSTANCE)
        : emptyList();
  }

  int defaultConverterFactoriesSize() {
    return hasJava8Types ? 1 : 0;
  }

  boolean isDefaultMethod(Method method) {
    return hasJava8Types && method.isDefault();
  }

  @Nullable Object invokeDefaultMethod(Method method, Class<?> declaringClass, Object object,
      @Nullable Object... args) throws Throwable {
    // Because the service interface might not be public, we need to use a MethodHandle lookup
    // that ignores the visibility of the declaringClass.
    Constructor<Lookup> constructor = Lookup.class.getDeclaredConstructor(Class.class, int.class);
    constructor.setAccessible(true);
    return constructor.newInstance(declaringClass, -1 /* trusted */)
        .unreflectSpecial(method, declaringClass)
        .bindTo(object)
        .invokeWithArguments(args);
  }

  static final class Android extends Platform {
    Android() {
      super(Build.VERSION.SDK_INT >= 24);
    }

    @Override public Executor defaultCallbackExecutor() {
      return new MainThreadExecutor();
    }

    static class MainThreadExecutor implements Executor {
      private final Handler handler = new Handler(Looper.getMainLooper());

      @Override public void execute(Runnable r) {
        handler.post(r);
      }
    }
  }
}
