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
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;
import kotlin.coroutines.Continuation;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.KotlinExtensions;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Applies {@linkplain NetworkBehavior behavior} to responses and adapts them into the appropriate
 * return type using the {@linkplain Retrofit#callAdapterFactories() call adapters} of {@link
 * Retrofit}.
 *
 * @see MockRetrofit#create(Class)
 */
public final class BehaviorDelegate<T> {
  final Retrofit retrofit;
  private final NetworkBehavior behavior;
  private final ExecutorService executor;
  private final Class<T> service;

  BehaviorDelegate(
      Retrofit retrofit, NetworkBehavior behavior, ExecutorService executor, Class<T> service) {
    this.retrofit = retrofit;
    this.behavior = behavior;
    this.executor = executor;
    this.service = service;
  }

  public T returningResponse(@Nullable Object response) {
    return returning(Calls.response(response));
  }

  @SuppressWarnings("unchecked") // Single-interface proxy creation guarded by parameter safety.
  public <R> T returning(Call<R> call) {
    final Call<R> behaviorCall = new BehaviorCall<>(behavior, executor, call);
    return (T)
        Proxy.newProxyInstance(
            service.getClassLoader(),
            new Class[] {service},
            (proxy, method, args) -> {
              ServiceMethodAdapterInfo adapterInfo = parseServiceMethodAdapterInfo(method);

              Annotation[] methodAnnotations = method.getAnnotations();
              CallAdapter<R, T> callAdapter =
                  (CallAdapter<R, T>)
                      retrofit.callAdapter(adapterInfo.responseType, methodAnnotations);

              T adapted = callAdapter.adapt(behaviorCall);
              if (!adapterInfo.isSuspend) {
                return adapted;
              }

              Call<Object> adaptedCall = (Call<Object>) adapted;
              Continuation<Object> continuation = (Continuation<Object>) args[args.length - 1];
              try {
                return adapterInfo.wantsResponse
                    ? KotlinExtensions.awaitResponse(adaptedCall, continuation)
                    : KotlinExtensions.await(adaptedCall, continuation);
              } catch (Exception e) {
                return KotlinExtensions.suspendAndThrow(e, continuation);
              }
            });
  }

  /**
   * Computes the adapter type of the method for lookup via {@link Retrofit#callAdapter} as well as
   * information on whether the method is a {@code suspend fun}.
   *
   * <p>In the case of a Kotlin {@code suspend fun}, the last parameter type is a {@code
   * Continuation} whose parameter carries the actual response type. In this case, we return {@code
   * Call<T>} where {@code T} is the body type.
   */
  private static ServiceMethodAdapterInfo parseServiceMethodAdapterInfo(Method method) {
    Type[] genericParameterTypes = method.getGenericParameterTypes();
    if (genericParameterTypes.length != 0) {
      Type lastParameterType = genericParameterTypes[genericParameterTypes.length - 1];
      if (lastParameterType instanceof ParameterizedType) {
        ParameterizedType parameterizedLastParameterType = (ParameterizedType) lastParameterType;
        try {
          if (parameterizedLastParameterType.getRawType() == Continuation.class) {
            Type resultType = parameterizedLastParameterType.getActualTypeArguments()[0];
            if (resultType instanceof WildcardType) {
              resultType = ((WildcardType) resultType).getLowerBounds()[0];
            }
            if (resultType instanceof ParameterizedType) {
              ParameterizedType parameterizedResultType = (ParameterizedType) resultType;
              if (parameterizedResultType.getRawType() == Response.class) {
                Type bodyType = parameterizedResultType.getActualTypeArguments()[0];
                Type callType = new CallParameterizedTypeImpl(bodyType);
                return new ServiceMethodAdapterInfo(true, true, callType);
              }
            }
            Type callType = new CallParameterizedTypeImpl(resultType);
            return new ServiceMethodAdapterInfo(true, false, callType);
          }
        } catch (NoClassDefFoundError ignored) {
          // Not using coroutines.
        }
      }
    }
    return new ServiceMethodAdapterInfo(false, false, method.getGenericReturnType());
  }

  static final class CallParameterizedTypeImpl implements ParameterizedType {
    private final Type bodyType;

    CallParameterizedTypeImpl(Type bodyType) {
      this.bodyType = bodyType;
    }

    @Override
    public Type[] getActualTypeArguments() {
      return new Type[] {bodyType};
    }

    @Override
    public Type getRawType() {
      return Call.class;
    }

    @Override
    public @Nullable Type getOwnerType() {
      return null;
    }
  }

  static class ServiceMethodAdapterInfo {
    final boolean isSuspend;
    /**
     * Whether the suspend function return type was {@code Response<T>}. Only meaningful if {@link
     * #isSuspend} is true.
     */
    final boolean wantsResponse;

    final Type responseType;

    ServiceMethodAdapterInfo(boolean isSuspend, boolean wantsResponse, Type responseType) {
      this.isSuspend = isSuspend;
      this.wantsResponse = wantsResponse;
      this.responseType = responseType;
    }
  }
}
