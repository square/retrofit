/*
 * Copyright (C) 2015 Square, Inc.
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
package retrofit.mock;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class MockRetrofit {
  private final NetworkBehavior behavior;
  private final NetworkBehavior.Adapter<Object> adapter;

  @SuppressWarnings("unchecked") //
  public MockRetrofit(NetworkBehavior behavior, NetworkBehavior.Adapter<?> adapter) {
    this.adapter = (NetworkBehavior.Adapter<Object>) adapter;
    this.behavior = behavior;
  }

  @SuppressWarnings("unchecked") // Single-interface proxy creation guarded by parameter safety.
  public <T> T create(Class<T> service, final T instance) {
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class[] { service },
        new InvocationHandler() {
          @Override public Object invoke(Object proxy, Method method, Object[] args)
              throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
              return method.invoke(this, args);
            }
            method.setAccessible(true); // Just In Case™

            Object value = method.invoke(instance, args);
            return adapter.applyBehavior(behavior, value);
          }
        });
  }
}
