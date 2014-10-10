package retrofit;

/**
 * Static factory methods for creating {@link Endpoint} instances.
 *
 * @author Matt Hickman (mhickman@palantir.com)
 */
public final class Endpoints {
  private Endpoints() {
  }

  /** Create a server with the provided URL. */
  public static Endpoint newFixedEndpoint(final String url) {
    return new Endpoint() {
      @Override public String getUrl() {
        return url;
      }
    };
  }
}
