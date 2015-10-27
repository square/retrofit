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
package retrofit;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class CallableRequestTest {
  @Rule public final MockWebServer server = new MockWebServer();

  @Test public void http200Sync() throws IOException {
    server.enqueue(new MockResponse().setBody("Hi"));

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/").toString())
        .addConverterFactory(new StringConverterFactory())
        .build();

    CallableRequest request = new CallableRequest.Builder(retrofit)
        .path("/")
        .responseType(String.class)
        .method(Method.GET)
        .build();

    Call<String> call = request.newCall();

    Response<String> response = call.execute();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void http200Async() throws InterruptedException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/").toString())
        .addConverterFactory(new StringConverterFactory())
        .build();

    CallableRequest request = new CallableRequest.Builder(retrofit)
        .path("/")
        .responseType(String.class)
        .method(Method.GET)
        .build();

    Call<String> call = request.newCall();

    server.enqueue(new MockResponse().setBody("Hi"));

    final AtomicReference<Response<String>> responseRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    call.enqueue(new Callback<String>() {
      @Override public void onResponse(Response<String> response, Retrofit retrofit) {
        responseRef.set(response);
        latch.countDown();
      }

      @Override public void onFailure(Throwable t) {
        t.printStackTrace();
      }
    });
    assertTrue(latch.await(2, SECONDS));

    Response<String> response = responseRef.get();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }
}
