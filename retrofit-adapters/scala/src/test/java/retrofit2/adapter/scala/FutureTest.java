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

import static java.util.concurrent.TimeUnit.SECONDS;
import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public final class FutureTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/")
    Future<String> body();

    @GET("/")
    Future<Response<String>> response();
  }

  private Service service;

  @Before
  public void setUp() {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(new StringConverterFactory())
            .addCallAdapterFactory(ScalaCallAdapterFactory.create())
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void bodySuccess200() throws Exception {
    server.enqueue(new MockResponse().setBody("Hi"));

    Future<String> future = service.body();
    String result = Await.result(future, Duration.create(5, SECONDS));
    assertThat(result).isEqualTo("Hi");
  }

  @Test
  public void bodySuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    Future<String> future = service.body();
    try {
      Await.result(future, Duration.create(5, SECONDS));
      fail();
    } catch (Exception e) {
      assertThat(e)
          .isInstanceOf(HttpException.class) // Required for backwards compatibility.
          .isInstanceOf(retrofit2.HttpException.class)
          .hasMessage("HTTP 404 Client Error");
    }
  }

  @Test
  public void bodyFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    Future<String> future = service.body();
    try {
      Await.result(future, Duration.create(5, SECONDS));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IOException.class);
    }
  }

  @Test
  public void responseSuccess200() throws Exception {
    server.enqueue(new MockResponse().setBody("Hi"));

    Future<Response<String>> future = service.response();
    Response<String> response = Await.result(future, Duration.create(5, SECONDS));
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test
  public void responseSuccess404() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    Future<Response<String>> future = service.response();
    Response<String> response = Await.result(future, Duration.create(5, SECONDS));
    assertThat(response.isSuccessful()).isFalse();
    assertThat(response.errorBody().string()).isEqualTo("Hi");
  }

  @Test
  public void responseFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    Future<Response<String>> future = service.response();
    try {
      Await.result(future, Duration.create(5, SECONDS));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IOException.class);
    }
  }
}
