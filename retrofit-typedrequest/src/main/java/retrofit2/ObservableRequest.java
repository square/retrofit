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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import rx.Observable;

public final class ObservableRequest extends TypedRequest {
  ObservableRequest(Retrofit retrofit, ParameterizedType returnType, BodyEncoding bodyEncoding,
      String path, Method method, Object body, Object tag, List<Query> query,
      Map<String, String> headers, List<Part> parts, List<Field> fields) {
    super(retrofit, returnType, bodyEncoding,
        path, method, body, tag, query, headers, parts, fields);
  }

  public Builder newBuilder(Retrofit retrofit) {
    return new Builder(retrofit, this);
  }

  public static final class Builder extends TypedRequest.Builder {

    public Builder(Retrofit retrofit) {
      super(retrofit);
    }

    public Builder(Retrofit retrofit, ObservableRequest observableRequest) {
      super(retrofit, observableRequest);
    }

    @Override protected TypedRequest newRequest() {
      ParameterizedType returnType = new ParameterizedType() {
        @Override public Type[] getActualTypeArguments() {
          return new Type[] { responseType };
        }

        @Override public Type getRawType() {
          return Observable.class;
        }

        @Override public Type getOwnerType() {
          return null;
        }
      };
      return new ObservableRequest(retrofit, returnType, bodyEncoding, path, method, body, tag,
          query, headers, parts, fields);
    }

    @Override public Builder path(String path) {
      return (Builder) super.path(path);
    }

    @Override public Builder method(Method method) {
      return (Builder) super.method(method);
    }

    @Override public Builder tag(Object tag) {
      return (Builder) super.tag(tag);
    }

    @Override public Builder body(Object body) {
      return (Builder) super.body(body);
    }

    @Override public Builder queryParams(List<Query> query) {
      return (Builder) super.queryParams(query);
    }

    @Override public Builder headers(Map<String, String> headers) {
      return (Builder) super.headers(headers);
    }

    @Override public Builder parts(List<Part> parts) {
      return (Builder) super.parts(parts);
    }

    @Override public Builder fields(List<Field> fields) {
      return (Builder) super.fields(fields);
    }

    @Override public Builder responseType(Type responseType) {
      return (Builder) super.responseType(responseType);
    }

    @Override public ObservableRequest build() {
      return (ObservableRequest) super.build();
    }
  }
}
