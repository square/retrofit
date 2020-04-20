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
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.http.GET;

public final class ScalarsConverterPrimitivesFactoryTest {
  interface Service {
    @GET("/")
    boolean booleanPrimitive();

    @GET("/")
    byte bytePrimitive();

    @GET("/")
    char charPrimitive();

    @GET("/")
    double doublePrimitive();

    @GET("/")
    float floatPrimitive();

    @GET("/")
    int integerPrimitive();

    @GET("/")
    long longPrimitive();

    @GET("/")
    short shortPrimitive();
  }

  static class DirectCallIOException extends RuntimeException {
    DirectCallIOException(String message, IOException e) {
      super(message, e);
    }
  }

  static class DirectCallAdapterFactory extends CallAdapter.Factory {
    @Override
    public CallAdapter<?, ?> get(
        final Type returnType, Annotation[] annotations, Retrofit retrofit) {
      return new CallAdapter<Object, Object>() {
        @Override
        public Type responseType() {
          return returnType;
        }

        @Override
        public Object adapt(Call call) {
          try {
            return call.execute().body();
          } catch (IOException e) {
            throw new DirectCallIOException(e.getMessage(), e);
          }
        }
      };
    }
  }

  @Rule public final MockWebServer server = new MockWebServer();

  private Service service;

  @Before
  public void setUp() {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(ScalarsConverterFactory.create())
            .addCallAdapterFactory(new DirectCallAdapterFactory())
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void supportedResponseTypes() throws IOException, InterruptedException {
    server.enqueue(new MockResponse().setBody("true"));
    boolean booleanResponse = service.booleanPrimitive();
    assertThat(booleanResponse).isTrue();

    server.enqueue(new MockResponse().setBody("5"));
    byte byteResponse = service.bytePrimitive();
    assertThat(byteResponse).isEqualTo((byte) 5);

    server.enqueue(new MockResponse().setBody("b"));
    char characterResponse = service.charPrimitive();
    assertThat(characterResponse).isEqualTo('b');

    server.enqueue(new MockResponse().setBody(""));
    try {
      service.charPrimitive();
      fail();
    } catch (DirectCallIOException e) {
      assertThat(e).hasMessage("Expected body of length 1 for Character conversion but was 0");
    }

    server.enqueue(new MockResponse().setBody("bb"));
    try {
      service.charPrimitive();
      fail();
    } catch (DirectCallIOException e) {
      assertThat(e).hasMessage("Expected body of length 1 for Character conversion but was 2");
    }

    server.enqueue(new MockResponse().setBody("13.13"));
    double doubleResponse = service.doublePrimitive();
    assertThat(doubleResponse).isEqualTo(13.13);

    server.enqueue(new MockResponse().setBody("13.13"));
    float floatResponse = service.floatPrimitive();
    assertThat(floatResponse).isEqualTo(13.13f);

    server.enqueue(new MockResponse().setBody("13"));
    int integerResponse = service.integerPrimitive();
    assertThat(integerResponse).isEqualTo(13);

    server.enqueue(new MockResponse().setBody("1347"));
    long longResponse = service.longPrimitive();
    assertThat(longResponse).isEqualTo(1347L);

    server.enqueue(new MockResponse().setBody("134"));
    short shortResponse = service.shortPrimitive();
    assertThat(shortResponse).isEqualTo((short) 134);
  }
}
