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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import kotlin.Unit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.http.GET;
import retrofit2.http.HEAD;

public final class KotlinUnitTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/")
    Call<Unit> empty();

    @HEAD("/")
    Call<Unit> head();
  }

  @Test
  public void unitGet() throws IOException {
    Retrofit retrofit = new Retrofit.Builder().baseUrl(server.url("/")).build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    Response<Unit> response = example.empty().execute();
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isSameAs(Unit.INSTANCE);
  }

  @Test
  public void unitHead() throws IOException {
    Retrofit retrofit = new Retrofit.Builder().baseUrl(server.url("/")).build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    Response<Unit> response = example.head().execute();
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isSameAs(Unit.INSTANCE);
  }
}
