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
package retrofit2.adapter.rxjava;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import rx.Completable;

import java.io.IOException;

import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class CompletableTest {
  @Rule
  public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/") Completable completable();
  }

  private Service service;

  @Before public void setUp() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build();
    service = retrofit.create(Service.class);
  }

  @Test public void completableSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    service.completable().await();
  }

  @Test public void completableSuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    try {
      service.completable().await();
      fail();
    } catch (RuntimeException e) {
      Throwable cause = e.getCause();
      assertThat(cause).isInstanceOf(HttpException.class).hasMessage("HTTP 404 Client Error");
    }
  }

  @Test public void completableFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    try {
      service.completable().await();
      fail();
    } catch (RuntimeException e) {
      assertThat(e.getCause()).isInstanceOf(IOException.class);
    }
  }
}
