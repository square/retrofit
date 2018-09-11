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
package retrofit2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import kotlin.coroutines.Continuation;
import okhttp3.Call;
import okhttp3.ResponseBody;

import static retrofit2.Utils.getRawType;
import static retrofit2.Utils.methodError;

/** Adapts an invocation of an interface method into an HTTP call. */
abstract class HttpServiceMethod<ResponseT, ReturnT> extends ServiceMethod<ReturnT> {
  /**
   * Inspects the annotations on an interface method to construct a reusable service method that
   * speaks HTTP. This requires potentially-expensive reflection so it is best to build each service
   * method only once and reuse it.
   */
  static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
      Retrofit retrofit, Method method, RequestFactory requestFactory) {
    CallAdapter<ResponseT, ReturnT> callAdapter = null;
    boolean continuationWantsResponse = false;
    boolean continuationBodyNullable = false;
    Type responseType;
    if (requestFactory.isKotlinSuspendFunction) {
      Type[] parameterTypes = method.getGenericParameterTypes();
      Type continuationType = parameterTypes[parameterTypes.length - 1];
      responseType = Utils.getParameterLowerBound(0, (ParameterizedType) continuationType);
      if (getRawType(responseType) == Response.class && responseType instanceof ParameterizedType) {
        // Unwrap the actual body type from Response<T>.
        responseType = Utils.getParameterUpperBound(0, (ParameterizedType) responseType);
        continuationWantsResponse = true;
      } else {
        // TODO figure out if type is nullable or not
        // Metadata metadata = method.getDeclaringClass().getAnnotation(Metadata.class)
        // Find the entry for method
        // Determine if return type is nullable or not
      }
    } else {
      callAdapter = createCallAdapter(retrofit, method);
      responseType = callAdapter.responseType();
    }

    if (responseType == okhttp3.Response.class) {
      throw methodError(method, "'"
          + getRawType(responseType).getName()
          + "' is not a valid response body type. Did you mean ResponseBody?");
    }
    if (responseType == Response.class) {
      throw methodError(method, "Response must include generic type (e.g., Response<String>)");
    }
    // TODO support Unit for Kotlin?
    if (requestFactory.httpMethod.equals("HEAD") && !Void.class.equals(responseType)) {
      throw methodError(method, "HEAD method must use Void as response type.");
    }

    Converter<ResponseBody, ResponseT> responseConverter =
        createResponseConverter(retrofit, method, responseType);

    okhttp3.Call.Factory callFactory = retrofit.callFactory;
    if (callAdapter != null) {
      return new CallAdapted<>(requestFactory, callFactory, callAdapter, responseConverter);
    } else if (continuationWantsResponse) {
      //noinspection unchecked Kotlin compiler guarantees ReturnT to be Object.
      return (HttpServiceMethod<ResponseT, ReturnT>) new SuspendForResponse<>(requestFactory,
          callFactory, responseConverter);
    } else {
      //noinspection unchecked Kotlin compiler guarantees ReturnT to be Object.
      return (HttpServiceMethod<ResponseT, ReturnT>) new SuspendForBody<>(requestFactory,
          callFactory, responseConverter, continuationBodyNullable);
    }
  }

  private static <ResponseT, ReturnT> CallAdapter<ResponseT, ReturnT> createCallAdapter(
      Retrofit retrofit, Method method) {
    Type returnType = method.getGenericReturnType();
    Annotation[] annotations = method.getAnnotations();
    try {
      //noinspection unchecked
      return (CallAdapter<ResponseT, ReturnT>) retrofit.callAdapter(returnType, annotations);
    } catch (RuntimeException e) { // Wide exception range because factories are user code.
      throw methodError(method, e, "Unable to create call adapter for %s", returnType);
    }
  }

  private static <ResponseT> Converter<ResponseBody, ResponseT> createResponseConverter(
      Retrofit retrofit, Method method, Type responseType) {
    Annotation[] annotations = method.getAnnotations();
    try {
      return retrofit.responseBodyConverter(responseType, annotations);
    } catch (RuntimeException e) { // Wide exception range because factories are user code.
      throw methodError(method, e, "Unable to create converter for %s", responseType);
    }
  }

  private final RequestFactory requestFactory;
  private final okhttp3.Call.Factory callFactory;
  private final Converter<ResponseBody, ResponseT> responseConverter;

  HttpServiceMethod(RequestFactory requestFactory, Call.Factory callFactory,
      Converter<ResponseBody, ResponseT> responseConverter) {
    this.requestFactory = requestFactory;
    this.callFactory = callFactory;
    this.responseConverter = responseConverter;
  }

  @Override final @Nullable ReturnT invoke(Object[] args) {
    return adapt(new OkHttpCall<>(requestFactory, args, callFactory, responseConverter), args);
  }

  protected abstract @Nullable ReturnT adapt(OkHttpCall<ResponseT> call, Object[] args);

  static final class CallAdapted<ResponseT, ReturnT> extends HttpServiceMethod<ResponseT, ReturnT> {
    private final CallAdapter<ResponseT, ReturnT> callAdapter;

    CallAdapted(RequestFactory requestFactory, Call.Factory callFactory,
        CallAdapter<ResponseT, ReturnT> callAdapter,
        Converter<ResponseBody, ResponseT> responseConverter) {
      super(requestFactory, callFactory, responseConverter);
      this.callAdapter = callAdapter;
    }

    @Override protected ReturnT adapt(OkHttpCall<ResponseT> call, Object[] args) {
      return callAdapter.adapt(call);
    }
  }

  static final class SuspendForResponse<ResponseT> extends HttpServiceMethod<ResponseT, Object> {
    SuspendForResponse(RequestFactory requestFactory, Call.Factory callFactory,
        Converter<ResponseBody, ResponseT> responseConverter) {
      super(requestFactory, callFactory, responseConverter);
    }

    @Override protected Object adapt(OkHttpCall<ResponseT> call, Object[] args) {
      Continuation<Response<ResponseT>> continuation =
          (Continuation<Response<ResponseT>>) args[args.length - 1];
      return KotlinExtensions.awaitResponse(call, continuation);
    }
  }

  static final class SuspendForBody<ResponseT> extends HttpServiceMethod<ResponseT, Object> {
    private final boolean isNullable;

    SuspendForBody(RequestFactory requestFactory, Call.Factory callFactory,
        Converter<ResponseBody, ResponseT> responseConverter, boolean isNullable) {
      super(requestFactory, callFactory, responseConverter);
      this.isNullable = isNullable;
    }

    @Override protected Object adapt(OkHttpCall<ResponseT> call, Object[] args) {
      Continuation<ResponseT> continuation = (Continuation<ResponseT>) args[args.length - 1];
      return isNullable
          ? KotlinExtensions.awaitNullable(call, continuation)
          : KotlinExtensions.await(call, continuation);
    }
  }
}
