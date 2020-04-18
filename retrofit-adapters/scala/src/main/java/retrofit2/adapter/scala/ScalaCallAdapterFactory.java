/*
 * Copyright (C) 2017 Square, Inc.
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
package retrofit2.adapter.scala;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;
import scala.concurrent.Future;

/**
 * A {@linkplain CallAdapter.Factory call adapter} which creates Scala futures.
 *
 * <p>Adding this class to {@link Retrofit} allows you to return {@link Future} from service
 * methods.
 *
 * <pre><code>
 * interface MyService {
 *   &#64;GET("user/me")
 *   Future&lt;User&gt; getUser()
 * }
 * </code></pre>
 *
 * There are two configurations supported for the {@code Future} type parameter:
 *
 * <ul>
 *   <li>Direct body (e.g., {@code Future<User>}) returns the deserialized body for 2XX responses,
 *       sets {@link retrofit2.HttpException HttpException} errors for non-2XX responses, and sets
 *       {@link IOException} for network errors.
 *   <li>Response wrapped body (e.g., {@code Future<Response<User>>}) returns a {@link Response}
 *       object for all HTTP responses and sets {@link IOException} for network errors
 * </ul>
 */
public final class ScalaCallAdapterFactory extends CallAdapter.Factory {
  public static ScalaCallAdapterFactory create() {
    return new ScalaCallAdapterFactory();
  }

  private ScalaCallAdapterFactory() {}

  @Override
  public @Nullable CallAdapter<?, ?> get(
      Type returnType, Annotation[] annotations, Retrofit retrofit) {
    if (getRawType(returnType) != Future.class) {
      return null;
    }
    if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalStateException(
          "Future return type must be parameterized as Future<Foo> or Future<? extends Foo>");
    }
    Type innerType = getParameterUpperBound(0, (ParameterizedType) returnType);

    if (getRawType(innerType) != Response.class) {
      // Generic type is not Response<T>. Use it for body-only adapter.
      return new BodyCallAdapter<>(innerType);
    }

    if (!(innerType instanceof ParameterizedType)) {
      throw new IllegalStateException(
          "Response must be parameterized as Response<Foo> or Response<? extends Foo>");
    }

    Type responseType = getParameterUpperBound(0, (ParameterizedType) innerType);
    return new ResponseCallAdapter<>(responseType);
  }
}
