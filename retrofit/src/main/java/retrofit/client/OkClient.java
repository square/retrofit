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
package retrofit.client;

import com.squareup.okhttp.OkHttpClient;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/** Retrofit client that uses OkHttp for communication. */
public class OkClient extends UrlConnectionClient {
  private final OkHttpClient client;

  public OkClient() {
    this(new OkHttpClient());
  }

  public OkClient(OkHttpClient client) {
    this.client = client;
  }

  @Override protected HttpURLConnection openConnection(Request request) throws IOException {
    HttpURLConnection connection = client.open(new URL(request.getUrl()));
    connection.setConnectTimeout(Defaults.CONNECT_TIMEOUT);
    connection.setReadTimeout(Defaults.READ_TIMEOUT);
    return connection;
  }
}
