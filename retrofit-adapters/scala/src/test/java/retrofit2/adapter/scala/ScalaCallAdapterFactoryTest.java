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
package retrofit2.adapter.scala;

import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;
import scala.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class ScalaCallAdapterFactoryTest {
  private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

  @Rule public final MockWebServer server = new MockWebServer();

  private final CallAdapter.Factory factory = ScalaCallAdapterFactory.create();
  private Retrofit retrofit;

  @Before public void setUp() {
    retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(factory)
        .build();
  }

  @Test public void responseType() {
    Type bodyClass = new TypeToken<Future<String>>() {}.getType();
    assertThat(factory.get(bodyClass, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(String.class);
    Type bodyWildcard = new TypeToken<Future<? extends String>>() {}.getType();
    assertThat(factory.get(bodyWildcard, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(String.class);
    Type bodyGeneric = new TypeToken<Future<List<String>>>() {}.getType();
    assertThat(factory.get(bodyGeneric, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(new TypeToken<List<String>>() {}.getType());
    Type responseClass = new TypeToken<Future<Response<String>>>() {}.getType();
    assertThat(factory.get(responseClass, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(String.class);
    Type responseWildcard = new TypeToken<Future<Response<? extends String>>>() {}.getType();
    assertThat(factory.get(responseWildcard, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(String.class);
    Type resultClass = new TypeToken<Future<Response<String>>>() {}.getType();
    assertThat(factory.get(resultClass, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(String.class);
    Type resultWildcard = new TypeToken<Future<Response<? extends String>>>() {}.getType();
    assertThat(factory.get(resultWildcard, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(String.class);
  }

  @Test public void nonListenableFutureReturnsNull() {
    CallAdapter<?, ?> adapter = factory.get(String.class, NO_ANNOTATIONS, retrofit);
    assertThat(adapter).isNull();
  }

  @Test public void rawTypeThrows() {
    Type observableType = new TypeToken<Future>() {}.getType();
    try {
      factory.get(observableType, NO_ANNOTATIONS, retrofit);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage(
          "Future return type must be parameterized as Future<Foo> or Future<? extends Foo>");
    }
  }

  @Test public void rawResponseTypeThrows() {
    Type observableType = new TypeToken<Future<Response>>() {}.getType();
    try {
      factory.get(observableType, NO_ANNOTATIONS, retrofit);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage(
          "Response must be parameterized as Response<Foo> or Response<? extends Foo>");
    }
  }
}
