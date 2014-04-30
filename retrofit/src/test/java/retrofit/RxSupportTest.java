package retrofit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

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

public class RxSupportTest {

  private Object response;
  private ResponseWrapper responseWrapper;
  private Callable<ResponseWrapper> callable = spy(new Callable<ResponseWrapper>() {
    @Override public ResponseWrapper call() throws Exception {
      return responseWrapper;
    }
  });

  private QueuedSynchronousExecutor executor;
  private ErrorHandler errorHandler;
  private RxSupport rxSupport;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    response = new Object();
    responseWrapper = new ResponseWrapper(
            new Response(
                    "http://example.com", 200, "Success",
                    Collections.<Header>emptyList(), mock(TypedInput.class)
            ), response
    );
    executor = spy(new QueuedSynchronousExecutor());
    errorHandler = ErrorHandler.DEFAULT;
    rxSupport = new RxSupport(executor, errorHandler);
  }

  @Mock
  Observer<Object> subscriber;

  @Test
  public void testObservableCallsOnNextOnHttpExecutor() throws Exception {
    rxSupport.createRequestObservable(callable).subscribe(subscriber);
    executor.executeNextInQueue();
    verify(subscriber, times(1)).onNext(response);
  }

  @Test
  public void testObservableCallsOnNextOnHttpExecutorWithSubscriber() throws Exception {
    TestScheduler test = Schedulers.test();
    rxSupport.createRequestObservable(callable).subscribeOn(test).subscribe(subscriber);
    // Subscription is handled via the Scheduler.
    test.triggerActions();
    // This will only execute up to the executor in OnSubscribe.
    verify(subscriber, never()).onNext(any());
    // Upon continuing the executor we then run the retrofit request.
    executor.executeNextInQueue();
    verify(subscriber, times(1)).onNext(response);
  }

  @Test
  public void testObservableUnSubscribesDoesNotExecuteCallable() throws Exception {
    Subscription subscription = rxSupport.createRequestObservable(callable).subscribe(subscriber);
    verify(subscriber, never()).onNext(any());

    // UnSubscribe here should cancel the queued runnable.
    subscription.unsubscribe();

    executor.executeNextInQueue();
    verify(callable, never()).call();
    verify(subscriber, never()).onNext(response);
  }

  @Test
  public void testObservableCallsOperatorsOffHttpExecutor() throws Exception {
    TestScheduler test = Schedulers.test();
    rxSupport.createRequestObservable(callable)
            .delaySubscription(1000, TimeUnit.MILLISECONDS, test)
            .subscribe(subscriber);

    verify(subscriber, never()).onNext(any());
    test.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
    // Upon continuing the executor we then run the retrofit request.
    executor.executeNextInQueue();
    verify(subscriber, times(1)).onNext(response);
  }

  @Test
  public void testObservableDoesNotLockExecutor() throws Exception {
    TestScheduler test = Schedulers.test();
    Subscription subscription1 = rxSupport.createRequestObservable(callable)
            .delay(1000, TimeUnit.MILLISECONDS, test)
            .subscribe(subscriber);

    Subscription subscription2 = rxSupport.createRequestObservable(callable)
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

  @Test
  public void testObservableRespectsObserveOn() throws Exception {
    TestScheduler observe = Schedulers.test();
    rxSupport.createRequestObservable(callable)
            .observeOn(observe)
            .subscribe(subscriber);

    verify(subscriber, never()).onNext(any());
    executor.executeNextInQueue();

    // Should have no response yet, but callback should of been executed.
    verify(subscriber, never()).onNext(any());
    verify(callable, times(1)).call();

    // Forward the Observable Scheduler
    observe.triggerActions();
    verify(subscriber, times(1)).onNext(response);
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