// Copyright 2013 Square, Inc.
package retrofit;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import retrofit.client.Request;
import retrofit.client.Response;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static retrofit.RestAdapter.LogLevel;
import static retrofit.RetrofitError.unexpectedError;

/**
 * Wraps mock implementations of API interfaces so that they exhibit the delay and error
 * characteristics of a real network.
 * <p>
 * Because APIs are defined as interfaces, versions of the API that use mock data can be created by
 * simply implementing the API interface on a class. These mock implementations execute
 * synchronously which is a large deviation from the behavior of those backed by an API call over
 * the network. By wrapping the mock instances using this class, the interface will still use mock
 * data but exhibit the delays and errors that a real network would face.
 * <p>
 * Create an API interface and a mock implementation of it.
 * <pre>
 *   public interface UserService {
 *     &#64;GET("/user/{id}")
 *     User getUser(@Path("id") String userId);
 *   }
 *   public class MockUserService implements UserService {
 *     &#64;Override public User getUser(String userId) {
 *       return new User("Jake");
 *     }
 *   }
 * </pre>
 * Given a {@link RestAdapter} an instance of this class can be created by calling {@link #from}.
 * <pre>
 *   MockRestAdapter mockRestAdapter = MockRestAdapter.from(restAdapter);
 * </pre>
 * Instances of this class should be used as a singleton so that the behavior of every mock service
 * is consistent.
 * <p>
 * Rather than using the {@code MockUserService} directly, pass it through
 * {@link #create(Class, Object) the create method}.
 * <pre>
 *   UserService service = mockRestAdapter.create(UserService.class, new MockUserService());
 * </pre>
 * The returned {@code UserService} instance will now behave like it is happening over the network
 * while allowing the mock implementation to be written synchronously.
 * <p>
 * HTTP errors can be simulated in your mock services by throwing an instance of
 * {@link MockHttpException}. This should be done for both synchronous and asynchronous methods.
 * Do not call the {@link Callback#failure(RetrofitError) failure()} method of a callback.
 */
public final class MockRestAdapter {
  private static final int DEFAULT_DELAY_MS = 2000; // Network calls will take 2 seconds.
  private static final int DEFAULT_VARIANCE_PCT = 40; // Network delay varies by ±40%.
  private static final int DEFAULT_ERROR_PCT = 3; // 3% of network calls will fail.
  private static final int ERROR_DELAY_FACTOR = 3; // Network errors will be scaled by this value.

  /**
   * Create a new {@link MockRestAdapter} which will act as a factory for mock services. Some of
   * the configuration of the supplied {@link RestAdapter} will be used generating mock behavior.
   */
  public static MockRestAdapter from(RestAdapter restAdapter) {
    return new MockRestAdapter(restAdapter);
  }

  /** A listener invoked when the network behavior values for a {@link MockRestAdapter} change. */
  public interface ValueChangeListener {
    void onMockValuesChanged(long delayMs, int variancePct, int errorPct);

    ValueChangeListener EMPTY = new ValueChangeListener() {
      @Override public void onMockValuesChanged(long delayMs, int variancePct, int errorPct) {
      }
    };
  }

  private final RestAdapter restAdapter;
  private MockRxSupport mockRxSupport;
  final Random random = new Random();

  private ValueChangeListener listener = ValueChangeListener.EMPTY;
  private int delayMs = DEFAULT_DELAY_MS;
  private int variancePct = DEFAULT_VARIANCE_PCT;
  private int errorPct = DEFAULT_ERROR_PCT;

  private MockRestAdapter(RestAdapter restAdapter) {
    this.restAdapter = restAdapter;
  }

  /** Set a listener to be notified when any mock value changes. */
  public void setValueChangeListener(ValueChangeListener listener) {
    this.listener = listener;
  }

  private void notifyValueChangeListener() {
    listener.onMockValuesChanged(delayMs, variancePct, errorPct);
  }

  /** Set the network round trip delay, in milliseconds. */
  public void setDelay(long delayMs) {
    if (delayMs < 0) {
      throw new IllegalArgumentException("Delay must be positive value.");
    }
    if (delayMs > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Delay value too large. Max: " + Integer.MAX_VALUE);
    }
    if (this.delayMs != delayMs) {
      this.delayMs = (int) delayMs;
      notifyValueChangeListener();
    }
  }

  /** The network round trip delay, in milliseconds */
  public long getDelay() {
    return delayMs;
  }

  /** Set the plus-or-minus variance percentage of the network round trip delay. */
  public void setVariancePercentage(int variancePct) {
    if (variancePct < 0 || variancePct > 100) {
      throw new IllegalArgumentException("Variance percentage must be between 0 and 100.");
    }
    if (this.variancePct != variancePct) {
      this.variancePct = variancePct;
      notifyValueChangeListener();
    }
  }

  /** The plus-or-minus variance percentage of the network round trip delay. */
  public int getVariancePercentage() {
    return variancePct;
  }

  /** Set the percentage of calls to {@link #calculateIsFailure()} that return {@code true}. */
  public void setErrorPercentage(int errorPct) {
    if (errorPct < 0 || errorPct > 100) {
      throw new IllegalArgumentException("Error percentage must be between 0 and 100.");
    }
    if (this.errorPct != errorPct) {
      this.errorPct = errorPct;
      notifyValueChangeListener();
    }
  }

  /** The percentage of calls to {@link #calculateIsFailure()} that return {@code true}. */
  public int getErrorPercentage() {
    return errorPct;
  }

  /**
   * Randomly determine whether this call should result in a network failure.
   * <p>
   * This method is exposed for implementing other, non-Retrofit services which exhibit similar
   * network behavior. Retrofit services automatically will exhibit network behavior when wrapped
   * using {@link #create(Class, Object)}.
   */
  public boolean calculateIsFailure() {
    int randomValue = random.nextInt(100) + 1;
    return randomValue <= errorPct;
  }

  /**
   * Get the delay (in milliseconds) that should be used for triggering a network error.
   * <p>
   * Because we are triggering an error, use a random delay between 0 and three times the normal
   * network delay to simulate a flaky connection failing anywhere from quickly to slowly.
   * <p>
   * This method is exposed for implementing other, non-Retrofit services which exhibit similar
   * network behavior. Retrofit services automatically will exhibit network behavior when wrapped
   * using {@link #create(Class, Object)}.
   */
  public int calculateDelayForError() {
    return random.nextInt(delayMs * ERROR_DELAY_FACTOR);
  }

  /**
   * Get the delay (in milliseconds) that should be used for delaying a network call response.
   * <p>
   * This method is exposed for implementing other, non-Retrofit services which exhibit similar
   * network behavior. Retrofit services automatically will exhibit network behavior when wrapped
   * using {@link #create(Class, Object)}.
   */
  public int calculateDelayForCall() {
    float errorPercent = variancePct / 100f; // e.g., 20 / 100f == 0.2f
    float lowerBound = 1f - errorPercent; // 0.2f --> 0.8f
    float upperBound = 1f + errorPercent; // 0.2f --> 1.2f
    float bound = upperBound - lowerBound; // 1.2f - 0.8f == 0.4f
    float delayPercent = (random.nextFloat() * bound) + lowerBound; // 0.8 + (rnd * 0.4)
    return (int) (delayMs * delayPercent);
  }

  /**
   * Wrap the supplied mock implementation of a service so that it exhibits the delay and error
   * characteristics of a real network.
   *
   * @see #setDelay(long)
   * @see #setVariancePercentage(int)
   * @see #setErrorPercentage(int)
   */
  @SuppressWarnings("unchecked")
  public <T> T create(Class<T> service, T mockService) {
    Utils.validateServiceClass(service);
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        new MockHandler(mockService, restAdapter.getMethodInfoCache(service)));
  }

  private class MockHandler implements InvocationHandler {
    private final Object mockService;
    private final Map<Method, RestMethodInfo> methodInfoCache;

    public MockHandler(Object mockService, Map<Method, RestMethodInfo> methodInfoCache) {
      this.mockService = mockService;
      this.methodInfoCache = methodInfoCache;
    }

    @Override public Object invoke(Object proxy, Method method, final Object[] args)
        throws Throwable {
      // If the method is a method from Object then defer to normal invocation.
      if (method.getDeclaringClass() == Object.class) {
        return method.invoke(this, args);
      }

      // Load or create the details cache for the current method.
      final RestMethodInfo methodInfo = RestAdapter.getMethodInfo(methodInfoCache, method);

      if (methodInfo.isSynchronous) {
        try {
          return invokeSync(methodInfo, restAdapter.requestInterceptor, args);
        } catch (RetrofitError error) {
          Throwable newError = restAdapter.errorHandler.handleError(error);
          if (newError == null) {
            throw new IllegalStateException("Error handler returned null for wrapped exception.",
                error);
          }
          throw newError;
        }
      }

      if (restAdapter.httpExecutor == null || restAdapter.callbackExecutor == null) {
        throw new IllegalStateException("Asynchronous invocation requires calling setExecutors.");
      }
      // Apply the interceptor synchronously, recording the interception so we can replay it later.
      // This way we still defer argument serialization to the background thread.
      final RequestInterceptorTape interceptorTape = new RequestInterceptorTape();
      restAdapter.requestInterceptor.intercept(interceptorTape);

      if (methodInfo.isObservable) {
        if (mockRxSupport == null) {
          if (Platform.HAS_RX_JAVA) {
            mockRxSupport = new MockRxSupport(restAdapter);
          } else {
            throw new IllegalStateException("Observable method found but no RxJava on classpath");
          }
        }
        return mockRxSupport.createMockObservable(this, methodInfo, interceptorTape, args);
      }

      restAdapter.httpExecutor.execute(new Runnable() {
        @Override public void run() {
          invokeAsync(methodInfo, interceptorTape, args);
        }
      });
      return null; // Asynchronous methods should have return type of void.
    }

    private Request buildRequest(RestMethodInfo methodInfo, RequestInterceptor interceptor,
        Object[] args) throws Throwable {
      methodInfo.init();

      // Begin building a normal request.
      String apiUrl = restAdapter.server.getUrl();
      RequestBuilder requestBuilder = new RequestBuilder(apiUrl, methodInfo, restAdapter.converter);
      requestBuilder.setArguments(args);

      // Run it through the interceptor.
      interceptor.intercept(requestBuilder);

      Request request = requestBuilder.build();

      if (restAdapter.logLevel.log()) {
        request = restAdapter.logAndReplaceRequest("MOCK", request, args);
      }

      return request;
    }

    private Object invokeSync(RestMethodInfo methodInfo, RequestInterceptor interceptor,
        Object[] args) throws Throwable {
      Request request = buildRequest(methodInfo, interceptor, args);
      String url = request.getUrl();

      if (calculateIsFailure()) {
        sleep(calculateDelayForError());
        IOException exception = new IOException("Mock network error!");
        if (restAdapter.logLevel.log()) {
          restAdapter.logException(exception, url);
        }
        throw RetrofitError.networkError(url, exception);
      }

      LogLevel logLevel = restAdapter.logLevel;
      RestAdapter.Log log = restAdapter.log;

      int callDelay = calculateDelayForCall();
      long beforeNanos = System.nanoTime();
      try {
        Object returnValue = methodInfo.method.invoke(mockService, args);

        // Sleep for whatever amount of time is left to satisfy the network delay, if any.
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - beforeNanos);
        sleep(callDelay - tookMs);

        if (logLevel.log()) {
          log.log(String.format("<--- MOCK 200 %s (%sms)", url, callDelay));
          if (logLevel.ordinal() >= LogLevel.FULL.ordinal()) {
            log.log(returnValue + ""); // Hack to convert toString while supporting null.
            log.log("<--- END MOCK");
          }
        }

        return returnValue;
      } catch (InvocationTargetException e) {
        Throwable innerEx = e.getCause();
        if (!(innerEx instanceof MockHttpException)) {
          throw innerEx;
        }
        MockHttpException httpEx = (MockHttpException) innerEx;
        Response response = httpEx.toResponse(restAdapter.converter);

        // Sleep for whatever amount of time is left to satisfy the network delay, if any.
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - beforeNanos);
        sleep(callDelay - tookMs);

        if (logLevel.log()) {
          log.log(String.format("<---- MOCK %s %s (%sms)", httpEx.code, url, callDelay));
          if (logLevel.ordinal() >= LogLevel.FULL.ordinal()) {
            log.log(httpEx.responseBody + ""); // Hack to convert toString while supporting null.
            log.log("<--- END MOCK");
          }
        }

        throw new MockHttpRetrofitError(httpEx.reason, url, response, httpEx.responseBody,
            methodInfo.responseObjectType);
      }
    }

    private void invokeAsync(final RestMethodInfo methodInfo, RequestInterceptor interceptorTape,
        final Object[] args) {
      Request request;
      try {
        request = buildRequest(methodInfo, interceptorTape, args);
      } catch (final Throwable throwable) {
        restAdapter.callbackExecutor.execute(new Runnable() {
          @Override public void run() {
            throw new RuntimeException(throwable);
          }
        });
        return;
      }

      final String url = request.getUrl();
      final Callback callback = (Callback) args[args.length - 1];

      if (calculateIsFailure()) {
        sleep(calculateDelayForError());
        IOException exception = new IOException("Mock network error!");
        if (restAdapter.logLevel.log()) {
          restAdapter.logException(exception, url);
        }
        RetrofitError error = RetrofitError.networkError(url, exception);
        Throwable cause = restAdapter.errorHandler.handleError(error);
        final RetrofitError e = cause == error ? error : unexpectedError(error.getUrl(), cause);
        restAdapter.callbackExecutor.execute(new Runnable() {
          @Override public void run() {
            callback.failure(e);
          }
        });
        return;
      }

      final int callDelay = calculateDelayForCall();
      sleep(callDelay);

      restAdapter.callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          LogLevel logLevel = restAdapter.logLevel;
          RestAdapter.Log log = restAdapter.log;

          try {
            methodInfo.method.invoke(mockService, args);
            if (logLevel.log()) {
              log.log(String.format("<--- MOCK 200 %s (%sms)", url, callDelay));
            }
          } catch (Throwable throwable) {
            final Throwable innerEx = throwable.getCause();
            if (!(innerEx instanceof MockHttpException)) {
              if (innerEx instanceof RuntimeException) {
                throw (RuntimeException) innerEx;
              }
              throw new RuntimeException(innerEx);
            }

            MockHttpException httpEx = (MockHttpException) innerEx;
            Response response = httpEx.toResponse(restAdapter.converter);

            if (logLevel.log()) {
              log.log(String.format("<---- MOCK %s %s (%sms)", httpEx.code, url, callDelay));
              if (logLevel.ordinal() >= LogLevel.FULL.ordinal()) {
                log.log(String.valueOf(httpEx.responseBody));
                log.log("<--- END MOCK");
              }
            }

            RetrofitError error = new MockHttpRetrofitError(httpEx.getMessage(), url, response,
                httpEx.responseBody, methodInfo.responseObjectType);
            Throwable cause = restAdapter.errorHandler.handleError(error);
            final RetrofitError e = cause == error ? error : unexpectedError(error.getUrl(), cause);
            callback.failure(e);
          }
        }
      });
    }
  }

  /**
   * Waits a given number of milliseconds (of uptimeMillis) before returning. Similar to {@link
   * Thread#sleep(long)}, but does not throw {@link InterruptedException}; {@link
   * Thread#interrupt()} events are deferred until the next interruptible operation.  Does not
   * return until at least the specified number of milliseconds has elapsed.
   *
   * @param ms to sleep before returning, in milliseconds of uptime.
   */
  private static void sleep(long ms) {
    // This implementation is modified from Android's SystemClock#sleep.

    long start = uptimeMillis();
    long duration = ms;
    boolean interrupted = false;
    while (duration > 0) {
      try {
        Thread.sleep(duration);
      } catch (InterruptedException e) {
        interrupted = true;
      }
      duration = start + ms - uptimeMillis();
    }

    if (interrupted) {
      // Important: we don't want to quietly eat an interrupt() event,
      // so we make sure to re-interrupt the thread so that the next
      // call to Thread.sleep() or Object.wait() will be interrupted.
      Thread.currentThread().interrupt();
    }
  }

  private static long uptimeMillis() {
    return System.nanoTime() / 1000000L;
  }

  /** Indirection to avoid VerifyError if RxJava isn't present. */
  private static class MockRxSupport {
    private final Scheduler httpScheduler;
    private final ErrorHandler errorHandler;

    MockRxSupport(RestAdapter restAdapter) {
      httpScheduler = Schedulers.from(restAdapter.httpExecutor);
      errorHandler = restAdapter.errorHandler;
    }

    Observable createMockObservable(final MockHandler mockHandler, final RestMethodInfo methodInfo,
        final RequestInterceptor interceptor, final Object[] args) {
      return Observable.just("nothing") //
          .flatMap(new Func1<String, Observable<?>>() {
            @Override public Observable<?> call(String s) {
              try {
                return (Observable) mockHandler.invokeSync(methodInfo, interceptor, args);
              } catch (RetrofitError e) {
                return Observable.error(errorHandler.handleError(e));
              } catch (Throwable throwable) {
                return Observable.error(throwable);
              }
            }
          }).subscribeOn(httpScheduler);
    }
  }
}
