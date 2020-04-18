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
package retrofit2;

import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.helpers.ToStringConverterFactory;
import retrofit2.http.GET;

public final class CompletableFutureTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/")
    CompletableFuture<String> body();

    @GET("/")
    CompletableFuture<Response<String>> response();
  }

  private Service service;

  @Before
  public void setUp() {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(new ToStringConverterFactory())
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void bodySuccess200() throws Exception {
    server.enqueue(new MockResponse().setBody("Hi"));

    CompletableFuture<String> future = service.body();
    assertThat(future.get()).isEqualTo("Hi");
  }

  @Test
  public void bodySuccess404() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(404));

    CompletableFuture<String> future = service.body();
    try {
      future.get();
      fail();
    } catch (ExecutionException e) {
      assertThat(e.getCause())
          .isInstanceOf(HttpException.class) // Required for backwards compatibility.
          .isInstanceOf(HttpException.class)
          .hasMessage("HTTP 404 Client Error");
    }
  }

  @Test
  public void bodyFailure() throws Exception {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    CompletableFuture<String> future = service.body();
    try {
      future.get();
      fail();
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isInstanceOf(IOException.class);
    }
  }

  @Test
  public void responseSuccess200() throws Exception {
    server.enqueue(new MockResponse().setBody("Hi"));

    CompletableFuture<Response<String>> future = service.response();
    Response<String> response = future.get();
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test
  public void responseSuccess404() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    CompletableFuture<Response<String>> future = service.response();
    Response<String> response = future.get();
    assertThat(response.isSuccessful()).isFalse();
    assertThat(response.errorBody().string()).isEqualTo("Hi");
  }

  @Test
  public void responseFailure() throws Exception {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    CompletableFuture<Response<String>> future = service.response();
    try {
      future.get();
      fail();
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isInstanceOf(IOException.class);
    }
  }
}
