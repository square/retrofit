package retrofit;

import com.squareup.okhttp.Response;
import org.junit.Before;
import org.junit.Test;
import retrofit.http.GET;
import rx.Observable;
import rx.functions.Action1;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class MockServiceTest {
    interface SyncExample {
        @GET("/")
        Object doStuff();
    }


    interface AsyncExample {
        @GET("/")
        void doStuff(Callback<String> cb);
    }


    interface AsyncCallbackSubtypeExample {
        abstract class Foo implements Callback<String> {
        }

        @GET("/")
        void doStuff(Foo foo);
    }


    interface ObservableExample {
        @GET("/")
        Observable<String> doStuff();
    }


    private Executor httpExecutor;
    private Executor callbackExecutor;
    private RestAdapter restAdapter;
    private MockRestAdapter mockRestAdapter;
    private MockRestAdapter.ValueChangeListener valueChangeListener;
    private Throwable nextError;

    @Before
    public void setUp() throws IOException {
        httpExecutor = spy(new Utils.SynchronousExecutor());
        callbackExecutor = spy(new Utils.SynchronousExecutor());

        restAdapter = new RestAdapter.Builder() //
            .callbackExecutor(callbackExecutor)
            .endpoint("http://example.com")
            .errorHandler(new ErrorHandler() {
                @Override
                public Throwable handleError(RetrofitError cause) {
                    if (nextError != null) {
                        Throwable error = nextError;
                        nextError = null;
                        return error;
                    }
                    return cause;
                }
            })
            .build();

        valueChangeListener = mock(MockRestAdapter.ValueChangeListener.class);

        mockRestAdapter = MockRestAdapter.from(restAdapter, httpExecutor);
        mockRestAdapter.setValueChangeListener(valueChangeListener);

        // Seed the random with a value so the tests are deterministic.
        mockRestAdapter.random.setSeed(2847);
        mockRestAdapter.setDelay(0);
        mockRestAdapter.setVariancePercentage(0);
        mockRestAdapter.setErrorPercentage(0);
    }

    @Test
    public void syncCall() {
        final Object expected = "Hi";
        final SimpleResponseDispatcher responseDispatcher = new SimpleResponseDispatcher();
        SyncExample mockService = MockService.create(mockRestAdapter, SyncExample.class, responseDispatcher);
        responseDispatcher.setResponse(Object.class, expected);

        final AtomicReference<Object> actual = new AtomicReference<Object>();
        actual.set(mockService.doStuff());
        assertThat(actual.get()).isNotNull().isSameAs(expected);
    }

    @Test
    public void asyncCall() {
        final String expected = "Hi";

        final SimpleResponseDispatcher responseDispatcher = new SimpleResponseDispatcher();
        AsyncExample mockService = MockService.create(mockRestAdapter, AsyncExample.class, responseDispatcher);
        responseDispatcher.setResponse(String.class, expected);

        final AtomicReference<Object> actual = new AtomicReference<Object>();
        mockService.doStuff(new Callback<String>() {
            @Override
            public void success(String result, Response response) {
                actual.set(result);
            }

            @Override
            public void failure(RetrofitError error) {
                throw new AssertionError();
            }
        });

        verify(httpExecutor).execute(any(Runnable.class));
        verify(callbackExecutor).execute(any(Runnable.class));

        assertThat(actual.get()).isNotNull().isSameAs(expected);
    }

    @Test
    public void observableCall() {
        final String expected = "Hi";
        final SimpleResponseDispatcher responseDispatcher = new SimpleResponseDispatcher();
        ObservableExample mockService = MockService.create(mockRestAdapter, ObservableExample.class, responseDispatcher);
        responseDispatcher.setResponse(String.class, expected);

        final AtomicReference<Object> actual = new AtomicReference<Object>();
        Action1<Object> onSuccess = new Action1<Object>() {
            @Override
            public void call(Object o) {
                actual.set(o);
            }
        };
        Action1<Throwable> onError = new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throw new AssertionError();
            }
        };

        mockService.doStuff().subscribe(onSuccess, onError);

        verify(httpExecutor, atLeastOnce()).execute(any(Runnable.class));
        verifyZeroInteractions(callbackExecutor);

        assertThat(actual.get()).isNotNull().isSameAs(expected);
    }

    @Test
    public void asyncCanUseCallbackSubtype() {
        final String expected = "Hi";
        final SimpleResponseDispatcher responseDispatcher = new SimpleResponseDispatcher();
        AsyncExample mockService = MockService.create(mockRestAdapter, AsyncExample.class, responseDispatcher);
        responseDispatcher.setResponse(String.class, expected);

        final AtomicReference<Object> actual = new AtomicReference<Object>();

        mockService.doStuff(new AsyncCallbackSubtypeExample.Foo() {
            @Override
            public void success(String result, Response response) {
                actual.set(result);
            }

            @Override
            public void failure(RetrofitError error) {
                throw new AssertionError();
            }
        });

        assertThat(actual.get()).isNotNull().isEqualTo(expected);
    }
}
