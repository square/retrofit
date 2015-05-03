package retrofit;

import com.darylteo.rx.promises.java.Promise;
import com.squareup.okhttp.Response;

public class RetrofitPromise<T> extends Promise<T> implements Callback<T> {

    /**
     * Constructor.
     */

    public RetrofitPromise() {

    }

    /**
     * Executed when the request is performed with the success.
     *
     * @param responseObject The {@link T} object.
     * @param response The {@link Response}.
     */

    @Override
    public void success(T responseObject, Response response) {
        this.fulfill(responseObject);
    }

    /**
     * Executed when the request gives an error.
     *
     * @param error The {@link retrofit.RetrofitError}.
     */

    @Override
    public void failure(RetrofitError error) {
        this.reject(error.getCause());
    }
}
