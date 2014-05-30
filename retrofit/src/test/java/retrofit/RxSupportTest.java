package retrofit;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit.client.Header;
import retrofit.client.Response;
import retrofit.mime.TypedInput;
import rx.Observer;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static retrofit.RequestInterceptor.RequestFacade;
import static retrofit.RxSupport.Invoker;

public class RxSupportTest {

  private Object response;
  private ResponseWrapper responseWrapper;
  private Invoker invoker = spy(new Invoker() {
    @Override public ResponseWrapper invoke(RequestInterceptor requestInterceptor) {
      return responseWrapper;
    }
  });
  private RequestInterceptor requestInterceptor = spy(new RequestInterceptor() {
    @Override public void intercept(RequestFacade request) {
    }
  });

  private QueuedSynchronousExecutor executor;
  private RxSupport rxSupport;

  @Mock Observer<Object> subscriber;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
    response = new Object();
    responseWrapper = new ResponseWrapper(
            new Response(
                    "http://example.com", 200, "Success",
                    Collections.<Header>emptyList(), mock(TypedInput.class)
            ), response
    );
    executor = spy(new QueuedSynchronousExecutor());
    rxSupport = new RxSupport(executor, ErrorHandler.DEFAULT, requestInterceptor);
  }

  @Test public void observableCallsOnNextOnHttpExecutor() {
    rxSupport.createRequestObservable(invoker).subscribe(subscriber);
    executor.executeNextInQueue();
    verify(subscriber, times(1)).onNext(response);
  }

  @Test public void observableCallsOnNextOnHttpExecutorWithSubscriber() {
    TestScheduler test = Schedulers.test();
    rxSupport.createRequestObservable(invoker).subscribeOn(test).subscribe(subscriber);
    // Subscription is handled via the Scheduler.
    test.triggerActions();
    // This will only execute up to the executor in OnSubscribe.
    verify(subscriber, never()).onNext(any());
    // Upon continuing the executor we then run the retrofit request.
    executor.executeNextInQueue();
    verify(subscriber, times(1)).onNext(response);
  }

  @Test public void observableUnSubscribesDoesNotExecuteCallable() throws Exception {
    Subscription subscription = rxSupport.createRequestObservable(invoker).subscribe(subscriber);
    verify(subscriber, never()).onNext(any());

    // UnSubscribe here should cancel the queued runnable.
    subscription.unsubscribe();

    executor.executeNextInQueue();
    verify(invoker, never()).invoke(any(RequestInterceptor.class));
    verify(subscriber, never()).onNext(response);
  }

  @Test public void observableCallsOperatorsOffHttpExecutor() {
    TestScheduler test = Schedulers.test();
    rxSupport.createRequestObservable(invoker)
            .delaySubscription(1000, TimeUnit.MILLISECONDS, test)
            .subscribe(subscriber);

    verify(subscriber, never()).onNext(any());
    test.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
    // Upon continuing the executor we then run the retrofit request.
    executor.executeNextInQueue();
    verify(subscriber, times(1)).onNext(response);
  }

  @Test public void observableDoesNotLockExecutor() {
    TestScheduler test = Schedulers.test();
    rxSupport.createRequestObservable(invoker)
            .delay(1000, TimeUnit.MILLISECONDS, test)
            .subscribe(subscriber);

    rxSupport.createRequestObservable(invoker)
            .delay(2000, TimeUnit.MILLISECONDS, test)
            .subscribe(subscriber);

    // Nothing fired yet
    verify(subscriber, never()).onNext(any());
    // Subscriptions should of been queued up and executed even tho we delayed on the Subscriber.
    executor.executeNextInQueue();
    executor.executeNextInQueue();

    verify(subscriber, never()).onNext(response);

    test.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
    verify(subscriber, times(1)).onNext(response);

    test.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
    verify(subscriber, times(2)).onNext(response);
  }

  @Test public void observableRespectsObserveOn() throws Exception {
    TestScheduler observe = Schedulers.test();
    rxSupport.createRequestObservable(invoker)
            .observeOn(observe)
            .subscribe(subscriber);

    verify(subscriber, never()).onNext(any());
    executor.executeNextInQueue();

    // Should have no response yet, but callback should of been executed.
    verify(subscriber, never()).onNext(any());
    verify(invoker, times(1)).invoke(any(RequestInterceptor.class));

    // Forward the Observable Scheduler
    observe.triggerActions();
    verify(subscriber, times(1)).onNext(response);
  }

  @Test public void observableCallsInterceptorForEverySubscription() throws Exception {
    rxSupport.createRequestObservable(invoker).subscribe(subscriber);
    rxSupport.createRequestObservable(invoker).subscribe(subscriber);

    // The interceptor should have been called for each request upon subscription.
    verify(requestInterceptor, times(2)).intercept(any(RequestFacade.class));

    // Background execution of the requests should not touch the interceptor.
    executor.executeAll();
    verifyNoMoreInteractions(requestInterceptor);
  }

  /**
   * Test Executor to iterate through Executions to aid in checking
   * that the Observable implementation is correct.
   */
  static class QueuedSynchronousExecutor implements Executor {
    Deque<Runnable> runnableQueue = new ArrayDeque<Runnable>();

    @Override public void execute(Runnable runnable) {
      runnableQueue.add(runnable);
    }

    /**
     * Will throw exception if you are expecting something to be added to the Executor
     * and it hasn't.
     */
    void executeNextInQueue() {
      runnableQueue.removeFirst().run();
    }

    /**
     * Executes any queued executions on the executor.
     */
    void executeAll() {
      Iterator<Runnable> iterator = runnableQueue.iterator();
      while (iterator.hasNext()) {
        Runnable next = iterator.next();
        next.run();
        iterator.remove();
      }
    }
  }
}
