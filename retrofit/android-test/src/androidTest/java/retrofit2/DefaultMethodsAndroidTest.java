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

import androidx.test.filters.SdkSuppress;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.helpers.ToStringConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class DefaultMethodsAndroidTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Example {
    @GET("/")
    Call<String> user(@Query("name") String name);

    default Call<String> user() {
      return user("hey");
    }
  }

  @Test
  @SdkSuppress(minSdkVersion = 24, maxSdkVersion = 25)
  public void failsOnApi24And25() {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(new ToStringConverterFactory())
            .build();
    Example example = retrofit.create(Example.class);

    try {
      example.user();
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("Calling default methods on API 24 and 25 is not supported");
    }
  }

  @Test
  @SdkSuppress(minSdkVersion = 26)
  public void doesNotFailOnApi26() throws IOException, InterruptedException {
    server.enqueue(new MockResponse());
    server.enqueue(new MockResponse());

    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(new ToStringConverterFactory())
            .build();
    Example example = retrofit.create(Example.class);

    example.user().execute();
    assertThat(server.takeRequest().getRequestUrl().queryParameter("name")).isEqualTo("hey");

    example.user("hi").execute();
    assertThat(server.takeRequest().getRequestUrl().queryParameter("name")).isEqualTo("hi");
  }
}
