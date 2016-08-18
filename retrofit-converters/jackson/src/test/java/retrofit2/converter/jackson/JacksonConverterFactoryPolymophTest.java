/*
 * Copyright (C) 2013 Square, Inc.
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
package retrofit2.converter.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JacksonConverterFactoryPolymophTest {

  static class AnImplementation {

    private String theName;

    AnImplementation() {
    }

    AnImplementation(String name) {
      theName = name;
    }

    public String getName() {
      return theName;
    }
  }

  static class AChild extends AnImplementation {
    private int age;


    public AChild() {
    }

    public AChild(String name, int age) {
      super(name);
      this.age = age;
    }
  }


  interface Service {
    @POST("/")
    Call<AnImplementation> anImplementation(@Body AnImplementation impl);

    @POST("/")
    Call<AChild> anChildOfImplementation(@Body AnImplementation impl);
  }

  @Rule
  public final MockWebServer server = new MockWebServer();

  private Service service;

  @Before
  public void setUp() {
    ObjectMapper mapper = new ObjectMapper();

    mapper.configure(MapperFeature.AUTO_DETECT_GETTERS, false);
    mapper.configure(MapperFeature.AUTO_DETECT_SETTERS, false);
    mapper.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false);
    mapper.setVisibility(mapper.getSerializationConfig()
        .getDefaultVisibilityChecker()
        .withFieldVisibility(JsonAutoDetect.Visibility.ANY));


    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(JacksonConverterFactory.create(mapper))
        .build();
    service = retrofit.create(Service.class);
  }


  @Test public void anImplementation() throws IOException, InterruptedException {
    server.enqueue(new MockResponse().setBody("{\"theName\":\"value\"}"));

    Call<AnImplementation> call = service.anImplementation(new AnImplementation("value"));
    Response<AnImplementation> response = call.execute();
    AnImplementation body = response.body();
    assertThat(body.theName).isEqualTo("value");

    RecordedRequest request = server.takeRequest();
    // TODO figure out how to get Jackson to stop using AnInterface's serializer here.
    assertThat(request.getBody().readUtf8()).isEqualTo("{\"theName\":\"value\"}");
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

  @Test public void anChildOfImplementation() throws IOException, InterruptedException {
    server.enqueue(new MockResponse().setBody("{\"theName\":\"value\", \"age\":10}}"));

    Call<AChild> call = service.anChildOfImplementation(new AChild("value", 10));
    Response<AChild> response = call.execute();
    AChild body = response.body();
    assertThat(body.getName()).isEqualTo("value");
    assertThat(body.age).isEqualTo(10);

    RecordedRequest request = server.takeRequest();
    // TODO figure out how to get Jackson to stop using AnInterface's serializer here.
    assertThat(request.getBody().readUtf8()).isEqualTo("{\"theName\":\"value\",\"age\":10}");
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

}
