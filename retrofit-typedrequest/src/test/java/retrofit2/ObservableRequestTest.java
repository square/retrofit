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
package retrofit2;

import com.google.common.reflect.TypeToken;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Method;
import retrofit2.ObservableRequest;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.Observable;
import rx.observables.BlockingObservable;

import static org.assertj.core.api.Assertions.assertThat;

public class ObservableRequestTest {

  @Rule public final MockWebServer server = new MockWebServer();
  private Retrofit retrofit;

  @Before public void setUp() {
    retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/").toString())
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build();
  }

  @Test public void bodySuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    ObservableRequest request = new ObservableRequest.Builder(retrofit)
        .path("/")
        .responseType(String.class)
        .method(Method.GET)
        .build();

    Observable<String> observable = request.newCall();
    BlockingObservable<String> o = observable.toBlocking();
    assertThat(o.first()).isEqualTo("Hi");
  }

  @Test public void responseSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    ObservableRequest request =
        new ObservableRequest.Builder(retrofit)
            .path("/")
            .responseType(new TypeToken<Response<String>>(getClass()) {}.getType())
            .method(Method.GET)
            .build();

    Observable<Response<String>> observable = request.newCall();
    BlockingObservable<Response<String>> o = observable.toBlocking();
    Response<String> response = o.first();
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }
}
