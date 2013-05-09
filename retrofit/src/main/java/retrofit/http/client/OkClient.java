// Copyright 2013 Square, Inc.
package retrofit.http.client;

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
    return client.open(new URL(request.getUrl()));
  }
}
