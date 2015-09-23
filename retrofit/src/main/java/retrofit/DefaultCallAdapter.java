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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * A call adapter that uses the same thread for both I/O and application-level callbacks. For
 * synchronous calls this is the application thread making the request; for asynchronous calls this
 * is a thread provided by OkHttp's dispatcher.
 */
final class DefaultCallAdapter implements CallAdapter<Call<?>> {
  static final Factory FACTORY = new Factory() {
    @Override
    public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
      if (Utils.getRawType(returnType) != Call.class) {
        return null;
      }
      Type responseType = Utils.getCallResponseType(returnType);
      return new DefaultCallAdapter(responseType);
    }
  };

  private final Type responseType;

  DefaultCallAdapter(Type responseType) {
    this.responseType = responseType;
  }

  @Override public Type responseType() {
    return responseType;
  }

  @Override public <R> Call<R> adapt(Call<R> call) {
    return call;
  }
}
