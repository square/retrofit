// Copyright 2013 Square, Inc.
package retrofit;

import org.junit.Before;
import org.junit.Test;
import retrofit.client.Client;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.http.GET;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

public class RetryerTest {

  interface ExampleClient {
    @GET("/")
    Response throwsRetryableException();
  }

  static class TestException extends Exception {
  }

  /* An HTTP client which returns a 500 response a couple times. */
  static class MockServerErrorClient implements Client {
    int i = 0;

    @Override
    public Response execute(Request request) throws IOException {
      if (i++ < 2) {
        return new Response(500, "temporary error", Collections.<Header>emptyList(), null);
      }
      return new Response(200, "OK", Collections.<Header>emptyList(), null);
    }
  }

  ExampleClient client;
  Retryer retryer;

  @Before
  public void setup() {
    retryer = mock(Retryer.class);

    client = new RestAdapter.Builder() //
        .setServer("http://example.com")
        .setClient(new MockServerErrorClient())
        .setErrorHandler(new ErrorHandler() {
          @Override
          public Throwable handleError(RetrofitError cause) {
            Response r = cause.getResponse();
            if (r.getStatus() == 500) {
              return new RetryableError(cause);
            }
            return cause;
          }
        })
        .setRetryerFactory(new Retryer.Factory() {
          @Override
          public Retryer create() {
            return retryer;
          }
        })
        .setExecutors(new Utils.SynchronousExecutor(), new Utils.SynchronousExecutor())
        .build()
        .create(ExampleClient.class);
  }

  @Test
  public void retriesTransientFailure() throws Throwable {
    doNothing().when(retryer).continueOrPropagate(any(RetryableError.class));
    client.throwsRetryableException();
  }

  @Test(expected = RetryableError.class)
  public void only5TriesAllowedAndExponentialBackoff() throws Exception {
    RetryableError e = new RetryableError(null);
    Retryer.Default retryer = (Retryer.Default) Retryer.DEFAULT_FACTORY.create();

    assertThat(retryer.attempt).isEqualTo(1);
    assertThat(retryer.sleptForMillis).isZero();

    retryer.continueOrPropagate(e);
    assertThat(retryer.attempt).isEqualTo(2);
    assertThat(retryer.sleptForMillis).isEqualTo(150);

    retryer.continueOrPropagate(e);
    assertThat(retryer.attempt).isEqualTo(3);
    assertThat(retryer.sleptForMillis).isEqualTo(375);

    retryer.continueOrPropagate(e);
    assertThat(retryer.attempt).isEqualTo(4);
    assertThat(retryer.sleptForMillis).isEqualTo(712);

    retryer.continueOrPropagate(e);
    assertThat(retryer.attempt).isEqualTo(5);
    assertThat(retryer.sleptForMillis).isEqualTo(1218);

    retryer.continueOrPropagate(e);
    // fail
  }

  @Test
  public void considersRetryAfterButNotMoreThanMaxPeriod() throws Exception {
    Retryer.Default retryer = new Retryer.Default() {
      protected long currentTimeMillis() {
        return 0;
      }
    };

    retryer.continueOrPropagate(new RetryableError(null, new Date(5000)));
    assertThat(retryer.attempt).isEqualTo(2);
    assertThat(retryer.sleptForMillis).isEqualTo(1000);
  }
}
