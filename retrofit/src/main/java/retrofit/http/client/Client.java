package retrofit.http.client;

import java.io.IOException;

/**
 * Abstraction of an HTTP client which can execute {@link Request Requests}. This class must be
 * thread-safe as invocation may happen from multiple threads simultaneously.
 */
public interface Client {
  /**
   * Synchronously execute an HTTP represented by {@code request} and encapsulate all response data
   * into a {@link Response} instance.
   */
  Response execute(Request request) throws IOException;

  /**
   * Deferred means of obtaining a {@link Client}. For asynchronous requests this will always be
   * called on a background thread.
   */
  interface Provider {
    /** Obtain an HTTP client. Called once for each request. */
    Client get();
  }
}
