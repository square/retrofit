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
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import retrofit2.http.GET;
import retrofit2.http.Url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.annotation.Config.NEWEST_SDK;
import static org.robolectric.annotation.Config.NONE;
import static retrofit2.RequestFactoryTest.buildRequest;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = NEWEST_SDK, manifest = NONE)
@SuppressWarnings({"UnusedParameters", "unused"}) // Parameters inspected reflectively.
public final class RequestFactoryAndroidTest {
  @Test public void getWithAndroidUriUrl() {
    class Example {
      @GET
      Call<ResponseBody> method(@Url Uri url) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, Uri.parse("foo/bar/"));
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test public void getWithAndroidUriUrlAbsolute() {
    class Example {
      @GET
      Call<ResponseBody> method(@Url Uri url) {
        return null;
      }
    }

    Request request = buildRequest(Example.class, Uri.parse("https://example2.com/foo/bar/"));
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("https://example2.com/foo/bar/");
    assertThat(request.body()).isNull();
  }
}
