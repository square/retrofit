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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

abstract class Platform {
  private static final Platform PLATFORM = createPlatform();

  static Platform get() {
    return PLATFORM;
  }

  private static Platform createPlatform() {
    switch (System.getProperty("java.vm.name")) {
      case "Dalvik":
        if (Android24.isSupported()) {
          return new Android24();
        }
        return new Android21();

      case "RoboVM":
        return new RoboVm();

      default:
        if (Java16.isSupported()) {
          return new Java16();
        }
        if (Java14.isSupported()) {
          return new Java14();
        }
        return new Java8();
    }
  }

  abstract @Nullable Executor defaultCallbackExecutor();

  abstract List<? extends CallAdapter.Factory> createDefaultCallAdapterFactories(
      @Nullable Executor callbackExecutor);

  abstract List<? extends Converter.Factory> createDefaultConverterFactories();

  abstract boolean isDefaultMethod(Method method);

  abstract @Nullable Object invokeDefaultMethod(
      Method method, Class<?> declaringClass, Object proxy, Object... args) throws Throwable;

  private static final class Android21 extends Platform {
    @Override
    boolean isDefaultMethod(Method method) {
      return false;
    }

    @Nullable
    @Override
    Object invokeDefaultMethod(
        Method method, Class<?> declaringClass, Object proxy, Object... args) {
      throw new AssertionError();
    }

    @Override
    Executor defaultCallbackExecutor() {
      return MainThreadExecutor.INSTANCE;
    }

    @Override
    List<? extends CallAdapter.Factory> createDefaultCallAdapterFactories(
        @Nullable Executor callbackExecutor) {
      return singletonList(new DefaultCallAdapterFactory(callbackExecutor));
    }

    @Override
    List<? extends Converter.Factory> createDefaultConverterFactories() {
      return emptyList();
    }
  }

  @IgnoreJRERequirement // Only used on Android API 24+
  @TargetApi(24)
  private static final class Android24 extends Platform {
    static boolean isSupported() {
      return Build.VERSION.SDK_INT >= 24;
    }

    private @Nullable Constructor<Lookup> lookupConstructor;

    @Override
    Executor defaultCallbackExecutor() {
      return MainThreadExecutor.INSTANCE;
    }

    @Override
    List<? extends CallAdapter.Factory> createDefaultCallAdapterFactories(
        @Nullable Executor callbackExecutor) {
      return asList(
          new CompletableFutureCallAdapterFactory(),
          new DefaultCallAdapterFactory(callbackExecutor));
    }

    @Override
    List<? extends Converter.Factory> createDefaultConverterFactories() {
      return singletonList(new OptionalConverterFactory());
    }

    @Override
    public boolean isDefaultMethod(Method method) {
      return method.isDefault();
    }

    @Nullable
    @Override
    public Object invokeDefaultMethod(
        Method method, Class<?> declaringClass, Object proxy, Object... args) throws Throwable {
      if (Build.VERSION.SDK_INT < 26) {
        throw new UnsupportedOperationException(
            "Calling default methods on API 24 and 25 is not supported");
      }
      Constructor<Lookup> lookupConstructor = this.lookupConstructor;
      if (lookupConstructor == null) {
        lookupConstructor = Lookup.class.getDeclaredConstructor(Class.class, int.class);
        lookupConstructor.setAccessible(true);
        this.lookupConstructor = lookupConstructor;
      }
      return lookupConstructor
          .newInstance(declaringClass, -1 /* trusted */)
          .unreflectSpecial(method, declaringClass)
          .bindTo(proxy)
          .invokeWithArguments(args);
    }
  }

  private static final class RoboVm extends Platform {
    @Nullable
    @Override
    Executor defaultCallbackExecutor() {
      return null;
    }

    @Override
    List<? extends CallAdapter.Factory> createDefaultCallAdapterFactories(
        @Nullable Executor callbackExecutor) {
      return singletonList(new DefaultCallAdapterFactory(callbackExecutor));
    }

    @Override
    List<? extends Converter.Factory> createDefaultConverterFactories() {
      return emptyList();
    }

    @Override
    boolean isDefaultMethod(Method method) {
      return false;
    }

    @Nullable
    @Override
    Object invokeDefaultMethod(
        Method method, Class<?> declaringClass, Object proxy, Object... args) {
      throw new AssertionError();
    }
  }

  @IgnoreJRERequirement // Only used on JVM and Java 8 is the minimum-supported version.
  @SuppressWarnings("NewApi") // Not used for Android.
  private static final class Java8 extends Platform {
    private @Nullable Constructor<Lookup> lookupConstructor;

    @Nullable
    @Override
    Executor defaultCallbackExecutor() {
      return null;
    }

    @Override
    List<? extends CallAdapter.Factory> createDefaultCallAdapterFactories(
        @Nullable Executor callbackExecutor) {
      return asList(
          new CompletableFutureCallAdapterFactory(),
          new DefaultCallAdapterFactory(callbackExecutor));
    }

    @Override
    List<? extends Converter.Factory> createDefaultConverterFactories() {
      return singletonList(new OptionalConverterFactory());
    }

    @Override
    public boolean isDefaultMethod(Method method) {
      return method.isDefault();
    }

    @Override
    public @Nullable Object invokeDefaultMethod(
        Method method, Class<?> declaringClass, Object proxy, Object... args) throws Throwable {
      Constructor<Lookup> lookupConstructor = this.lookupConstructor;
      if (lookupConstructor == null) {
        lookupConstructor = Lookup.class.getDeclaredConstructor(Class.class, int.class);
        lookupConstructor.setAccessible(true);
        this.lookupConstructor = lookupConstructor;
      }
      return lookupConstructor
          .newInstance(declaringClass, -1 /* trusted */)
          .unreflectSpecial(method, declaringClass)
          .bindTo(proxy)
          .invokeWithArguments(args);
    }
  }

  /**
   * Java 14 allows a regular lookup to succeed for invoking default methods.
   *
   * <p>https://bugs.openjdk.java.net/browse/JDK-8209005
   */
  @IgnoreJRERequirement // Only used on JVM and Java 14.
  @SuppressWarnings("NewApi") // Not used for Android.
  private static final class Java14 extends Platform {
    static boolean isSupported() {
      try {
        Object version = Runtime.class.getMethod("version").invoke(null);
        Integer feature = (Integer) version.getClass().getMethod("feature").invoke(version);
        return feature >= 14;
      } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException ignored) {
        return false;
      }
    }

    @Nullable
    @Override
    Executor defaultCallbackExecutor() {
      return null;
    }

    @Override
    List<? extends CallAdapter.Factory> createDefaultCallAdapterFactories(
        @Nullable Executor callbackExecutor) {
      return asList(
          new CompletableFutureCallAdapterFactory(),
          new DefaultCallAdapterFactory(callbackExecutor));
    }

    @Override
    List<? extends Converter.Factory> createDefaultConverterFactories() {
      return singletonList(new OptionalConverterFactory());
    }

    @Override
    public boolean isDefaultMethod(Method method) {
      return method.isDefault();
    }

    @Nullable
    @Override
    public Object invokeDefaultMethod(
        Method method, Class<?> declaringClass, Object proxy, Object... args) throws Throwable {
      return MethodHandles.lookup()
          .unreflectSpecial(method, declaringClass)
          .bindTo(proxy)
          .invokeWithArguments(args);
    }
  }

  /**
   * Java 16 has a supported public API for invoking default methods on a proxy. We invoke it
   * reflectively because we cannot compile against the API directly.
   */
  @IgnoreJRERequirement // Only used on JVM and Java 16.
  @SuppressWarnings("NewApi") // Not used for Android.
  private static final class Java16 extends Platform {
    static boolean isSupported() {
      try {
        Object version = Runtime.class.getMethod("version").invoke(null);
        Integer feature = (Integer) version.getClass().getMethod("feature").invoke(version);
        return feature >= 16;
      } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException ignored) {
        return false;
      }
    }

    private @Nullable Method invokeDefaultMethod;

    @Nullable
    @Override
    Executor defaultCallbackExecutor() {
      return null;
    }

    @Override
    List<? extends CallAdapter.Factory> createDefaultCallAdapterFactories(
        @Nullable Executor callbackExecutor) {
      return asList(
          new CompletableFutureCallAdapterFactory(),
          new DefaultCallAdapterFactory(callbackExecutor));
    }

    @Override
    List<? extends Converter.Factory> createDefaultConverterFactories() {
      return singletonList(new OptionalConverterFactory());
    }

    @Override
    public boolean isDefaultMethod(Method method) {
      return method.isDefault();
    }

    @SuppressWarnings("JavaReflectionMemberAccess") // Only available on Java 16, as we expect.
    @Nullable
    @Override
    public Object invokeDefaultMethod(
        Method method, Class<?> declaringClass, Object proxy, Object... args) throws Throwable {
      Method invokeDefaultMethod = this.invokeDefaultMethod;
      if (invokeDefaultMethod == null) {
        invokeDefaultMethod =
            InvocationHandler.class.getMethod(
                "invokeDefault", Object.class, Method.class, Object[].class);
        this.invokeDefaultMethod = invokeDefaultMethod;
      }
      return invokeDefaultMethod.invoke(null, proxy, method, args);
    }
  }

  private static final class MainThreadExecutor implements Executor {
    static final Executor INSTANCE = new MainThreadExecutor();

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(Runnable r) {
      handler.post(r);
    }
  }
}
