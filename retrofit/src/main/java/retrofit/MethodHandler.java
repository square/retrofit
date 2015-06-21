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
import java.lang.reflect.Type;
import retrofit.http.Streaming;

import static retrofit.Utils.methodError;

final class MethodHandler<T> {
  @SuppressWarnings("unchecked")
  static MethodHandler<?> create(Method method, OkHttpClient client, Endpoint endpoint,
      CallAdapter.Factory callAdapterFactory, Converter.Factory converterFactory) {
    CallAdapter<Object> callAdapter =
        (CallAdapter<Object>) createCallAdapter(method, callAdapterFactory);
    Converter<Object> responseConverter =
        (Converter<Object>) createResponseConverter(method, callAdapter.responseType(),
            converterFactory);
    RequestFactory requestFactory = RequestFactoryParser.parse(method, endpoint, converterFactory);
    return new MethodHandler<>(client, requestFactory, callAdapter, responseConverter);
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

  private final OkHttpClient client;
  private final RequestFactory requestFactory;
  private final CallAdapter<T> callAdapter;
  private final Converter<T> responseConverter;

  private MethodHandler(OkHttpClient client, RequestFactory requestFactory,
      CallAdapter<T> callAdapter, Converter<T> responseConverter) {
    this.client = client;
    this.requestFactory = requestFactory;
    this.callAdapter = callAdapter;
    this.responseConverter = responseConverter;
  }

  Object invoke(Object... args) {
    return callAdapter.adapt(new OkHttpCall<>(client, requestFactory, responseConverter, args));
  }
}
