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
package retrofit2.mock;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutorService;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * Applies {@linkplain NetworkBehavior behavior} to responses and adapts them into the appropriate
 * return type using the {@linkplain Retrofit#callAdapterFactories() call adapters} of
 * {@link Retrofit}.
 *
 * @see MockRetrofit#create(Class)
 */
public final class BehaviorDelegate<T> {
  final Retrofit retrofit;
  private final NetworkBehavior behavior;
  private final ExecutorService executor;
  private final Class<T> service;

  BehaviorDelegate(Retrofit retrofit, NetworkBehavior behavior, ExecutorService executor,
      Class<T> service) {
    this.retrofit = retrofit;
    this.behavior = behavior;
    this.executor = executor;
    this.service = service;
  }

  public T returningResponse(Object response) {
    return returning(Calls.response(response));
  }

  @SuppressWarnings("unchecked") // Single-interface proxy creation guarded by parameter safety.
  public T returning(Call<?> call) {
    final Call<?> behaviorCall = new BehaviorCall<>(behavior, executor, call);
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class[] { service },
        new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Type returnType = method.getGenericReturnType();
            Annotation[] methodAnnotations = method.getAnnotations();
            CallAdapter<?> callAdapter = retrofit.callAdapter(returnType, methodAnnotations);
            return callAdapter.adapt(behaviorCall);
          }
        });
  }
}
