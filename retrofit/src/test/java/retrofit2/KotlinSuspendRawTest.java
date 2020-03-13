/*
 * Copyright (C) 2018 Square, Inc.
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

import kotlin.Result;
import kotlin.coroutines.Continuation;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.http.GET;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * This code path can only be tested from Java because Kotlin does not allow you specify a raw
 * Response type. Win! We still test this codepath for completeness.
 */
public final class KotlinSuspendRawTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/")
    Object body(Continuation<? super Response> continuation);
    @GET("/")
    Object result(Continuation<? super Result> continuation);
    @GET("/")
    Object resultResponse(Continuation<? super Result<Response>> continuation);
  }

  @Test public void body() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    Service service = retrofit.create(Service.class);

    try {
      service.body(null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Response must include generic type (e.g., Response<String>)\n"
          + "    for method Service.body");
    }
  }

  @Test public void result() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    Service service = retrofit.create(Service.class);

    try {
      service.result(null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Result must include generic type (e.g., Result<String>)\n"
          + "    for method Service.result");
    }
  }

  @Test public void resultResponse() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .build();
    Service service = retrofit.create(Service.class);

    try {
      service.resultResponse(null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Response must include generic type (e.g., Response<String>)\n"
          + "    for method Service.resultResponse");
    }
  }
}
