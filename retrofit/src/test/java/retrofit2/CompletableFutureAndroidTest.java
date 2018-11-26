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

import java.util.concurrent.CompletableFuture;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import retrofit2.helpers.ToStringConverterFactory;
import retrofit2.http.GET;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.robolectric.annotation.Config.NEWEST_SDK;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = NEWEST_SDK, manifest = NONE)
public final class CompletableFutureAndroidTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/") CompletableFuture<String> endpoint();
  }

  private Service service;

  @Before public void setUp() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .build();
    service = retrofit.create(Service.class);
  }

  @Config(sdk = 24)
  @Test public void completableFutureApi24() throws Exception {
    server.enqueue(new MockResponse().setBody("Hi"));

    CompletableFuture<String> future = service.endpoint();
    assertThat(future.get()).isEqualTo("Hi");
  }

  @Config(sdk = 21)
  @Test public void completableFuturePreApi24() {
    try {
      service.endpoint();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Unable to create call adapter for java.util.concurrent.CompletableFuture<java.lang.String>\n"
              + "    for method Service.endpoint");
    }
  }
}
