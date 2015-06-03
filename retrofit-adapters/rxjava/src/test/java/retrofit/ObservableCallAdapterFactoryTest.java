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

import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import org.junit.Test;
import rx.Observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class ObservableCallAdapterFactoryTest {
  private final ObservableCallAdapterFactory factory = ObservableCallAdapterFactory.create();

  @Test public void bodySuccess200() {
    Type type = new TypeToken<Observable<String>>() {}.getType();
    CallAdapter adapter = factory.get(type);
    Observable<String> observable = (Observable<String>) adapter.adapt(new EmptyCall() {
      @Override public void enqueue(Callback<Object> callback) {
        callback.success(Response.fromBody("Hi"));
      }
    });
    String body = observable.toBlocking().first();
    assertThat(body).isEqualTo("Hi");
  }

  @Test public void bodySuccess404() {
    Type type = new TypeToken<Observable<String>>() {}.getType();
    CallAdapter adapter = factory.get(type);
    Observable<String> observable = (Observable<String>) adapter.adapt(new EmptyCall() {
      @Override public void enqueue(Callback<Object> callback) {
        callback.success(
            Response.fromError(404, ResponseBody.create(MediaType.parse("application/json"), "Hi")));
      }
    });
    try {
      observable.toBlocking().first();
      fail();
    } catch (RuntimeException e) {
      // TODO assert on some indicator of 404.
    }
  }

  @Test public void bodyFailure() {
    Type type = new TypeToken<Observable<String>>() {}.getType();
    CallAdapter adapter = factory.get(type);
    final Throwable throwable = new IOException();
    Observable<String> observable = (Observable<String>) adapter.adapt(new EmptyCall() {
      @Override public void enqueue(Callback<Object> callback) {
        callback.failure(throwable);
      }
    });
    try {
      observable.toBlocking().first();
      fail();
    } catch (RuntimeException e) {
      assertThat(e.getCause()).isSameAs(throwable);
    }
  }

  @Test public void responseSuccess() {
    Type type = new TypeToken<Observable<Response<String>>>() {}.getType();
    CallAdapter adapter = factory.get(type);
    final Response<Object> response = Response.fromBody("Hi");
    Observable<Response<String>> observable = (Observable<Response<String>>) adapter.adapt(new EmptyCall() {
      @Override public void enqueue(Callback<Object> callback) {
        callback.success(response);
      }
    });
    Response<String> body = observable.toBlocking().first();
    assertThat(body).isSameAs(response);
  }

  @Test public void responseFailure() {
    Type type = new TypeToken<Observable<Response<String>>>() {}.getType();
    CallAdapter adapter = factory.get(type);
    final Throwable throwable = new IOException();
    Observable<Response<String>> observable = (Observable<Response<String>>) adapter.adapt(new EmptyCall() {
      @Override public void enqueue(Callback<Object> callback) {
        callback.failure(throwable);
      }
    });
    try {
      observable.toBlocking().first();
      fail();
    } catch (RuntimeException t) {
      assertThat(t.getCause()).isSameAs(throwable);
    }
  }

  @Test public void resultSuccess() {
    Type type = new TypeToken<Observable<Result<String>>>() {}.getType();
    CallAdapter adapter = factory.get(type);
    final Response<Object> response = Response.fromBody("Hi");
    Observable<Result<String>> observable = (Observable<Result<String>>) adapter.adapt(new EmptyCall() {
      @Override public void enqueue(Callback<Object> callback) {
        callback.success(response);
      }
    });
    Result<String> result = observable.toBlocking().first();
    assertThat(result.isError()).isFalse();
    assertThat(result.response()).isSameAs(response);
  }

  @Test public void resultFailure() {
    Type type = new TypeToken<Observable<Result<String>>>() {}.getType();
    CallAdapter adapter = factory.get(type);
    final Throwable throwable = new IOException();
    Observable<Result<String>> observable = (Observable<Result<String>>) adapter.adapt(new EmptyCall() {
      @Override public void enqueue(Callback<Object> callback) {
        callback.failure(throwable);
      }
    });
    Result<String> result = observable.toBlocking().first();
    assertThat(result.isError()).isTrue();
    assertThat(result.error()).isSameAs(throwable);
  }

  @Test public void responseType() {
    Type classType = new TypeToken<Observable<String>>() {}.getType();
    assertThat(factory.get(classType).responseType()).isEqualTo(String.class);
    Type wilcardType = new TypeToken<Observable<? extends String>>() {}.getType();
    assertThat(factory.get(wilcardType).responseType()).isEqualTo(String.class);
    Type genericType = new TypeToken<Observable<List<String>>>() {}.getType();
    assertThat(factory.get(genericType).responseType()) //
        .isEqualTo(new TypeToken<List<String>>() {}.getType());
    Type responseType = new TypeToken<Observable<Response<String>>>() {}.getType();
    assertThat(factory.get(responseType).responseType()).isEqualTo(String.class);
    Type resultType = new TypeToken<Observable<Response<String>>>() {}.getType();
    assertThat(factory.get(resultType).responseType()).isEqualTo(String.class);
  }

  @Test public void rawTypeThrows() {
    Type type = new TypeToken<Observable>() {}.getType();
    try {
      factory.get(type);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Observable return type must be parameterized as Observable<Foo> or Observable<? extends Foo>");
    }
  }

  @Test public void rawResponseTypeThrows() {
    Type type = new TypeToken<Observable<Response>>() {}.getType();
    try {
      factory.get(type);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Response must be parameterized as Response<Foo> or Response<? extends Foo>");
    }
  }

  @Test public void rawResultTypeThrows() {
    Type type = new TypeToken<Observable<Result>>() {}.getType();
    try {
      factory.get(type);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Result must be parameterized as Result<Foo> or Result<? extends Foo>");
    }
  }

  static abstract class EmptyCall implements Call<Object> {
    @Override public Response<Object> execute() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override public void cancel() {
    }

    @Override public Call<Object> clone() {
      try {
        return (Call<Object>) super.clone();
      } catch (CloneNotSupportedException e) {
        throw new AssertionError(e);
      }
    }
  }
}
