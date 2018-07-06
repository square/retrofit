/*
 * Copyright (C) 2018 Square, Inc.
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
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import retrofit2.internal.WebSocketCall;

final class WebSocketServiceMethod extends ServiceMethod<WebSocketCall<?, ?>> {
  private final okhttp3.WebSocket.Factory rawWebSocketFactory;
  private final RequestFactory requestFactory;
  private final Converter<ResponseBody, Object> inConverter;
  private final Converter<Object, RequestBody> outConverter;

  WebSocketServiceMethod(WebSocket.Factory rawWebSocketFactory, RequestFactory requestFactory,
      Converter<ResponseBody, Object> inConverter, Converter<Object, RequestBody> outConverter) {
    this.rawWebSocketFactory = rawWebSocketFactory;
    this.requestFactory = requestFactory;
    this.inConverter = inConverter;
    this.outConverter = outConverter;
  }

  @Override WebSocketCall<?, ?> invoke(@Nullable Object[] args) {
    return new OkHttpWebSocketCall<>(rawWebSocketFactory, requestFactory, args, inConverter,
        outConverter);
  }

  static final class Builder {
    private final Retrofit retrofit;
    private final Method method;

    Builder(Retrofit retrofit, Method method) {
      this.retrofit = retrofit;
      this.method = method;
    }

    ServiceMethod<WebSocketCall<?, ?>> build() {
      Type returnType = method.getGenericReturnType();
      if (!(returnType instanceof ParameterizedType)) {
        throw Utils.methodError(method, ""); // TODO error message
      }
      ParameterizedType parameterizedReturnType = (ParameterizedType) returnType;
      Type inType = Utils.getParameterUpperBound(0, parameterizedReturnType);
      Type outType = Utils.getParameterUpperBound(1, parameterizedReturnType);

      Annotation[] methodAnnotations = method.getAnnotations();

      Converter<ResponseBody, Object> inConverter =
          retrofit.incomingMessageConverter(inType, methodAnnotations);
      Converter<Object, RequestBody> outConverter =
          retrofit.outgoingMessageConverter(outType, methodAnnotations);

      RequestFactory requestFactory = RequestFactory.parseAnnotations(retrofit, method);

      return new WebSocketServiceMethod(retrofit.webSocketFactory, requestFactory, inConverter,
          outConverter);
    }
  }
}
