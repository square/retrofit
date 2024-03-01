/*
 * Copyright (C) 2024 Square, Inc.
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

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.ByteString;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class JacksonCborConverterFactoryTest {
  static class IntWrapper {
    public int value;

    public IntWrapper(int v) {
      value = v;
    }

    protected IntWrapper() {}
  }

  interface Service {
    @POST("/")
    Call<IntWrapper> post(@Body IntWrapper person);
  }

  @Rule public final MockWebServer server = new MockWebServer();

  private Service service;

  @Before
  public void setUp() {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(
                JacksonConverterFactory.create(new CBORMapper(), MediaType.get("application/cbor")))
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void post() throws IOException, InterruptedException {
    server.enqueue(
        new MockResponse()
            .setBody(new Buffer().write(ByteString.decodeHex("bf6576616c7565182aff"))));

    Call<IntWrapper> call = service.post(new IntWrapper(12));
    Response<IntWrapper> response = call.execute();
    assertThat(response.body().value).isEqualTo(42);

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readByteString())
        .isEqualTo(ByteString.decodeHex("bf6576616c75650cff"));
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/cbor");
  }
}
