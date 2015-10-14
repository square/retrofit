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
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit.http.Body;
import retrofit.http.POST;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class FastjsonConverterFactoryTest {

  static class AnObject {
    private String theName;

    public String getTheName() {
      return theName;
    }

    public void setTheName(String theName) {
      this.theName = theName;
    }
  }

  interface Service {
    @POST("/") Call<AnObject> anObject(@Body AnObject impl);
  }

  @Rule public final MockWebServer server = new MockWebServer();

  private Service service;

  @Before public void setUp() {
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(FastjsonConverterFactory.create())
            .build();
    service = retrofit.create(Service.class);
  }

  @Test public void anObject() throws IOException, InterruptedException {
    server.enqueue(new MockResponse().setBody("{\"theName\":\"value\"}"));
    AnObject requestBodyObject = new AnObject();
    requestBodyObject.setTheName("value");
    Call<AnObject> call = service.anObject(requestBodyObject);
    Response<AnObject> response = call.execute();
    AnObject body = response.body();
    assertThat(body.theName).isEqualTo("value");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readUtf8()).isEqualTo("{\"theName\":\"value\"}");
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }
}
