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

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit.HttpLoggingInterceptor.Level;

import static org.assertj.core.api.Assertions.assertThat;

public final class HttpLoggingInterceptorTest {
  private static final MediaType PLAIN = MediaType.parse("text/plain; charset=utf-8");

  @Rule public final MockWebServer server = new MockWebServer();

  private final OkHttpClient client = new OkHttpClient();
  private final List<String> logs = new ArrayList<>();
  private HttpLoggingInterceptor interceptor;

  @Before public void setUp() {
    HttpLoggingInterceptor.Logger logger = new HttpLoggingInterceptor.Logger() {
      @Override public void log(String message) {
        logs.add(message);
      }
    };
    interceptor = new HttpLoggingInterceptor(logger);
    client.networkInterceptors().add(interceptor);
    client.setConnectionPool(null);
  }

  @Test public void none() throws IOException {
    server.enqueue(new MockResponse());
    client.newCall(request().build()).execute();
    assertThat(logs).isEmpty();
  }

  @Test public void basicGet() throws IOException {
    interceptor.setLevel(Level.BASIC);

    server.enqueue(new MockResponse());
    client.newCall(request().build()).execute();

    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("--> GET / HTTP/1.1");
    assertThat(logs.get(1)).matches("<-- HTTP/1\\.1 200 OK \\(\\d+ms, 0-byte body\\)");
  }

  @Test public void basicPost() throws IOException {
    interceptor.setLevel(Level.BASIC);

    server.enqueue(new MockResponse());
    client.newCall(request().post(RequestBody.create(PLAIN, "Hi?")).build()).execute();

    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("--> POST / HTTP/1.1 (3-byte body)");
    assertThat(logs.get(1)).matches("<-- HTTP/1\\.1 200 OK \\(\\d+ms, 0-byte body\\)");
  }

  @Test public void basicResponseBody() throws IOException {
    interceptor.setLevel(Level.BASIC);

    server.enqueue(new MockResponse()
        .setBody("Hello!")
        .setHeader("Content-Type", PLAIN.toString()));
    client.newCall(request().build()).execute();

    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("--> GET / HTTP/1.1");
    assertThat(logs.get(1)).matches("<-- HTTP/1\\.1 200 OK \\(\\d+ms, 6-byte body\\)");
  }

  @Test public void headersGet() throws IOException {
    interceptor.setLevel(Level.HEADERS);

    server.enqueue(new MockResponse());
    client.newCall(request().build()).execute();

    assertThat(logs).hasSize(12);
    assertThat(logs.get(0)).isEqualTo("--> GET / HTTP/1.1");
    assertThat(logs.get(1)).isEqualTo("Host: " + server.getHostName() + ":" + server.getPort());
    assertThat(logs.get(2)).isEqualTo("Connection: Keep-Alive");
    assertThat(logs.get(3)).isEqualTo("Accept-Encoding: gzip");
    assertThat(logs.get(4)).isEqualTo("User-Agent: okhttp/2.5.0");
    assertThat(logs.get(5)).isEqualTo("--> END GET");
    assertThat(logs.get(6)).matches("<-- HTTP/1\\.1 200 OK \\(\\d+ms\\)");
    assertThat(logs.get(7)).isEqualTo("Content-Length: 0");
    assertThat(logs.get(8)).isEqualTo("OkHttp-Selected-Protocol: http/1.1");
    assertThat(logs.get(9)).matches("OkHttp-Sent-Millis: \\d+");
    assertThat(logs.get(10)).matches("OkHttp-Received-Millis: \\d+");
    assertThat(logs.get(11)).isEqualTo("<-- END HTTP");
  }

  @Test public void headersPost() throws IOException {
    interceptor.setLevel(Level.HEADERS);

    server.enqueue(new MockResponse());
    client.newCall(request().post(RequestBody.create(PLAIN, "Hi?")).build()).execute();

    assertThat(logs).hasSize(14);
    assertThat(logs.get(0)).isEqualTo("--> POST / HTTP/1.1");
    assertThat(logs.get(1)).isEqualTo("Content-Type: text/plain; charset=utf-8");
    assertThat(logs.get(2)).isEqualTo("Content-Length: 3");
    assertThat(logs.get(3)).isEqualTo("Host: " + server.getHostName() + ":" + server.getPort());
    assertThat(logs.get(4)).isEqualTo("Connection: Keep-Alive");
    assertThat(logs.get(5)).isEqualTo("Accept-Encoding: gzip");
    assertThat(logs.get(6)).isEqualTo("User-Agent: okhttp/2.5.0");
    assertThat(logs.get(7)).isEqualTo("--> END POST");
    assertThat(logs.get(8)).matches("<-- HTTP/1\\.1 200 OK \\(\\d+ms\\)");
    assertThat(logs.get(9)).isEqualTo("Content-Length: 0");
    assertThat(logs.get(10)).isEqualTo("OkHttp-Selected-Protocol: http/1.1");
    assertThat(logs.get(11)).matches("OkHttp-Sent-Millis: \\d+");
    assertThat(logs.get(12)).matches("OkHttp-Received-Millis: \\d+");
    assertThat(logs.get(13)).isEqualTo("<-- END HTTP");
  }

  @Test public void headersResponseBody() throws IOException {
    interceptor.setLevel(Level.HEADERS);

    server.enqueue(new MockResponse()
        .setBody("Hello!")
        .setHeader("Content-Type", PLAIN.toString()));
    client.newCall(request().build()).execute();

    assertThat(logs).hasSize(13);
    assertThat(logs.get(0)).isEqualTo("--> GET / HTTP/1.1");
    assertThat(logs.get(1)).isEqualTo("Host: " + server.getHostName() + ":" + server.getPort());
    assertThat(logs.get(2)).isEqualTo("Connection: Keep-Alive");
    assertThat(logs.get(3)).isEqualTo("Accept-Encoding: gzip");
    assertThat(logs.get(4)).isEqualTo("User-Agent: okhttp/2.5.0");
    assertThat(logs.get(5)).isEqualTo("--> END GET");
    assertThat(logs.get(6)).matches("<-- HTTP/1\\.1 200 OK \\(\\d+ms\\)");
    assertThat(logs.get(7)).isEqualTo("Content-Length: 6");
    assertThat(logs.get(8)).isEqualTo("Content-Type: text/plain; charset=utf-8");
    assertThat(logs.get(9)).isEqualTo("OkHttp-Selected-Protocol: http/1.1");
    assertThat(logs.get(10)).matches("OkHttp-Sent-Millis: \\d+");
    assertThat(logs.get(11)).matches("OkHttp-Received-Millis: \\d+");
    assertThat(logs.get(12)).isEqualTo("<-- END HTTP");
  }

  @Test public void bodyGet() throws IOException {
    interceptor.setLevel(Level.BODY);

    server.enqueue(new MockResponse());
    client.newCall(request().build()).execute();

    assertThat(logs).hasSize(12);
    assertThat(logs.get(0)).isEqualTo("--> GET / HTTP/1.1");
    assertThat(logs.get(1)).isEqualTo("Host: " + server.getHostName() + ":" + server.getPort());
    assertThat(logs.get(2)).isEqualTo("Connection: Keep-Alive");
    assertThat(logs.get(3)).isEqualTo("Accept-Encoding: gzip");
    assertThat(logs.get(4)).isEqualTo("User-Agent: okhttp/2.5.0");
    assertThat(logs.get(5)).isEqualTo("--> END GET");
    assertThat(logs.get(6)).matches("<-- HTTP/1\\.1 200 OK \\(\\d+ms\\)");
    assertThat(logs.get(7)).isEqualTo("Content-Length: 0");
    assertThat(logs.get(8)).isEqualTo("OkHttp-Selected-Protocol: http/1.1");
    assertThat(logs.get(9)).matches("OkHttp-Sent-Millis: \\d+");
    assertThat(logs.get(10)).matches("OkHttp-Received-Millis: \\d+");
    assertThat(logs.get(11)).isEqualTo("<-- END HTTP (0-byte body)");
  }

  @Test public void bodyPost() throws IOException {
    interceptor.setLevel(Level.BODY);

    server.enqueue(new MockResponse());
    client.newCall(request().post(RequestBody.create(PLAIN, "Hi?")).build()).execute();

    assertThat(logs).hasSize(16);
    assertThat(logs.get(0)).isEqualTo("--> POST / HTTP/1.1");
    assertThat(logs.get(1)).isEqualTo("Content-Type: text/plain; charset=utf-8");
    assertThat(logs.get(2)).isEqualTo("Content-Length: 3");
    assertThat(logs.get(3)).isEqualTo("Host: " + server.getHostName() + ":" + server.getPort());
    assertThat(logs.get(4)).isEqualTo("Connection: Keep-Alive");
    assertThat(logs.get(5)).isEqualTo("Accept-Encoding: gzip");
    assertThat(logs.get(6)).isEqualTo("User-Agent: okhttp/2.5.0");
    assertThat(logs.get(7)).isEqualTo("");
    assertThat(logs.get(8)).isEqualTo("Hi?");
    assertThat(logs.get(9)).isEqualTo("--> END POST (3-byte body)");
    assertThat(logs.get(10)).matches("<-- HTTP/1\\.1 200 OK \\(\\d+ms\\)");
    assertThat(logs.get(11)).isEqualTo("Content-Length: 0");
    assertThat(logs.get(12)).isEqualTo("OkHttp-Selected-Protocol: http/1.1");
    assertThat(logs.get(13)).matches("OkHttp-Sent-Millis: \\d+");
    assertThat(logs.get(14)).matches("OkHttp-Received-Millis: \\d+");
    assertThat(logs.get(15)).isEqualTo("<-- END HTTP (0-byte body)");
  }

  @Test public void bodyResponseBody() throws IOException {
    interceptor.setLevel(Level.BODY);

    server.enqueue(new MockResponse()
        .setBody("Hello!")
        .setHeader("Content-Type", PLAIN.toString()));
    client.newCall(request().build()).execute();

    assertThat(logs).hasSize(15);
    assertThat(logs.get(0)).isEqualTo("--> GET / HTTP/1.1");
    assertThat(logs.get(1)).isEqualTo("Host: " + server.getHostName() + ":" + server.getPort());
    assertThat(logs.get(2)).isEqualTo("Connection: Keep-Alive");
    assertThat(logs.get(3)).isEqualTo("Accept-Encoding: gzip");
    assertThat(logs.get(4)).isEqualTo("User-Agent: okhttp/2.5.0");
    assertThat(logs.get(5)).isEqualTo("--> END GET");
    assertThat(logs.get(6)).matches("<-- HTTP/1\\.1 200 OK \\(\\d+ms\\)");
    assertThat(logs.get(7)).isEqualTo("Content-Length: 6");
    assertThat(logs.get(8)).isEqualTo("Content-Type: text/plain; charset=utf-8");
    assertThat(logs.get(9)).isEqualTo("OkHttp-Selected-Protocol: http/1.1");
    assertThat(logs.get(10)).matches("OkHttp-Sent-Millis: \\d+");
    assertThat(logs.get(11)).matches("OkHttp-Received-Millis: \\d+");
    assertThat(logs.get(12)).isEqualTo("");
    assertThat(logs.get(13)).isEqualTo("Hello!");
    assertThat(logs.get(14)).isEqualTo("<-- END HTTP (6-byte body)");
  }

  private Request.Builder request() {
    return new Request.Builder().url(server.url("/"));
  }
}
