// Copyright 2014 Square, Inc.
package retrofit;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

/** An {@link Observable} of a single value for use with {@link MockRestAdapter}. */
public final class MockObservable<T> extends Observable<T> {
  /** Create instance from the specified value. */
  public static <T> MockObservable<T> from(T value) {
    return new MockObservable<T>(value);
  }

  final T value;

  private MockObservable(final T value) {
    super(new OnSubscribeFunc<T>() {
      @Override public Subscription onSubscribe(Observer<? super T> observer) {
        observer.onNext(value);
        observer.onCompleted();
        return Subscriptions.empty();
      }
    });
    this.value = value;
  }
}
