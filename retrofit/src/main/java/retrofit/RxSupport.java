package retrofit;

import rx.Observable;
import rx.Subscriber;

import java.util.concurrent.Callable;

/**
 * Utilities for supporting RxJava Observables.
 * <p>
 * RxJava might not be on the available to use. Check {@link Platform#HAS_RX_JAVA} before calling.
 */
final class RxSupport {
  private final ErrorHandler errorHandler;

  RxSupport(ErrorHandler errorHandler) {
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
          subscriber.onNext(wrapper.responseBody);
          subscriber.onCompleted();
        } catch (RetrofitError e) {
          subscriber.onError(errorHandler.handleError(e));
        } catch (Exception e) {
          // This is from the Callable.  It shouldn't actually throw.
          throw new RuntimeException(e);
        }
      }
    });
  }
}
