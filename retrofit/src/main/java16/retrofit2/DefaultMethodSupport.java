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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import javax.annotation.Nullable;

/** Java 16 finally has a public API for invoking default methods on a proxy. */
final class DefaultMethodSupport {
  @Nullable
  static Object invoke(
      Method method, Class<?> declaringClass, Object proxy, @Nullable Object[] args)
      throws Throwable {
    return InvocationHandler.invokeDefault(proxy, method, args);
  }

  private DefaultMethodSupport() {}
}
