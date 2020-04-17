/*
 * Copyright (C) 2018 Square, Inc.
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
package retrofit2.adapter.rxjava;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import rx.Observable;
import rx.Subscription;

public final class CancelDisposeTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/")
    Observable<String> go();
  }

  private final OkHttpClient client = new OkHttpClient();
  private Service service;

  @Before
  public void setUp() {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(new StringConverterFactory())
            .addCallAdapterFactory(RxJavaCallAdapterFactory.createAsync())
            .callFactory(client)
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void disposeCancelsCall() {
    Subscription subscription = service.go().subscribe();
    List<Call> calls = client.dispatcher().runningCalls();
    assertEquals(1, calls.size());
    subscription.unsubscribe();
    assertTrue(calls.get(0).isCanceled());
  }

  @Test
  public void cancelDoesNotDispose() {
    Subscription subscription = service.go().subscribe();
    List<Call> calls = client.dispatcher().runningCalls();
    assertEquals(1, calls.size());
    calls.get(0).cancel();
    assertFalse(subscription.isUnsubscribed());
  }
}
