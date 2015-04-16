package retrofit;

import com.squareup.okhttp.Request;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;

/**
 * Utilities for supporting RxJava Observables.
 * <p>
 * RxJava might not be on the available to use. Check {@link Platform#HAS_RX_JAVA} before calling.
 */
final class RxSupport {
  interface Invoker {
    void invoke(Request request, Callback callback);

    interface Callback {
      void next(Object o);
      void error(Throwable t);
    }
  }

  RxSupport() {
  }

  Observable createRequestObservable(final Invoker invoker, final RequestBuilder requestBuilder,
      final RequestInterceptor requestInterceptor) {
    return Observable.defer(new Func0<Observable<Object>>() {

      @Override public Observable<Object> call() {
        // Run interceptor, which was deferred until subscription.
        requestInterceptor.intercept(requestBuilder);

        return Observable.create(new Observable.OnSubscribe<Object>() {
          @Override public void call(final Subscriber<? super Object> subscriber) {
            invoker.invoke(requestBuilder.build(), new Invoker.Callback() {
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

    });
  }

}
