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
package retrofit2.adapter.sse;

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Streaming;

public class EventSourceCallAdapterFactoryTest {

  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @Streaming
    @GET("/")
    EventSource<Integer, String, String> sse();
  }

  private Service service;

  @Before
  public void setUp() {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(ScalarsConverterFactory.create())
            .addCallAdapterFactory(EventSourceCallAdapterFactory.INSTANCE)
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void simpleEvents() throws Exception {
    MockResponse mockResponse =
        new MockResponse()
            .setHeader("Content-Type", "text/event-stream")
            .setBody("id: 1\nevent: type1\ndata: foo\n\nid: 2\ndata: bar\n\n");
    server.enqueue(mockResponse);

    CompletableFuture<Void> completableFuture = new CompletableFuture<>();

    service
        .sse()
        .subscribe(
            new SseCallback<Integer, String, String>() {
              private final AtomicInteger count = new AtomicInteger(0);

              @Override
              public void onOpen(@NotNull EventSource<Integer, String, String> eventSource) {
                assertThat(count.get()).isEqualTo(0);
              }

              @Override
              public void onEvent(
                  @NotNull EventSource<Integer, String, String> eventSource,
                  @Nullable Integer id,
                  @Nullable String type,
                  @NotNull String data) {
                switch (count.incrementAndGet()) {
                  case 1:
                    assertThat(id).isEqualTo(1);
                    assertThat(type).isEqualTo("type1");
                    assertThat(data).isEqualTo("foo");
                    break;
                  case 2:
                    assertThat(id).isEqualTo(2);
                    assertThat(type).isEqualTo(null);
                    assertThat(data).isEqualTo("bar");
                    break;
                }
              }

              @Override
              public void onClosed(@NotNull EventSource<Integer, String, String> eventSource) {
                completableFuture.complete(null);
              }

              @Override
              public void onFailure(
                  @NotNull EventSource<Integer, String, String> eventSource,
                  @Nullable Throwable t) {
                completableFuture.completeExceptionally(t);
              }
            });

    completableFuture.get(5, TimeUnit.SECONDS);
  }
}
