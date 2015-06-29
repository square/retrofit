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
package retrofit;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.ResponseBody;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;
import retrofit.http.Streaming;

import static retrofit.Utils.methodError;

abstract class MethodHandler {
  @SuppressWarnings("unchecked")
  static MethodHandler create(Method method, OkHttpClient client, BaseUrl baseUrl,
      CallAdapter.Factory callAdapterFactory, Converter.Factory converterFactory,
      Executor callbackExecutor) {
    RequestFactory requestFactory = RequestFactoryParser.parse(method, baseUrl, converterFactory);

    if (method.getReturnType() == WebSocketCall.class) {
      return WebSocketHandler.create(method, client, requestFactory, converterFactory,
          callbackExecutor);
    } else {
      return CallHandler.create(method, client, requestFactory, callAdapterFactory,
          converterFactory);
    }
  }

  final OkHttpClient client;
  final RequestFactory requestFactory;

  private MethodHandler(OkHttpClient client, RequestFactory requestFactory) {
    this.client = client;
    this.requestFactory = requestFactory;
  }

  abstract Object invoke(Object... args);

  static final class CallHandler<T> extends MethodHandler {
    @SuppressWarnings("unchecked")
    static <T> MethodHandler create(Method method, OkHttpClient client,
        RequestFactory requestFactory, CallAdapter.Factory callAdapterFactory,
        Converter.Factory converterFactory) {
      CallAdapter<T> callAdapter = (CallAdapter<T>) createCallAdapter(method, callAdapterFactory);
      Type responseType = callAdapter.responseType();
      Converter<T> responseConverter =
          (Converter<T>) createResponseConverter(method, responseType, converterFactory);
      return new CallHandler<>(client, requestFactory, callAdapter, responseConverter);
    }

    private static CallAdapter<?> createCallAdapter(Method method,
        CallAdapter.Factory adapterFactory) {
      Type returnType = method.getGenericReturnType();
      if (Utils.hasUnresolvableType(returnType)) {
        throw methodError(method,
            "Method return type must not include a type variable or wildcard: %s", returnType);
      }

      if (returnType == void.class) {
        throw methodError(method, "Service methods cannot return void.");
      }

      CallAdapter<?> adapter = adapterFactory.get(returnType);
      if (adapter == null) {
        throw methodError(method, "Call adapter factory '%s' was unable to handle return type %s",
            adapterFactory, returnType);
      }
      return adapter;
    }

    private static Converter<?> createResponseConverter(Method method, Type responseType,
        Converter.Factory converterFactory) {
      if (responseType == ResponseBody.class) {
        boolean isStreaming = method.isAnnotationPresent(Streaming.class);
        return new OkHttpResponseBodyConverter(isStreaming);
      }

      if (converterFactory == null) {
        throw methodError(method, "Method response type is "
            + responseType
            + " but no converter factory registered. "
            + "Either add a converter factory to the Retrofit instance or use ResponseBody.");
      }

      Converter<?> converter = converterFactory.get(responseType);
      if (converter == null) {
        throw methodError(method, "Converter factory '%s' was unable to handle response type %s",
            converterFactory, responseType);
      }
      return converter;
    }

    private final CallAdapter<T> callAdapter;
    private final Converter<T> responseConverter;

    private CallHandler(OkHttpClient client, RequestFactory requestFactory,
        CallAdapter<T> callAdapter, Converter<T> responseConverter) {
      super(client, requestFactory);
      this.callAdapter = callAdapter;
      this.responseConverter = responseConverter;
    }

    @Override Object invoke(Object... args) {
      return callAdapter.adapt(new OkHttpCall<>(client, requestFactory, responseConverter, args));
    }
  }

  static final class WebSocketHandler<I, O> extends MethodHandler {
    static MethodHandler create(Method method, OkHttpClient client, RequestFactory requestFactory,
        Converter.Factory converterFactory, Executor callbackExecutor) {
      Type returnType = method.getGenericReturnType();
      Type incomingType = Utils.getParameterUpperBound(0, (ParameterizedType) returnType);
      Converter<?> incomingConverter = converterFactory.get(incomingType);
      Type outgoingType = Utils.getParameterUpperBound(1, (ParameterizedType) returnType);
      Converter<?> outgoingConverter = converterFactory.get(outgoingType);
      return new WebSocketHandler<>(client, requestFactory, incomingConverter, outgoingConverter,
          callbackExecutor);
    }

    private final Converter<I> incomingConverter;
    private final Converter<O> outgoingConverter;
    private final Executor callbackExecutor;

    private WebSocketHandler(OkHttpClient client, RequestFactory requestFactory,
        Converter<I> incomingConverter, Converter<O> outgoingConverter, Executor callbackExecutor) {
      super(client, requestFactory);
      this.incomingConverter = incomingConverter;
      this.outgoingConverter = outgoingConverter;
      this.callbackExecutor = callbackExecutor;
    }

    @Override Object invoke(Object... args) {
      return new OkHttpWebSocketCall<>(client, requestFactory, incomingConverter, outgoingConverter,
          callbackExecutor, args);
    }
  }
}
