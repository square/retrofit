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

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import javax.annotation.Nullable;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

/**
 * From Java 8 to Java 13, the only way to invoke a default method on a proxied interface is by
 * reflectively creating a trusted {@link Lookup} to invoke a method handle.
 * <p>
 * Note: This class has multi-release jar variants for newer versions of Java.
 */
final class DefaultMethodSupport {
  private static @Nullable Constructor<Lookup> lookupConstructor;

  @IgnoreJRERequirement // Only used on JVM or Android API 24+.
  @Nullable
  static Object invoke(
      Method method, Class<?> declaringClass, Object proxy, @Nullable Object[] args)
      throws Throwable {
    Constructor<Lookup> constructor = lookupConstructor;
    if (constructor == null) {
      constructor = Lookup.class.getDeclaredConstructor(Class.class, int.class);
      constructor.setAccessible(true);
      lookupConstructor = constructor;
    }
    return constructor
        .newInstance(declaringClass, -1 /* trusted */)
        .unreflectSpecial(method, declaringClass)
        .bindTo(proxy)
        .invokeWithArguments(args);
  }

  private DefaultMethodSupport() {}
}
