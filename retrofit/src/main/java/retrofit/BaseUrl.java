package retrofit;

import com.squareup.okhttp.HttpUrl;

/** The base URL of the remote service. */
public interface BaseUrl {
  /**
   * The base URL.
   * <p>
   * Consumers will call this method every time they need to create a request allowing values
   * to change over time.
   */
  HttpUrl url();
}
