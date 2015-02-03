package retrofit;

import rx.Observable;
import rx.Subscriber;

/**
 * Utilities for supporting RxJava Observables.
 * <p>
 * RxJava might not be on the available to use. Check {@link Platform#HAS_RX_JAVA} before calling.
 */
final class RxSupport {
  interface Invoker {
    void invoke(Callback callback);

    interface Callback {
      void next(Object o);
      void error(Throwable t);
    }
  }

  RxSupport() {
  }

  Observable createRequestObservable(final Invoker invoker) {
    return Observable.create(new Observable.OnSubscribe<Object>() {
      @Override public void call(final Subscriber<? super Object> subscriber) {
        invoker.invoke(new Invoker.Callback() {
          @Override public void next(Object o) {
            if (!subscriber.isUnsubscribed()) {
              subscriber.onNext(o);
              subscriber.onCompleted();
            }
          }

          @Override public void error(Throwable t) {
            if (!subscriber.isUnsubscribed()) {
              subscriber.onError(t);
            }
          }
        });
      }
    });
  }
}
