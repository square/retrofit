package retrofit;

import org.junit.Test;
import retrofit.client.Header;
import retrofit.client.Response;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author christopherperry
 */
public class RxSupportTest {

  @SuppressWarnings("unchecked")
  @Test public void requestObservable_shouldMakeRequest() {
    // Given
    RxSupport rxSupport = new RxSupport(null);
    Object response = new Object();

    // When
    TestSubscriber testSubscriber = new TestSubscriber();
    rxSupport.createRequestObservable(new TestCallable(response)) //
      .subscribe(testSubscriber);

    // Then
    testSubscriber.assertReceivedOnNext(Arrays.asList(response));
    testSubscriber.assertUnsubscribed();
  }

  @SuppressWarnings("unchecked")
  @Test public void requestObservable_shouldNotMakeRequest_whenUnsubscribed() {
    // Given
    RxSupport rxSupport = new RxSupport(null);
    Object response = new Object();

    // When
    TestSubscriber testSubscriber = new TestSubscriber();
    testSubscriber.unsubscribe();
    rxSupport.createRequestObservable(new TestCallable(response)) //
      .subscribe(testSubscriber);

    // Then
    List onNextEvents = testSubscriber.getOnNextEvents();
    assertThat(onNextEvents).isEmpty();
    testSubscriber.assertUnsubscribed();
  }

  @SuppressWarnings("unchecked")
  @Test public void requestObservable_shouldPassRetrofitErrorsToErrorHandlers() {
    // Given
    TestErrorHandler errorHandler = new TestErrorHandler();
    RxSupport rxSupport = new RxSupport(errorHandler);

    // When
    TestSubscriber testSubscriber = new TestSubscriber();
    ThrowingCallable request = new ThrowingCallable();
    rxSupport.createRequestObservable(request) //
      .subscribe(testSubscriber);

    // Then
    assertThat(errorHandler.error).isEqualTo(request.error);
    List onErrorEvents = testSubscriber.getOnErrorEvents();
    assertThat(onErrorEvents).hasSize(1).containsExactly(request.error);
  }

  // Simply passes back the original error
  static class TestErrorHandler implements ErrorHandler {
    private RetrofitError error;

    @Override public Throwable handleError(RetrofitError cause) {
      error = cause;
      return error;
    }
  }

  // Always throws a RetrofitError when called
  static class ThrowingCallable implements Callable<ResponseWrapper> {
    private RetrofitError error =
      new RetrofitError(null, null, null, null, false, null);

    @Override public ResponseWrapper call() throws Exception {
      throw error;
    }
  }

  // Returns a canned ResponseWrapper wrapping the response passed in when called
  static class TestCallable implements Callable<ResponseWrapper> {
    private final Object response;

    TestCallable(Object response) {
      this.response = response;
    }

    @Override public ResponseWrapper call() throws Exception {
      return new ResponseWrapper(
        new Response(
          "http://example.com", 200, "Success",
          Collections.<Header>emptyList(), null
        ), response
      );
    }
  }
}
