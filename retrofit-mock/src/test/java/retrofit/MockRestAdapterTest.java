// Copyright 2013 Square, Inc.
package retrofit;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import retrofit.client.Client;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.http.GET;
import rx.Observable;
import rx.util.functions.Action1;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static retrofit.MockRestAdapter.ValueChangeListener;
import static retrofit.Utils.SynchronousExecutor;

public class MockRestAdapterTest {
  interface SyncExample {
    @GET("/") Object doStuff();
  }

  interface AsyncExample {
    @GET("/") void doStuff(Callback<Object> cb);
  }

  interface ObservableExample {
    @GET("/") Observable<Object> doStuff();
  }

  private Executor httpExecutor;
  private Executor callbackExecutor;
  private MockRestAdapter mockRestAdapter;
  private ValueChangeListener valueChangeListener;

  @Before public void setUp() throws IOException {
    Client client = mock(Client.class);
    doThrow(new AssertionError()).when(client).execute(any(Request.class));

    httpExecutor = spy(new SynchronousExecutor());
    callbackExecutor = spy(new SynchronousExecutor());

    RestAdapter restAdapter = new RestAdapter.Builder() //
        .setClient(client)
        .setExecutors(httpExecutor, callbackExecutor)
        .setEndpoint("http://example.com")
        .setLogLevel(RestAdapter.LogLevel.NONE)
        .build();

    valueChangeListener = mock(ValueChangeListener.class);

    mockRestAdapter = MockRestAdapter.from(restAdapter);
    mockRestAdapter.setValueChangeListener(valueChangeListener);

    // Seed the random with a value so the tests are deterministic.
    mockRestAdapter.random.setSeed(2847);
  }

  @Test public void delayRestrictsRange() {
    try {
      mockRestAdapter.setDelay(-1);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Delay must be positive value.");
    }
    try {
      mockRestAdapter.setDelay(Long.MAX_VALUE);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageStartingWith("Delay value too large.");
    }
  }

  @Test public void varianceRestrictsRange() {
    try {
      mockRestAdapter.setVariancePercentage(-13);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Variance percentage must be between 0 and 100.");
    }
    try {
      mockRestAdapter.setVariancePercentage(174);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Variance percentage must be between 0 and 100.");
    }
  }

  @Test public void errorRestrictsRange() {
    try {
      mockRestAdapter.setErrorPercentage(-13);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Error percentage must be between 0 and 100.");
    }
    try {
      mockRestAdapter.setErrorPercentage(174);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Error percentage must be between 0 and 100.");
    }
  }

  @Test public void errorPercentageIsAccurate() {
    mockRestAdapter.setErrorPercentage(0);
    for (int i = 0; i < 10000; i++) {
      assertThat(mockRestAdapter.calculateIsFailure()).isFalse();
    }

    mockRestAdapter.setErrorPercentage(3);
    int failures = 0;
    for (int i = 0; i < 100000; i++) {
      if (mockRestAdapter.calculateIsFailure()) {
        failures += 1;
      }
    }
    assertThat(failures).isEqualTo(2964); // ~3% of 100k
  }

  @Test public void delayVarianceIsAccurate() {
    mockRestAdapter.setDelay(2000);

    mockRestAdapter.setVariancePercentage(0);
    for (int i = 0; i < 100000; i++) {
      assertThat(mockRestAdapter.calculateDelayForCall()).isEqualTo(2000);
    }

    mockRestAdapter.setVariancePercentage(40);
    int lowerBound = Integer.MAX_VALUE;
    int upperBound = Integer.MIN_VALUE;
    for (int i = 0; i < 100000; i++) {
      int delay = mockRestAdapter.calculateDelayForCall();
      if (delay > upperBound) {
        upperBound = delay;
      }
      if (delay < lowerBound) {
        lowerBound = delay;
      }
    }
    assertThat(upperBound).isEqualTo(2799); // ~40% above 2000
    assertThat(lowerBound).isEqualTo(1200); // ~40% below 2000
  }

  @Test public void errorVarianceIsAccurate() {
    mockRestAdapter.setDelay(2000);

    int lowerBound = Integer.MAX_VALUE;
    int upperBound = Integer.MIN_VALUE;
    for (int i = 0; i < 100000; i++) {
      int delay = mockRestAdapter.calculateDelayForError();
      if (delay > upperBound) {
        upperBound = delay;
      }
      if (delay < lowerBound) {
        lowerBound = delay;
      }
    }
    assertThat(upperBound).isEqualTo(5999); // 3 * 2000
    assertThat(lowerBound).isEqualTo(0);
  }

  @Test public void changeListenerOnlyInvokedWhenValueHasChanged() {
    long delay = mockRestAdapter.getDelay();
    int variance = mockRestAdapter.getVariancePercentage();
    int error = mockRestAdapter.getErrorPercentage();

    long newDelay = delay + 1;
    mockRestAdapter.setDelay(newDelay);
    verify(valueChangeListener).onMockValuesChanged(newDelay, variance, error);

    int newError = error + 1;
    mockRestAdapter.setErrorPercentage(newError);
    verify(valueChangeListener).onMockValuesChanged(newDelay, variance, newError);

    int newVariance = variance + 1;
    mockRestAdapter.setVariancePercentage(newVariance);
    verify(valueChangeListener).onMockValuesChanged(newDelay, newVariance, newError);

    // Now try setting the same values and ensure the listener was never called.
    mockRestAdapter.setDelay(newDelay);
    mockRestAdapter.setVariancePercentage(newVariance);
    mockRestAdapter.setErrorPercentage(newError);
    verifyNoMoreInteractions(valueChangeListener);
  }

  @Test public void syncFailureTriggersNetworkError() {
    mockRestAdapter.setErrorPercentage(100);
    mockRestAdapter.setDelay(1);

    class MockSyncExample implements SyncExample {
      @Override public Object doStuff() {
        throw new AssertionError();
      }
    }

    SyncExample mockService = mockRestAdapter.create(SyncExample.class, new MockSyncExample());

    try {
      mockService.doStuff();
      fail();
    } catch (RetrofitError e) {
      assertThat(e.isNetworkError()).isTrue();
      assertThat(e.getCause()).hasMessage("Mock network error!");
    }
  }

  @Test public void asyncFailureTriggersNetworkError() {
    mockRestAdapter.setDelay(1);
    mockRestAdapter.setErrorPercentage(100);

    class MockAsyncExample implements AsyncExample {
      @Override public void doStuff(Callback<Object> cb) {
        throw new AssertionError();
      }
    }

    AsyncExample mockService = mockRestAdapter.create(AsyncExample.class, new MockAsyncExample());

    final AtomicReference<RetrofitError> errorRef = new AtomicReference<RetrofitError>();
    mockService.doStuff(new Callback<Object>() {
      @Override public void success(Object o, Response response) {
        throw new AssertionError();
      }

      @Override public void failure(RetrofitError error) {
        errorRef.set(error);
      }
    });

    verify(httpExecutor).execute(any(Runnable.class));
    verify(callbackExecutor).execute(any(Runnable.class));

    RetrofitError error = errorRef.get();
    assertThat(error.isNetworkError()).isTrue();
    assertThat(error.getCause()).hasMessage("Mock network error!");
  }

  @Test public void syncApiIsCalledWithDelay() {
    mockRestAdapter.setDelay(100);
    mockRestAdapter.setVariancePercentage(0);
    mockRestAdapter.setErrorPercentage(0);

    final AtomicBoolean called = new AtomicBoolean();
    final Object expected = new Object();
    class MockSyncExample implements SyncExample {
      @Override public Object doStuff() {
        called.set(true);
        return expected;
      }
    }

    SyncExample mockService = mockRestAdapter.create(SyncExample.class, new MockSyncExample());

    long startNanos = System.nanoTime();
    Object actual = mockService.doStuff();
    long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

    assertThat(called.get()).isTrue();
    assertThat(actual).isEqualTo(expected);
    assertThat(tookMs).isGreaterThanOrEqualTo(100);
  }

  @Test public void asyncApiIsCalledWithDelay() {
    mockRestAdapter.setDelay(100);
    mockRestAdapter.setVariancePercentage(0);
    mockRestAdapter.setErrorPercentage(0);

    final Object expected = new Object();
    class MockAsyncExample implements AsyncExample {
      @Override public void doStuff(Callback<Object> cb) {
        cb.success(expected, null);
      }
    }

    AsyncExample mockService = mockRestAdapter.create(AsyncExample.class, new MockAsyncExample());

    final long startNanos = System.nanoTime();
    final AtomicLong tookMs = new AtomicLong();
    final AtomicReference<Object> actual = new AtomicReference<Object>();
    mockService.doStuff(new Callback<Object>() {
      @Override public void success(Object result, Response response) {
        tookMs.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        actual.set(result);
      }

      @Override public void failure(RetrofitError error) {
        throw new AssertionError();
      }
    });

    verify(httpExecutor).execute(any(Runnable.class));
    verify(callbackExecutor).execute(any(Runnable.class));

    assertThat(actual.get()).isNotNull().isSameAs(expected);
    assertThat(tookMs.get()).isGreaterThanOrEqualTo(100);
  }

  @Test public void observableApiIsCalledWithDelay() {
    mockRestAdapter.setDelay(100);
    mockRestAdapter.setVariancePercentage(0);
    mockRestAdapter.setErrorPercentage(0);

    final Object expected = new Object();
    class MockObservableExample implements ObservableExample {
      @Override public Observable<Object> doStuff() {
        return Observable.from(expected);
      }
    }

    ObservableExample mockService =
        mockRestAdapter.create(ObservableExample.class, new MockObservableExample());

    final long startNanos = System.nanoTime();
    final AtomicLong tookMs = new AtomicLong();
    final AtomicReference<Object> actual = new AtomicReference<Object>();
    Action1<Object> onSuccess = new Action1<Object>() {
      @Override public void call(Object o) {
        tookMs.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        actual.set(o);
      }
    };
    Action1<Throwable> onError = new Action1<Throwable>() {
      @Override public void call(Throwable throwable) {
        throw new AssertionError();
      }
    };

    mockService.doStuff().subscribe(onSuccess, onError);

    verify(httpExecutor, atLeastOnce()).execute(any(Runnable.class));
    verifyZeroInteractions(callbackExecutor);

    assertThat(actual.get()).isNotNull().isSameAs(expected);
    assertThat(tookMs.get()).isGreaterThanOrEqualTo(100);
  }


  @Test public void syncHttpExceptionBecomesError() {
    mockRestAdapter.setDelay(100);
    mockRestAdapter.setVariancePercentage(0);
    mockRestAdapter.setErrorPercentage(0);

    final Object expected = new Object();
    class MockSyncExample implements SyncExample {
      @Override public Object doStuff() {
        throw new MockHttpException(404, "Not Found", expected);
      }
    }

    SyncExample mockService = mockRestAdapter.create(SyncExample.class, new MockSyncExample());

    long startNanos = System.nanoTime();
    try {
      mockService.doStuff();
      fail();
    } catch (RetrofitError e) {
      long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
      assertThat(tookMs).isGreaterThanOrEqualTo(100);
      assertThat(e.isNetworkError()).isFalse();
      assertThat(e.getResponse().getStatus()).isEqualTo(404);
      assertThat(e.getResponse().getReason()).isEqualTo("Not Found");
      assertThat(e.getBody()).isSameAs(expected);
    }
  }

  @Test public void asyncHttpExceptionBecomesError() {
    mockRestAdapter.setDelay(100);
    mockRestAdapter.setVariancePercentage(0);
    mockRestAdapter.setErrorPercentage(0);

    final Object expected = new Object();
    class MockAsyncExample implements AsyncExample {
      @Override public void doStuff(Callback<Object> cb) {
        throw new MockHttpException(404, "Not Found", expected);
      }
    }

    AsyncExample mockService = mockRestAdapter.create(AsyncExample.class, new MockAsyncExample());

    final long startNanos = System.nanoTime();
    final AtomicLong tookMs = new AtomicLong();
    final AtomicReference<RetrofitError> errorRef = new AtomicReference<RetrofitError>();
    mockService.doStuff(new Callback<Object>() {
      @Override public void success(Object o, Response response) {
        throw new AssertionError();
      }

      @Override public void failure(RetrofitError error) {
        tookMs.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        errorRef.set(error);
      }
    });

    verify(httpExecutor).execute(any(Runnable.class));
    verify(callbackExecutor).execute(any(Runnable.class));

    RetrofitError error = errorRef.get();
    assertThat(tookMs.get()).isGreaterThanOrEqualTo(100);
    assertThat(error.isNetworkError()).isFalse();
    assertThat(error.getResponse().getStatus()).isEqualTo(404);
    assertThat(error.getResponse().getReason()).isEqualTo("Not Found");
    assertThat(error.getBody()).isSameAs(expected);
  }

  @Test public void observableHttpExceptionBecomesError() {
    mockRestAdapter.setDelay(100);
    mockRestAdapter.setVariancePercentage(0);
    mockRestAdapter.setErrorPercentage(0);

    final Object expected = new Object();
    class MockObservableExample implements ObservableExample {
      @Override public Observable<Object> doStuff() {
        throw new MockHttpException(404, "Not Found", expected);
      }
    }

    ObservableExample mockService =
        mockRestAdapter.create(ObservableExample.class, new MockObservableExample());

    final long startNanos = System.nanoTime();
    final AtomicLong tookMs = new AtomicLong();
    final AtomicReference<RetrofitError> errorRef = new AtomicReference<RetrofitError>();
    mockService.doStuff().subscribe(new Action1<Object>() {
      @Override public void call(Object o) {
        throw new AssertionError();
      }
    }, new Action1<Throwable>() {
      @Override public void call(Throwable error) {
        assertThat(error).isInstanceOf(RetrofitError.class);
        tookMs.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        errorRef.set((RetrofitError) error);
      }
    });

    verify(httpExecutor, atLeastOnce()).execute(any(Runnable.class));
    verifyZeroInteractions(callbackExecutor);

    RetrofitError error = errorRef.get();
    assertThat(tookMs.get()).isGreaterThanOrEqualTo(100);
    assertThat(error.isNetworkError()).isFalse();
    assertThat(error.getResponse().getStatus()).isEqualTo(404);
    assertThat(error.getResponse().getReason()).isEqualTo("Not Found");
    assertThat(error.getBody()).isSameAs(expected);
  }

  @Test public void asyncCallToFailureIsNotAllowed() {
    mockRestAdapter.setErrorPercentage(0);

    class MockAsyncExample implements AsyncExample {
      @Override public void doStuff(Callback<Object> cb) {
        cb.failure(null);
      }
    }

    AsyncExample mockService = mockRestAdapter.create(AsyncExample.class, new MockAsyncExample());

    try {
      mockService.doStuff(mock(Callback.class));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage(
          "Calling failure directly is not supported. Throw MockHttpException instead.");
    }
  }
}
