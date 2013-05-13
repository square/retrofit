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
package retrofit.android;

import android.net.http.AndroidHttpClient;
import retrofit.client.ApacheClient;

/**
 * Provides a {@link retrofit.client.Client} which uses the Android-specific version of
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
