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
package retrofit2.converter.scalars;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import okhttp3.ResponseBody;
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
import retrofit2.http.GET;
import retrofit2.http.POST;

public final class ScalarsConverterFactoryTest {
  interface Service {
    @POST("/")
    Call<ResponseBody> object(@Body Object body);

    @POST("/")
    Call<ResponseBody> stringObject(@Body String body);

    @POST("/")
    Call<ResponseBody> booleanPrimitive(@Body boolean body);

    @POST("/")
    Call<ResponseBody> booleanObject(@Body Boolean body);

    @POST("/")
    Call<ResponseBody> bytePrimitive(@Body byte body);

    @POST("/")
    Call<ResponseBody> byteObject(@Body Byte body);

    @POST("/")
    Call<ResponseBody> charPrimitive(@Body char body);

    @POST("/")
    Call<ResponseBody> charObject(@Body Character body);

    @POST("/")
    Call<ResponseBody> doublePrimitive(@Body double body);

    @POST("/")
    Call<ResponseBody> doubleObject(@Body Double body);

    @POST("/")
    Call<ResponseBody> floatPrimitive(@Body float body);

    @POST("/")
    Call<ResponseBody> floatObject(@Body Float body);

    @POST("/")
    Call<ResponseBody> integerPrimitive(@Body int body);

    @POST("/")
    Call<ResponseBody> integerObject(@Body Integer body);

    @POST("/")
    Call<ResponseBody> longPrimitive(@Body long body);

    @POST("/")
    Call<ResponseBody> longObject(@Body Long body);

    @POST("/")
    Call<ResponseBody> shortPrimitive(@Body short body);

    @POST("/")
    Call<ResponseBody> shortObject(@Body Short body);

    @GET("/")
    Call<Object> object();

    @GET("/")
    Call<String> stringObject();

    @GET("/")
    Call<Boolean> booleanObject();

    @GET("/")
    Call<Byte> byteObject();

    @GET("/")
    Call<Character> charObject();

    @GET("/")
    Call<Double> doubleObject();

    @GET("/")
    Call<Float> floatObject();

    @GET("/")
    Call<Integer> integerObject();

    @GET("/")
    Call<Long> longObject();

    @GET("/")
    Call<Short> shortObject();
  }

  @Rule public final MockWebServer server = new MockWebServer();

  private Service service;

  @Before
  public void setUp() {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(ScalarsConverterFactory.create())
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void unsupportedRequestTypesNotMatched() {
    try {
      service.object(null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              ""
                  + "Unable to create @Body converter for class java.lang.Object (parameter #1)\n"
                  + "    for method Service.object");
      assertThat(e.getCause())
          .hasMessage(
              ""
                  + "Could not locate RequestBody converter for class java.lang.Object.\n"
                  + "  Tried:\n"
                  + "   * retrofit2.BuiltInConverters\n"
                  + "   * retrofit2.converter.scalars.ScalarsConverterFactory\n"
                  + "   * retrofit2.OptionalConverterFactory");
    }
  }

  @Test
  public void supportedRequestTypes() throws IOException, InterruptedException {
    RecordedRequest request;

    server.enqueue(new MockResponse());
    service.stringObject("string").execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("6");
    assertThat(request.getBody().readUtf8()).isEqualTo("string");

    server.enqueue(new MockResponse());
    service.booleanPrimitive(true).execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("4");
    assertThat(request.getBody().readUtf8()).isEqualTo("true");

    server.enqueue(new MockResponse());
    service.booleanObject(false).execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("5");
    assertThat(request.getBody().readUtf8()).isEqualTo("false");

    server.enqueue(new MockResponse());
    service.bytePrimitive((byte) 0).execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("1");
    assertThat(request.getBody().readUtf8()).isEqualTo("0");

    server.enqueue(new MockResponse());
    service.byteObject((byte) 1).execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("1");
    assertThat(request.getBody().readUtf8()).isEqualTo("1");

    server.enqueue(new MockResponse());
    service.charPrimitive('a').execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("1");
    assertThat(request.getBody().readUtf8()).isEqualTo("a");

    server.enqueue(new MockResponse());
    service.charObject('b').execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("1");
    assertThat(request.getBody().readUtf8()).isEqualTo("b");

    server.enqueue(new MockResponse());
    service.doublePrimitive(2.2d).execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("3");
    assertThat(request.getBody().readUtf8()).isEqualTo("2.2");

    server.enqueue(new MockResponse());
    service.doubleObject(3.3d).execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("3");
    assertThat(request.getBody().readUtf8()).isEqualTo("3.3");

    server.enqueue(new MockResponse());
    service.floatPrimitive(4.4f).execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("3");
    assertThat(request.getBody().readUtf8()).isEqualTo("4.4");

    server.enqueue(new MockResponse());
    service.floatObject(5.5f).execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("3");
    assertThat(request.getBody().readUtf8()).isEqualTo("5.5");

    server.enqueue(new MockResponse());
    service.integerPrimitive(6).execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("1");
    assertThat(request.getBody().readUtf8()).isEqualTo("6");

    server.enqueue(new MockResponse());
    service.integerObject(7).execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("1");
    assertThat(request.getBody().readUtf8()).isEqualTo("7");

    server.enqueue(new MockResponse());
    service.longPrimitive(8L).execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("1");
    assertThat(request.getBody().readUtf8()).isEqualTo("8");

    server.enqueue(new MockResponse());
    service.longObject(9L).execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("1");
    assertThat(request.getBody().readUtf8()).isEqualTo("9");

    server.enqueue(new MockResponse());
    service.shortPrimitive((short) 10).execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("2");
    assertThat(request.getBody().readUtf8()).isEqualTo("10");

    server.enqueue(new MockResponse());
    service.shortObject((short) 11).execute();
    request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    assertThat(request.getHeader("Content-Length")).isEqualTo("2");
    assertThat(request.getBody().readUtf8()).isEqualTo("11");
  }

  @Test
  public void unsupportedResponseTypesNotMatched() {
    try {
      service.object();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              ""
                  + "Unable to create converter for class java.lang.Object\n"
                  + "    for method Service.object");
      assertThat(e.getCause())
          .hasMessage(
              ""
                  + "Could not locate ResponseBody converter for class java.lang.Object.\n"
                  + "  Tried:\n"
                  + "   * retrofit2.BuiltInConverters\n"
                  + "   * retrofit2.converter.scalars.ScalarsConverterFactory\n"
                  + "   * retrofit2.OptionalConverterFactory");
    }
  }

  @Test
  public void supportedResponseTypes() throws IOException, InterruptedException {
    server.enqueue(new MockResponse().setBody("test"));
    Response<String> stringResponse = service.stringObject().execute();
    assertThat(stringResponse.body()).isEqualTo("test");

    server.enqueue(new MockResponse().setBody("true"));
    Response<Boolean> booleanResponse = service.booleanObject().execute();
    assertThat(booleanResponse.body()).isTrue();

    server.enqueue(new MockResponse().setBody("5"));
    Response<Byte> byteResponse = service.byteObject().execute();
    assertThat(byteResponse.body()).isEqualTo((byte) 5);

    server.enqueue(new MockResponse().setBody("b"));
    Response<Character> characterResponse = service.charObject().execute();
    assertThat(characterResponse.body()).isEqualTo('b');

    server.enqueue(new MockResponse().setBody(""));
    try {
      service.charObject().execute();
      fail();
    } catch (IOException e) {
      assertThat(e).hasMessage("Expected body of length 1 for Character conversion but was 0");
    }

    server.enqueue(new MockResponse().setBody("bb"));
    try {
      service.charObject().execute();
      fail();
    } catch (IOException e) {
      assertThat(e).hasMessage("Expected body of length 1 for Character conversion but was 2");
    }

    server.enqueue(new MockResponse().setBody("13.13"));
    Response<Double> doubleResponse = service.doubleObject().execute();
    assertThat(doubleResponse.body()).isEqualTo(13.13);

    server.enqueue(new MockResponse().setBody("13.13"));
    Response<Float> floatResponse = service.floatObject().execute();
    assertThat(floatResponse.body()).isEqualTo(13.13f);

    server.enqueue(new MockResponse().setBody("13"));
    Response<Integer> integerResponse = service.integerObject().execute();
    assertThat(integerResponse.body()).isEqualTo(13);

    server.enqueue(new MockResponse().setBody("1347"));
    Response<Long> longResponse = service.longObject().execute();
    assertThat(longResponse.body()).isEqualTo(1347L);

    server.enqueue(new MockResponse().setBody("134"));
    Response<Short> shortResponse = service.shortObject().execute();
    assertThat(shortResponse.body()).isEqualTo((short) 134);
  }
}
