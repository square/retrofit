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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;

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

  List<? extends CallAdapter.Factory> defaultCallAdapterFactories(
      @Nullable Executor callbackExecutor) {
    return singletonList(new DefaultCallAdapterFactory(callbackExecutor));
  }

  int defaultCallAdapterFactoriesSize() {
    return 1;
  }

  List<? extends Converter.Factory> defaultConverterFactories() {
    return emptyList();
  }

  int defaultConverterFactoriesSize() {
    return 0;
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
    @Override boolean isDefaultMethod(Method method) {
      return method.isDefault();
    }

    @Override Object invokeDefaultMethod(Method method, Class<?> declaringClass, Object object,
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

    @Override List<? extends CallAdapter.Factory> defaultCallAdapterFactories(
        @Nullable Executor callbackExecutor) {
      List<CallAdapter.Factory> factories = new ArrayList<>(2);
      factories.add(CompletableFutureCallAdapterFactory.INSTANCE);
      factories.add(new DefaultCallAdapterFactory(callbackExecutor));
      return unmodifiableList(factories);
    }

    @Override int defaultCallAdapterFactoriesSize() {
      return 2;
    }

    @Override List<? extends Converter.Factory> defaultConverterFactories() {
      return singletonList(OptionalConverterFactory.INSTANCE);
    }

    @Override int defaultConverterFactoriesSize() {
      return 1;
    }
  }

  static class Android extends Platform {
    @IgnoreJRERequirement // Guarded by API check.
    @Override boolean isDefaultMethod(Method method) {
      if (Build.VERSION.SDK_INT < 24) {
        return false;
      }
      return method.isDefault();
    }

    @Override public Executor defaultCallbackExecutor() {
      return new MainThreadExecutor();
    }

    @Override List<? extends CallAdapter.Factory> defaultCallAdapterFactories(
        @Nullable Executor callbackExecutor) {
      if (callbackExecutor == null) throw new AssertionError();
      DefaultCallAdapterFactory executorFactory = new DefaultCallAdapterFactory(callbackExecutor);
      return Build.VERSION.SDK_INT >= 24
        ? asList(CompletableFutureCallAdapterFactory.INSTANCE, executorFactory)
        : singletonList(executorFactory);
    }

    @Override int defaultCallAdapterFactoriesSize() {
      return Build.VERSION.SDK_INT >= 24 ? 2 : 1;
    }

    @Override List<? extends Converter.Factory> defaultConverterFactories() {
      return Build.VERSION.SDK_INT >= 24
          ? singletonList(OptionalConverterFactory.INSTANCE)
          : Collections.<Converter.Factory>emptyList();
    }

    @Override int defaultConverterFactoriesSize() {
      return Build.VERSION.SDK_INT >= 24 ? 1 : 0;
    }

    static class MainThreadExecutor implements Executor {
      private final Handler handler = new Handler(Looper.getMainLooper());

      @Override public void execute(Runnable r) {
        handler.post(r);
      }
    }
  }
}
