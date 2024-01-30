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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import javax.annotation.Nullable;

/**
 * Java 14 allows a regular (i.e., non-trusted) lookup to succeed for invoking default methods.
 * <p>
 * https://bugs.openjdk.java.net/browse/JDK-8209005
 */
final class DefaultMethodSupport {
  @Nullable
  static Object invoke(
      Method method, Class<?> declaringClass, Object proxy, @Nullable Object[] args)
      throws Throwable {
    return MethodHandles.lookup()
        .unreflectSpecial(method, declaringClass)
        .bindTo(proxy)
        .invokeWithArguments(args);
  }

  private DefaultMethodSupport() {}
}
