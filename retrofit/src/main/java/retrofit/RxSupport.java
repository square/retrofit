package retrofit;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Utilities for supporting RxJava Observables.
 * Used primarily by {@link retrofit.RestAdapter}.
 *
 * Remember RxJava might not be on the classpath, check its included before calling, use
 * {@link Platform#HAS_RX_JAVA}
 */
final class RxSupport {
  private final Scheduler scheduler;
  private final ErrorHandler errorHandler;

  RxSupport(Executor executor, ErrorHandler errorHandler) {
    this.scheduler = new RetrofitScheduler(executor);
    this.errorHandler = errorHandler;
  }

  Observable createRequestObservable(final Callable<ResponseWrapper> request) {
    return Observable.create(new Observable.OnSubscribe<Object>() {
      @Override public void call(Subscriber<? super Object> subscriber) {
        if (subscriber.isUnsubscribed()) {
          return;
        }
        try {
          ResponseWrapper wrapper = request.call();
          if (subscriber.isUnsubscribed()) {
            return;
          }
          subscriber.onNext(wrapper.responseBody);
          subscriber.onCompleted();
        } catch (RetrofitError e) {
          subscriber.onError(errorHandler.handleError(e));
        } catch (Exception e) {
          // This is from the Callable.  It shouldn't actually throw.
          throw new RuntimeException(e);
        }
      }
    }).subscribeOn(scheduler);
  }


  /**
   * RetrofitScheduler, similar to the {@link rx.schedulers.EventLoopsScheduler} in the same way
   * it dumps requests onto a Executor, but we can pass in the Executor.
   *
   * This does not support Scheduled execution, which may cause issues with peoples implementations.
   * If they are doing, wait() or debouncing() on this scheduler. Future implementations, should
   * either add {@code schedule()} support, or let the user provide the {@link rx.Scheduler} to
   * RestAdapter builder.
   */
  static class RetrofitScheduler extends Scheduler {
    private final Executor executorService;

    /*package*/ RetrofitScheduler(Executor executorService) {
      this.executorService = executorService;
    }

    @Override
    public Worker createWorker() {
      return new EventLoopScheduler(executorService);
    }

    static class EventLoopScheduler extends Worker implements Subscription {
      private final CompositeSubscription innerSubscription = new CompositeSubscription();
      private final Executor executor;

      EventLoopScheduler(Executor executor) {
        this.executor = executor;
      }

      @Override
      public Subscription schedule(final Action0 action) {
        if (innerSubscription.isUnsubscribed()) {
          // Don't schedule, we are un-subscribed.
          return Subscriptions.empty();
        }

        final AtomicReference<Subscription> sf = new AtomicReference<Subscription>();
        final FutureTask<Void> futureTask =
                new FutureTask<Void>(getActionRunnable(action, sf), null);
        final Subscription s = Subscriptions.from(futureTask);
        executor.execute(futureTask);

        sf.set(s);
        innerSubscription.add(s);
        return s;
      }

      @Override
      public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
        throw new UnsupportedOperationException("This Scheduler does not support timed Actions");
      }

      @Override
      public void unsubscribe() {
        innerSubscription.unsubscribe();
      }

      @Override
      public boolean isUnsubscribed() {
        return innerSubscription.isUnsubscribed();
      }

      private Runnable getActionRunnable(final Action0 action,
                                         final AtomicReference<Subscription> sf) {
        return new Runnable() {
          @Override
          public void run() {
            try {
              if (innerSubscription.isUnsubscribed()) return;
              action.call();
            } finally {
              // Remove the subscription now that we've completed.
              Subscription s = sf.get();
              if (s != null) innerSubscription.remove(s);
            }
          }
        };
      }
    }
  }
}
