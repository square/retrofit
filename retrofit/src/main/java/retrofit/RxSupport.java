package retrofit;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

/**
 * Utilities for supporting RxJava Observables.
 * Used primarily by {@link retrofit.RestAdapter}.
 *
 * Remember RxJava might not be on the classpath, check its included before calling, use
 * {@link Platform#HAS_RX_JAVA}
 */
final class RxSupport {
  private final Executor executor;
  private final ErrorHandler errorHandler;

  RxSupport(Executor executor, ErrorHandler errorHandler) {
    this.executor = executor;
    this.errorHandler = errorHandler;
  }

  Observable createRequestObservable(final Callable<ResponseWrapper> request) {
    return Observable.create(new Observable.OnSubscribe<Object>() {
      @Override public void call(Subscriber<? super Object> subscriber) {
        if (subscriber.isUnsubscribed()) {
          return;
        }
        final FutureTask<Void> task = new FutureTask<Void>(getRunnable(subscriber, request), null);
        final Subscription s = Subscriptions.from(task);
        // We add our subscription to the current subscriber so the future task can be
        // unSubscribed from from thegc
        subscriber.add(s);
        executor.execute(task);
      }
    });
  }

  private Runnable getRunnable(final Subscriber<? super Object> subscriber,
                               final Callable<ResponseWrapper> request) {
    return new Runnable() {
      @Override public void run() {
        try {
          if (subscriber.isUnsubscribed()) {
            return;
          }
          ResponseWrapper wrapper = request.call();
          subscriber.onNext(wrapper.responseBody);
          subscriber.onCompleted();
        } catch (RetrofitError e) {
          subscriber.onError(errorHandler.handleError(e));
        } catch (Exception e) {
          // This is from the Callable.  It shouldn't actually throw.
          throw new RuntimeException(e);
        }
      }
    };
  }

}
