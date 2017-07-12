package retrofit2.adapter.rxjava2;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;
import retrofit2.Response;


/**
 * @author JSpiner (jspiner@naver.com)
 */
public class ResponseObservable<T> extends Observable<Response<T>> {
    private final Observable<Response<T>> upstream;

    ResponseObservable(Observable<Response<T>> upstream) {
        this.upstream = upstream;
    }

    @Override protected void subscribeActual(Observer<? super Response<T>> observer) {
        upstream.subscribe(new ResponseObservable.ResponseObserver<T>(observer));
    }

    private static class ResponseObserver<R> implements Observer<Response<R>> {
        private final Observer<? super Response<R>> observer;
        private boolean terminated;

        ResponseObserver(Observer<? super Response<R>> observer) {
            this.observer = observer;
        }

        @Override public void onSubscribe(Disposable disposable) {
            observer.onSubscribe(disposable);
        }

        @Override public void onNext(Response<R> response) {
            if (response.isSuccessful()) {
                observer.onNext(response);
            } else {
                terminated = true;
                Throwable t = new HttpException(response);
                try {
                    observer.onError(t);
                } catch (Throwable inner) {
                    Exceptions.throwIfFatal(inner);
                    RxJavaPlugins.onError(new CompositeException(t, inner));
                }
            }
        }

        @Override public void onComplete() {
            if (!terminated) {
                observer.onComplete();
            }
        }

        @Override public void onError(Throwable throwable) {
            if (!terminated) {
                observer.onError(throwable);
            } else {
                // This should never happen! onNext handles and forwards errors automatically.
                Throwable broken = new AssertionError(
                        "This should never happen! Report as a bug with the full stacktrace.");
                //noinspection UnnecessaryInitCause Two-arg AssertionError constructor is 1.7+ only.
                broken.initCause(throwable);
                RxJavaPlugins.onError(broken);
            }
        }
    }
}
