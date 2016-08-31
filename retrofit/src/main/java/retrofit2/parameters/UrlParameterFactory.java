/*
 * Copyright (C) 2016 Square, Inc.
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
package retrofit2.parameters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;

import okhttp3.HttpUrl;
import retrofit2.ParameterHandler;
import retrofit2.RequestBuilder;
import retrofit2.Retrofit;
import retrofit2.http.Url;

public class UrlParameterFactory implements ParameterHandler.Factory {

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof Url) {
      if (type == HttpUrl.class
          || type == String.class
          || type == URI.class
          || (type instanceof Class && "android.net.Uri".equals(((Class<?>) type).getName()))) {
        return new UrlParameter();
      } else {
        throw new IllegalArgumentException(
            "@Url must be okhttp3.HttpUrl, String, java.net.URI, or android.net.Uri type.");
      }
    }
    return null;
  }

  static final class UrlParameter implements ParameterHandler<Object> {
    @Override
    public void apply(RequestBuilder builder, Object value) {
      builder.setRelativeUrl(value);
    }
  }
}
