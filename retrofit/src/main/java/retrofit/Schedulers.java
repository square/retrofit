package retrofit;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Indirect access to Scheduler API for
 */
/*package*/ final class Schedulers {

  /**
   * RetrofitScheduler, similar to the {@link rx.schedulers.EventLoopsScheduler} in the same way
   * it dumps requests onto a Executor, but we can pass in the Executor.
   * <p/>
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

    static class EventLoopScheduler extends Scheduler.Worker implements Subscription {
      private final CompositeSubscription innerSubscription = new CompositeSubscription();
      private final Executor executor;

      /* package */ EventLoopScheduler(Executor executor) {
        this.executor = executor;
      }

      @Override
      public Subscription schedule(final Action0 action) {
        if (innerSubscription.isUnsubscribed()) {
          // don't schedule, we are unsubscribed
          return Subscriptions.empty();
        }

        final AtomicReference<Subscription> sf = new AtomicReference<Subscription>();
        final Subscription s;
        if (executor instanceof ExecutorService) {
          s = Subscriptions.from(((ExecutorService) executor).submit(
                  getActionRunnable(action, sf)));
        } else {
            /*
            This is not ideal, we should use a ExecutorService, that way we can pass future
            back to the subscription, so if the user un-subscribe from the parent we can
            request the Future to cancel. This will always execute, meaning we could
            lock of the retrofit threads if a request is active for a long time.
            I would potentially force an API change to make sure this is always an ExecutorService
            */
          s = Subscriptions.empty();
          executor.execute(getActionRunnable(action, sf));
        }

        sf.set(s);
        innerSubscription.add(s);
        return s;
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
              // remove the subscription now that we're completed
              Subscription s = sf.get();
              if (s != null) innerSubscription.remove(s);
            }
          }
        };
      }

      @Override
      public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
        throw new UnsupportedOperationException("This Scheduler does not support timed requests");
      }

      @Override
      public void unsubscribe() {
        innerSubscription.unsubscribe();
      }

      @Override
      public boolean isUnsubscribed() {
        return innerSubscription.isUnsubscribed();
      }
    }
  }
}
