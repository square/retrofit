package retrofit2.adapter.rxjava;

import rx.Completable;
import rx.Observable;

/**
 * A class that allows {@link Observable} and {@link Completable} to be transformed before
 * returning then from {@link RxJavaCallAdapterFactory}.
 *
 * For example, you can use the {@link Transformer} to apply a observeOn or subscribeOn {@link rx.Scheduler}
 */
public interface Transformer {
    <T> Observable<T> transform(Observable<T> observable);
    Completable transform(Completable completable);
}
