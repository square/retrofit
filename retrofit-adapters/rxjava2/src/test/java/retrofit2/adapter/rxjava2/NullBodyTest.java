package retrofit2.adapter.rxjava2;

import hu.akarnokd.rxjava2.debug.SavedHooks;
import hu.akarnokd.rxjava2.debug.validator.RxJavaProtocolValidator;
import io.reactivex.Completable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.http.GET;

public class NullBodyTest {
    @Rule
    public final MockWebServer server = new MockWebServer();
    private final TestScheduler scheduler = new TestScheduler();

    interface Api {
        @GET("/")
        Completable doStuff();
    }

    @Test
    public void nullBody() {
        SavedHooks hooks = RxJavaProtocolValidator.enableAndChain();

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(new OkHttpClient())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(scheduler))
                .build();

        final Api api = retrofit.create(Api.class);

        server.enqueue(new MockResponse());
        TestObserver<Void> observer = api.doStuff().test();

        scheduler.triggerActions();

        observer
                .assertComplete()
                .assertNoErrors();


        hooks.restore();
    }
}


