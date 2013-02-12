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
}
