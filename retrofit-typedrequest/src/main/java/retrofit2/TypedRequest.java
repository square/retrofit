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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static retrofit2.Utils.checkNotNull;

public abstract class TypedRequest {
  enum BodyEncoding {
    NONE,
    MULTIPART,
    FORM_URL_ENCODED
  }

  protected final ParameterizedType returnType;
  protected final BodyEncoding bodyEncoding;
  protected final String path;
  protected final Method method;
  protected final Object body;
  protected final Object tag;
  protected final List<Query> query;
  protected final Map<String, String> headers;
  protected final List<Part> parts;
  protected final List<Field> fields;
  protected final CallAdapter callAdapter;
  protected final TypedRawCallFactory<Object> callFactory;
  protected boolean isCancelled;

  TypedRequest(Retrofit retrofit, ParameterizedType returnType,
      BodyEncoding bodyEncoding, String path, Method method, Object body, Object tag,
      List<Query> query, Map<String, String> headers, List<Part> parts, List<Field> fields) {
    this.returnType = returnType;
    this.path = path;
    this.method = method;
    this.body = body;
    this.tag = tag;
    this.query = query;
    this.headers = headers;
    this.parts = parts;
    this.fields = fields;
    this.bodyEncoding = bodyEncoding;
    this.callAdapter = retrofit.callAdapter(returnType, new Annotation[0]);
    callFactory = new TypedRawCallFactory<>(retrofit, callAdapter.responseType(), this);
  }

  public String path() {
    return path;
  }

  public Method method() {
    return method;
  }

  public Object body() {
    return body;
  }

  public Object tag() {
    return tag;
  }

  public List<Query> queryParams() {
    return query;
  }

  public Map<String, String> headers() {
    return headers;
  }

  public List<Part> parts() {
    return parts;
  }

  public List<Field> fields() {
    return fields;
  }

  public BodyEncoding bodyEncoding() {
    return bodyEncoding;
  }

  public void cancel() {
    isCancelled = true;
  }

  public boolean isCancelled() {
    return isCancelled;
  }

  public ParameterizedType returnType() {
    return returnType;
  }

  @SuppressWarnings("unchecked")
  public <T> T newCall() {
    return (T) callAdapter.adapt(new OkHttpCall<>(callFactory, null));
  }

  private RuntimeException requestError(String message, Object... args) {
    if (args.length > 0) {
      message = String.format(message, args);
    }
    return new IllegalArgumentException(getClass().getSimpleName() + ": " + message);
  }

  public abstract static class Builder {
    protected final Retrofit retrofit;
    protected Type responseType;
    protected BodyEncoding bodyEncoding = BodyEncoding.NONE;
    protected String path;
    protected Method method;
    protected Object body;
    protected Object tag;
    protected List<Query> query = Collections.emptyList();
    protected Map<String, String> headers = Collections.emptyMap();
    protected List<Part> parts = Collections.emptyList();
    protected List<Field> fields = Collections.emptyList();

    public Builder(Retrofit retrofit, TypedRequest request) {
      this(retrofit);
      path = request.path;
      method = request.method;
      tag = request.tag;
      body = request.body;
      responseType = request.returnType.getActualTypeArguments()[0];

      if (query != null && !query.isEmpty()) {
        query = request.query;
      }
      if (headers != null && !headers.isEmpty()) {
        headers = request.headers;
      }
      if (fields != null && !fields.isEmpty()) {
        fields = request.fields;
        bodyEncoding = BodyEncoding.FORM_URL_ENCODED;
      }
      if (parts != null && !parts.isEmpty()) {
        parts = request.parts;
        bodyEncoding = BodyEncoding.MULTIPART;
      }
    }

    public Builder(Retrofit retrofit) {
      this.retrofit = retrofit;
    }

    public Builder path(String path) {
      this.path = path;
      return this;
    }

    public Builder method(Method method) {
      this.method = checkNotNull(method, "method == null");
      return this;
    }

    public Builder tag(Object tag) {
      this.tag = tag;
      return this;
    }

    public Builder body(Object body) {
      this.body = body;
      return this;
    }

    public Builder queryParams(List<Query> query) {
      this.query = checkNotNull(query, "query == null");
      return this;
    }

    public Builder headers(Map<String, String> headers) {
      this.headers = checkNotNull(headers, "headers == null");
      return this;
    }

    public Builder parts(List<Part> parts) {
      this.parts = checkNotNull(parts, "parts == null");
      bodyEncoding = BodyEncoding.MULTIPART;
      return this;
    }

    public Builder fields(List<Field> fields) {
      this.fields = checkNotNull(fields, "fields == null");
      bodyEncoding = BodyEncoding.FORM_URL_ENCODED;
      return this;
    }

    public Builder responseType(Type responseType) {
      this.responseType = responseType;
      return this;
    }

    public TypedRequest build() {
      checkNotNull(path, "path == null");
      checkNotNull(method, "method == null");
      checkNotNull(responseType, "responseType == null");

      if (path.length() == 0 || path.charAt(0) != '/') {
        throw new IllegalArgumentException("URL path \"" + path + "\" must start with '/'.");
      }

      boolean requestHasBody =
          method == Method.PATCH || method == Method.POST || method == Method.PUT;

      boolean gotBody = body != null;
      boolean gotField = !fields.isEmpty();
      boolean gotPart = !parts.isEmpty();

      if (bodyEncoding == BodyEncoding.NONE && !requestHasBody && gotBody) {
        throw new IllegalArgumentException("Non-body HTTP method cannot contain body.");
      }
      if (bodyEncoding == BodyEncoding.FORM_URL_ENCODED && !gotField) {
        throw new IllegalArgumentException("Form-encoded method must contain at least one field.");
      }
      if (bodyEncoding == BodyEncoding.MULTIPART && !gotPart) {
        throw new IllegalArgumentException("Multipart method must contain at least one part.");
      }

      return newRequest();
    }

    protected abstract TypedRequest newRequest();
  }
}
