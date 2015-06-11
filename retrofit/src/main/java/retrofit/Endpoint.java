package retrofit;

import com.squareup.okhttp.HttpUrl;

/** An API endpoint. */
public interface Endpoint {
  /**
   * The base URL.
   * <p>
   * Consumers will call this method every time they need to create a request allowing values
   * to change over time.
   */
  HttpUrl url();
}
