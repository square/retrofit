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
package retrofit.converter.java8;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;
import javax.annotation.Nullable;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.http.GET;

public final class Java8OptionalConverterFactoryTest {
  interface Service {
    @GET("/")
    Call<Optional<Object>> optional();

    @GET("/")
    Call<Object> object();
  }

  @Rule public final MockWebServer server = new MockWebServer();

  private Service service;

  @Before
  public void setUp() {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(Java8OptionalConverterFactory.create())
            .addConverterFactory(new AlwaysNullConverterFactory())
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void optional() throws IOException {
    server.enqueue(new MockResponse());

    Optional<Object> optional = service.optional().execute().body();
    assertThat(optional).isNotNull();
    assertThat(optional.isPresent()).isFalse();
  }

  @Test
  public void onlyMatchesOptional() throws IOException {
    server.enqueue(new MockResponse());

    Object body = service.object().execute().body();
    assertThat(body).isNull();
  }

  @Test
  public void delegates() throws IOException {
    Object object = new Object();
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(
                new Converter.Factory() {
                  @Nullable
                  @Override
                  public Converter<ResponseBody, ?> responseBodyConverter(
                      Type type, Annotation[] annotations, Retrofit retrofit) {
                    if (getRawType(type) != Object.class) {
                      return null;
                    }
                    return value -> object;
                  }
                })
            .addConverterFactory(Java8OptionalConverterFactory.create())
            .build();

    server.enqueue(new MockResponse());

    Service service = retrofit.create(Service.class);
    Optional<Object> optional = service.optional().execute().body();
    assertThat(optional).isNotNull();
    assertThat(optional.get()).isSameAs(object);
  }
}
