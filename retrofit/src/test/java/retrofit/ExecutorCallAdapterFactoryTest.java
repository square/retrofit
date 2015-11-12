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

import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SuppressWarnings("unchecked")
public final class ExecutorCallAdapterFactoryTest {
  private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

  private final Retrofit retrofit = new Retrofit.Builder()
      .baseUrl("http://localhost:1")
      .build();
  private final Callback<String> callback = mock(Callback.class);
  private final Executor callbackExecutor = spy(new Executor() {
    @Override public void execute(Runnable runnable) {
      runnable.run();
    }
  });
  private final CallAdapter.Factory factory = new ExecutorCallAdapterFactory(callbackExecutor);

  @Test public void rawTypeThrows() {
    try {
      factory.get(Call.class, NO_ANNOTATIONS, retrofit);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Call return type must be parameterized as Call<Foo> or Call<? extends Foo>");
    }
  }

  @Test public void responseThrows() {
    Type returnType = new TypeToken<Call<Response<String>>>() {}.getType();
    try {
      factory.get(returnType, NO_ANNOTATIONS, retrofit);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Call<T> cannot use Response as its generic parameter. "
          + "Specify the response body type only (e.g., Call<TweetResponse>).");
    }
  }

  @Test public void responseType() {
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

  @Test public void adaptedCallExecute() throws IOException {
    Type returnType = new TypeToken<Call<String>>() {}.getType();
    CallAdapter<Call<?>> adapter =
        (CallAdapter<Call<?>>) factory.get(returnType, NO_ANNOTATIONS, retrofit);
    final Response<String> response = Response.success("Hi");
    Call<String> call = (Call<String>) adapter.adapt(new EmptyCall() {
      @Override public Response<String> execute() throws IOException {
        return response;
      }
    });
    assertThat(call.execute()).isSameAs(response);
  }

  @Test public void adaptedCallEnqueueUsesExecutorForSuccessCallback() {
    Type returnType = new TypeToken<Call<String>>() {}.getType();
    CallAdapter<Call<?>> adapter =
        (CallAdapter<Call<?>>) factory.get(returnType, NO_ANNOTATIONS, retrofit);
    final Response<String> response = Response.success("Hi");
    Call<String> call = (Call<String>) adapter.adapt(new EmptyCall() {
      @Override public void enqueue(Callback<String> callback) {
        callback.onResponse(response);
      }
    });
    call.enqueue(callback);
    verify(callbackExecutor).execute(any(Runnable.class));
    verify(callback).onResponse(response);
  }

  @Test public void adaptedCallEnqueueUsesExecutorForFailureCallback() {
    Type returnType = new TypeToken<Call<String>>() {}.getType();
    CallAdapter<Call<?>> adapter =
        (CallAdapter<Call<?>>) factory.get(returnType, NO_ANNOTATIONS, retrofit);
    final Throwable throwable = new IOException();
    Call<String> call = (Call<String>) adapter.adapt(new EmptyCall() {
      @Override public void enqueue(Callback<String> callback) {
        callback.onFailure(throwable);
      }
    });
    call.enqueue(callback);
    verify(callbackExecutor).execute(any(Runnable.class));
    verifyNoMoreInteractions(callbackExecutor);
    verify(callback).onFailure(throwable);
    verifyNoMoreInteractions(callback);
  }

  @Test public void adaptedCallCloneDeepCopy() {
    Type returnType = new TypeToken<Call<String>>() {}.getType();
    CallAdapter<Call<?>> adapter =
        (CallAdapter<Call<?>>) factory.get(returnType, NO_ANNOTATIONS, retrofit);
    Call<String> delegate = mock(Call.class);
    Call<String> call = (Call<String>) adapter.adapt(delegate);
    Call<String> cloned = call.clone();
    assertThat(cloned).isNotSameAs(call);
    verify(delegate).clone();
    verifyNoMoreInteractions(delegate);
  }

  @Test public void adaptedCallCancel() {
    Type returnType = new TypeToken<Call<String>>() {}.getType();
    CallAdapter<Call<?>> adapter =
        (CallAdapter<Call<?>>) factory.get(returnType, NO_ANNOTATIONS, retrofit);
    Call<String> delegate = mock(Call.class);
    Call<String> call = (Call<String>) adapter.adapt(delegate);
    call.cancel();
    verify(delegate).cancel();
    verifyNoMoreInteractions(delegate);
  }

  static class EmptyCall implements Call<String> {
    @Override public void enqueue(Callback<String> callback) {
      throw new UnsupportedOperationException();
    }

    @Override public Response<String> execute() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override public void cancel() {
      throw new UnsupportedOperationException();
    }

    @Override public Call<String> clone() {
      throw new UnsupportedOperationException();
    }
  }
}
