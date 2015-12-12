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
package retrofit2;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class XStreamConverterFactoryTest {
  interface Service {
    @GET("/") Call<TestObject> get();
    @POST("/") Call<TestObject> post(@Body TestObject impl);
    @GET("/") Call<String> wrongClass();
  }

  @Rule public final MockWebServer server = new MockWebServer();

  private Service service;

  @Before public void setUp() {
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(XStreamConverterFactory.create())
            .build();
    service = retrofit.create(Service.class);
  }

  @Test public void bodyWays() throws IOException, InterruptedException {
    server.enqueue(new MockResponse().setBody(
            "<test-object><id>1337</id><message>hello world</message></test-object>"));

    Call<TestObject> call = service.post(new TestObject(1337, "hello world"));
    Response<TestObject> response = call.execute();
    TestObject body = response.body();
    assertThat(body.id).isEqualTo(1337);
    assertThat(body.message).isEqualTo("hello world");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readUtf8()).isEqualTo(
            "<test-object>\n  <id>1337</id>\n  <message>hello world</message>\n</test-object>");
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/xml; charset=UTF-8");
  }

  @Test public void deserializeWrongValue() throws IOException {
    server.enqueue(new MockResponse().setBody("<test-object><foo/><bar/></test-object>"));

    Call<?> call = service.get();
    try {
      call.execute();
      fail();
    } catch (RuntimeException e) {
      assertThat(e).isInstanceOf(AbstractReflectionConverter.UnknownFieldException.class)
              .hasMessageStartingWith("No such field retrofit2.TestObject.foo");
    }
  }

  @Test public void deserializeWrongClass() throws IOException {
    server.enqueue(new MockResponse().setBody(
            "<test-object><id>1337</id><message>hello world</message></test-object>"));

    Call<?> call = service.wrongClass();
    try {
      call.execute();
      fail();
    } catch (RuntimeException e) {
      assertThat(e).isInstanceOf(CannotResolveClassException.class)
              .hasMessage("test-object");
    }
  }
}
