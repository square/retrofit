package retrofit;

import com.squareup.okhttp.HttpUrl;

import static retrofit.Utils.checkNotNull;

/** An API endpoint. */
public abstract class Endpoint {
  /** Create an endpoint with the provided {@code url}. */
  public static Endpoint createFixed(String url) {
    checkNotNull(url, "url == null");
    if (url.trim().length() == 0) {
      throw new IllegalArgumentException("Empty URL");
    }
    final HttpUrl httpUrl = HttpUrl.parse(url);
    if (httpUrl == null) {
      throw new IllegalArgumentException("Invalid URL: " + url);
    }
    return new Endpoint() {
      @Override public HttpUrl url() {
        return httpUrl;
      }
    };
  }

  /**
   * The base URL.
   * <p>
   * Consumers will call this method every time they need to create a request allowing values
   * to change over time.
   */
  public abstract HttpUrl url();
}
