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

import static org.junit.Assert.assertNotNull;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.helpers.ToStringConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public final class Java8DefaultStaticMethodsInValidationTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Example {
    @GET("/")
    Call<String> user(@Query("name") String name);

    default Call<String> user() {
      return user("hey");
    }

    static String staticMethod() {
      return "Hi";
    }
  }

  @Test
  public void test() {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(new ToStringConverterFactory())
            .validateEagerly(true)
            .build();
    assertNotNull(retrofit.create(Example.class));
  }
}
