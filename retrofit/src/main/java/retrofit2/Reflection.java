/*
 * Copyright (C) 2024 Square, Inc.
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

import android.annotation.TargetApi;
import android.os.Build;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import javax.annotation.Nullable;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

class Reflection {
  boolean isDefaultMethod(Method method) {
    return false;
  }

  @Nullable
  Object invokeDefaultMethod(
      Method method, Class<?> declaringClass, Object proxy, @Nullable Object[] args)
      throws Throwable {
    throw new AssertionError();
  }

  String describeMethodParameter(Method method, int index) {
    return "parameter #" + (index + 1);
  }

  @IgnoreJRERequirement // Only used on JVM.
  static class Java8 extends Reflection {
    @Override
    boolean isDefaultMethod(Method method) {
      return method.isDefault();
    }

    @Override
    Object invokeDefaultMethod(
        Method method, Class<?> declaringClass, Object proxy, @Nullable Object[] args)
        throws Throwable {
      return DefaultMethodSupport.invoke(method, declaringClass, proxy, args);
    }

    @Override
    String describeMethodParameter(Method method, int index) {
      Parameter parameter = method.getParameters()[index];
      if (parameter.isNamePresent()) {
        return "parameter '" + parameter.getName() + '\'';
      }
      return super.describeMethodParameter(method, index);
    }
  }

  /**
   * Android does not support MR jars, so this uses the Java 8 support class.
   * Default methods and the reflection API to detect them were added to API 24
   * as part of the initial Java 8 set. MethodHandle, our means of invoking the default method
   * through the proxy, was not added until API 26.
   */
  @TargetApi(24)
  @IgnoreJRERequirement // Only used on Android API 24+.
  static final class Android24 extends Reflection {
    @Override
    boolean isDefaultMethod(Method method) {
      return method.isDefault();
    }

    @Override
    Object invokeDefaultMethod(
        Method method, Class<?> declaringClass, Object proxy, @Nullable Object[] args)
        throws Throwable {
      if (Build.VERSION.SDK_INT < 26) {
        throw new UnsupportedOperationException(
            "Calling default methods on API 24 and 25 is not supported");
      }
      return DefaultMethodSupport.invoke(method, declaringClass, proxy, args);
    }
  }
}
