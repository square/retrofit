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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.Request;
import okio.Timeout;
import org.junit.Test;

@SuppressWarnings("unchecked")
public final class DefaultCallAdapterFactoryTest {
  private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

  private final Retrofit retrofit = new Retrofit.Builder().baseUrl("http://localhost:1").build();
  private final CallAdapter.Factory factory = new DefaultCallAdapterFactory(Runnable::run);

  @Test
  public void rawTypeThrows() {
    try {
      factory.get(Call.class, NO_ANNOTATIONS, retrofit);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage("Call return type must be parameterized as Call<Foo> or Call<? extends Foo>");
    }
  }

  @Test
  public void responseType() {
    Type classType = new TypeToken<Call<String>>() {}.getType();
    assertThat(factory.get(classType, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(String.class);
    Type wilcardType = new TypeToken<Call<? extends String>>() {}.getType();
    assertThat(factory.get(wilcardType, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(String.class);
    Type genericType = new TypeToken<Call<List<String>>>() {}.getType();
    assertThat(factory.get(genericType, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(new TypeToken<List<String>>() {}.getType());
  }

  @Test
  public void adaptedCallExecute() throws IOException {
    Type returnType = new TypeToken<Call<String>>() {}.getType();
    CallAdapter<String, Call<String>> adapter =
        (CallAdapter<String, Call<String>>) factory.get(returnType, NO_ANNOTATIONS, retrofit);
    final Response<String> response = Response.success("Hi");
    Call<String> call =
        adapter.adapt(
            new EmptyCall() {
              @Override
              public Response<String> execute() {
                return response;
              }
            });
    assertThat(call.execute()).isSameAs(response);
  }

  @Test
  public void adaptedCallCloneDeepCopy() {
    Type returnType = new TypeToken<Call<String>>() {}.getType();
    CallAdapter<String, Call<String>> adapter =
        (CallAdapter<String, Call<String>>) factory.get(returnType, NO_ANNOTATIONS, retrofit);
    final AtomicBoolean cloned = new AtomicBoolean();
    Call<String> delegate =
        new EmptyCall() {
          @Override
          public Call<String> clone() {
            cloned.set(true);
            return this;
          }
        };
    Call<String> call = adapter.adapt(delegate);
    assertThat(call.clone()).isNotSameAs(call);
    assertTrue(cloned.get());
  }

  @Test
  public void adaptedCallCancel() {
    Type returnType = new TypeToken<Call<String>>() {}.getType();
    CallAdapter<String, Call<String>> adapter =
        (CallAdapter<String, Call<String>>) factory.get(returnType, NO_ANNOTATIONS, retrofit);
    final AtomicBoolean canceled = new AtomicBoolean();
    Call<String> delegate =
        new EmptyCall() {
          @Override
          public void cancel() {
            canceled.set(true);
          }
        };
    Call<String> call = adapter.adapt(delegate);
    call.cancel();
    assertTrue(canceled.get());
  }

  static class EmptyCall implements Call<String> {
    @Override
    public void enqueue(Callback<String> callback) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isExecuted() {
      return false;
    }

    @Override
    public Response<String> execute() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void cancel() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCanceled() {
      return false;
    }

    @Override
    public Call<String> clone() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Request request() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Timeout timeout() {
      return Timeout.NONE;
    }
  }
}
