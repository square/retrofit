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
package retrofit2;

import android.net.Uri;
import java.io.IOException;
import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.http.GET;
import retrofit2.http.Url;

import static org.assertj.core.api.Assertions.assertThat;

public final class UriAndroidTest {
  @Rule public final MockWebServer server1 = new MockWebServer();
  @Rule public final MockWebServer server2 = new MockWebServer();

  interface Service {
    @GET
    Call<ResponseBody> method(@Url Uri url);
  }

  private Service service;

  @Before
  public void setUp() {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server1.url("/"))
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void getWithAndroidUriUrl() throws IOException, InterruptedException {
    server1.enqueue(new MockResponse().setBody("Hi"));

    service.method(Uri.parse("foo/bar/")).execute();
    assertThat(server1.takeRequest().getRequestUrl()).isEqualTo(server1.url("foo/bar/"));
  }

  @Test
  public void getWithAndroidUriUrlAbsolute() throws IOException, InterruptedException {
    server2.enqueue(new MockResponse().setBody("Hi"));

    HttpUrl url = server2.url("/");
    service.method(Uri.parse(url.toString())).execute();
    assertThat(server2.takeRequest().getRequestUrl()).isEqualTo(url);
  }
}
