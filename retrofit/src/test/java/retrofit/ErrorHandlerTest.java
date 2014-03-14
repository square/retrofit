// Copyright 2013 Square, Inc.
package retrofit;

import java.io.IOException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import retrofit.client.Client;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.http.GET;
import rx.Observable;
import rx.Observer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ErrorHandlerTest {

  interface ExampleClient {
    @GET("/")
    Response throwsCustomException() throws TestException;

    @GET("/")
    void onErrorWrappedCustomException(Callback<Response> callback);

    @GET("/")
    Observable<Response> onErrorCustomException();
  }

  static class TestException extends Exception {
  }

  /* An HTTP client which always returns a 400 response */
  static class MockInvalidResponseClient implements Client {
    @Override public Response execute(Request request) throws IOException {
      return new Response("", 400, "invalid request", Collections.<Header>emptyList(), null);
    }
  }

  ExampleClient client;
  ErrorHandler errorHandler;

  @Before public void setup() {
    errorHandler = mock(ErrorHandler.class);

    client = new RestAdapter.Builder() //
        .setEndpoint("http://example.com")
        .setClient(new MockInvalidResponseClient())
        .setErrorHandler(errorHandler)
        .setExecutors(new Utils.SynchronousExecutor(), new Utils.SynchronousExecutor())
        .build()
        .create(ExampleClient.class);
  }

  @Test public void customizedExceptionUsed() throws Throwable {
    TestException exception = new TestException();
    doReturn(exception).when(errorHandler).handleError(any(RetrofitError.class));

    try {
      client.throwsCustomException();
      failBecauseExceptionWasNotThrown(TestException.class);
    } catch (TestException e) {
      assertThat(e).isSameAs(exception);
    }
  }

  @Test public void onErrorWrappedCustomException() throws Throwable {
    final TestException exception = new TestException();
    doReturn(exception).when(errorHandler).handleError(any(RetrofitError.class));

    client.onErrorWrappedCustomException(new Callback<Response>() {

      @Override public void success(Response response, Response response2) {
        failBecauseExceptionWasNotThrown(TestException.class);
      }

      @Override public void failure(RetrofitError error) {
        assertThat(error.getCause()).isSameAs(exception);
      }
    });
  }

  @Test public void onErrorCustomException() throws Throwable {
    final TestException exception = new TestException();
    doReturn(exception).when(errorHandler).handleError(any(RetrofitError.class));

    client.onErrorCustomException().subscribe(new Observer<Response>() {
      @Override public void onCompleted() {
        failBecauseExceptionWasNotThrown(TestException.class);
      }

      @Override public void onError(Throwable e) {
        assertThat(e).isSameAs(exception);
      }

      @Override public void onNext(Response response) {
        failBecauseExceptionWasNotThrown(TestException.class);
      }
    });
  }

  @Test public void returningNullThrowsException() throws Exception {
    doReturn(null).when(errorHandler).handleError(any(RetrofitError.class));

    try {
      client.throwsCustomException();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Error handler returned null for wrapped exception.");
    }
  }
}
