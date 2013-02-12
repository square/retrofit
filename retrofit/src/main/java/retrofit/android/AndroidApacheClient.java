// Copyright 2013 Square, Inc.
package retrofit.android;

import android.net.http.AndroidHttpClient;
import retrofit.http.client.ApacheClient;

/**
 * Provides a {@link retrofit.http.client.Client} which uses the Android-specific version of
 * {@link org.apache.http.client.HttpClient}, {@link AndroidHttpClient}.
 * <p>
 * If you need to provide a customized version of the {@link AndroidHttpClient} or a different
 * {@link org.apache.http.client.HttpClient} on Android use {@link ApacheClient} directly.
 */
public final class AndroidApacheClient extends ApacheClient {
  public AndroidApacheClient() {
    super(AndroidHttpClient.newInstance("Retrofit"));
  }
}
