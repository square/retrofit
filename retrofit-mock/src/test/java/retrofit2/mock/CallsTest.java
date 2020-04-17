/*
 * Copyright (C) 2017 Square, Inc.
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
package retrofit2.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class CallsTest {
  @Test
  public void bodyExecute() throws IOException {
    Call<String> taco = Calls.response("Taco");
    assertEquals("Taco", taco.execute().body());
  }

  @Test
  public void bodyEnqueue() throws IOException {
    Call<String> taco = Calls.response("Taco");
    final AtomicReference<Response<String>> responseRef = new AtomicReference<>();
    taco.enqueue(
        new Callback<String>() {
          @Override
          public void onResponse(Call<String> call, Response<String> response) {
            responseRef.set(response);
          }

          @Override
          public void onFailure(Call<String> call, Throwable t) {
            fail();
          }
        });
    assertThat(responseRef.get().body()).isEqualTo("Taco");
  }

  @Test
  public void responseExecute() throws IOException {
    Response<String> response = Response.success("Taco");
    Call<String> taco = Calls.response(response);
    assertFalse(taco.isExecuted());
    assertSame(response, taco.execute());
    assertTrue(taco.isExecuted());
    try {
      taco.execute();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Already executed");
    }
  }

  @Test
  public void responseEnqueue() {
    Response<String> response = Response.success("Taco");
    Call<String> taco = Calls.response(response);
    assertFalse(taco.isExecuted());

    final AtomicReference<Response<String>> responseRef = new AtomicReference<>();
    taco.enqueue(
        new Callback<String>() {
          @Override
          public void onResponse(Call<String> call, Response<String> response) {
            responseRef.set(response);
          }

          @Override
          public void onFailure(Call<String> call, Throwable t) {
            fail();
          }
        });
    assertSame(response, responseRef.get());
    assertTrue(taco.isExecuted());

    try {
      taco.enqueue(
          new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
              fail();
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
              fail();
            }
          });
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Already executed");
    }
  }

  @Test
  public void enqueueNullThrows() {
    Call<String> taco = Calls.response("Taco");
    try {
      taco.enqueue(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("callback == null");
    }
  }

  @Test
  public void responseCancelExecute() {
    Call<String> taco = Calls.response(Response.success("Taco"));
    assertFalse(taco.isCanceled());
    taco.cancel();
    assertTrue(taco.isCanceled());

    try {
      taco.execute();
      fail();
    } catch (IOException e) {
      assertThat(e).hasMessage("canceled");
    }
  }

  @Test
  public void responseCancelEnqueue() throws IOException {
    Call<String> taco = Calls.response(Response.success("Taco"));
    assertFalse(taco.isCanceled());
    taco.cancel();
    assertTrue(taco.isCanceled());

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    taco.enqueue(
        new Callback<String>() {
          @Override
          public void onResponse(Call<String> call, Response<String> response) {
            fail();
          }

          @Override
          public void onFailure(Call<String> call, Throwable t) {
            failureRef.set(t);
          }
        });
    assertThat(failureRef.get()).isInstanceOf(IOException.class).hasMessage("canceled");
  }

  @Test
  public void failureExecute() {
    IOException failure = new IOException("Hey");
    Call<Object> taco = Calls.failure(failure);
    assertFalse(taco.isExecuted());
    try {
      taco.execute();
      fail();
    } catch (IOException e) {
      assertSame(failure, e);
    }
    assertTrue(taco.isExecuted());
  }

  @Test
  public void failureExecuteCheckedException() {
    CertificateException failure = new CertificateException("Hey");
    Call<Object> taco = Calls.failure(failure);
    assertFalse(taco.isExecuted());
    try {
      taco.execute();
      fail();
    } catch (Exception e) {
      assertSame(failure, e);
    }
    assertTrue(taco.isExecuted());
  }

  @Test
  public void failureEnqueue() {
    IOException failure = new IOException("Hey");
    Call<Object> taco = Calls.failure(failure);
    assertFalse(taco.isExecuted());

    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    taco.enqueue(
        new Callback<Object>() {
          @Override
          public void onResponse(Call<Object> call, Response<Object> response) {
            fail();
          }

          @Override
          public void onFailure(Call<Object> call, Throwable t) {
            failureRef.set(t);
          }
        });
    assertSame(failure, failureRef.get());
    assertTrue(taco.isExecuted());
  }

  @Test
  public void cloneHasOwnState() throws IOException {
    Call<String> taco = Calls.response("Taco");
    assertEquals("Taco", taco.execute().body());
    Call<String> anotherTaco = taco.clone();
    assertFalse(anotherTaco.isExecuted());
    assertEquals("Taco", anotherTaco.execute().body());
    assertTrue(anotherTaco.isExecuted());
  }

  @Test
  public void deferredReturnExecute() throws IOException {
    Call<Integer> counts =
        Calls.defer(
            new Callable<Call<Integer>>() {
              private int count = 0;

              @Override
              public Call<Integer> call() throws Exception {
                return Calls.response(++count);
              }
            });
    Call<Integer> a = counts.clone();
    Call<Integer> b = counts.clone();

    assertEquals(1, b.execute().body().intValue());
    assertEquals(2, a.execute().body().intValue());
  }

  @Test
  public void deferredReturnEnqueue() {
    Call<Integer> counts =
        Calls.defer(
            new Callable<Call<Integer>>() {
              private int count = 0;

              @Override
              public Call<Integer> call() throws Exception {
                return Calls.response(++count);
              }
            });
    Call<Integer> a = counts.clone();
    Call<Integer> b = counts.clone();

    final AtomicReference<Response<Integer>> responseRef = new AtomicReference<>();
    Callback<Integer> callback =
        new Callback<Integer>() {
          @Override
          public void onResponse(Call<Integer> call, Response<Integer> response) {
            responseRef.set(response);
          }

          @Override
          public void onFailure(Call<Integer> call, Throwable t) {
            fail();
          }
        };
    b.enqueue(callback);
    assertEquals(1, responseRef.get().body().intValue());

    a.enqueue(callback);
    assertEquals(2, responseRef.get().body().intValue());
  }

  @Test
  public void deferredThrowExecute() throws IOException {
    final IOException failure = new IOException("Hey");
    Call<Object> failing =
        Calls.defer(
            () -> {
              throw failure;
            });
    try {
      failing.execute();
      fail();
    } catch (IOException e) {
      assertSame(failure, e);
    }
  }

  @Test
  public void deferredThrowEnqueue() {
    final IOException failure = new IOException("Hey");
    Call<Object> failing =
        Calls.defer(
            () -> {
              throw failure;
            });
    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    failing.enqueue(
        new Callback<Object>() {
          @Override
          public void onResponse(Call<Object> call, Response<Object> response) {
            fail();
          }

          @Override
          public void onFailure(Call<Object> call, Throwable t) {
            failureRef.set(t);
          }
        });
    assertSame(failure, failureRef.get());
  }

  @Test
  public void deferredThrowUncheckedExceptionEnqueue() {
    final RuntimeException failure = new RuntimeException("Hey");
    final AtomicReference<Throwable> failureRef = new AtomicReference<>();
    Calls.failure(failure)
        .enqueue(
            new Callback<Object>() {
              @Override
              public void onResponse(Call<Object> call, Response<Object> response) {
                fail();
              }

              @Override
              public void onFailure(Call<Object> call, Throwable t) {
                failureRef.set(t);
              }
            });
    assertSame(failure, failureRef.get());
  }
}
