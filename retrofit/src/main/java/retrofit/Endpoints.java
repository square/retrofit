package retrofit;

/**
 * Static factory methods for creating {@link Endpoint} instances.
 *
 * @author Matt Hickman (mhickman@palantir.com)
 */
public final class Endpoints {
  private static final String DEFAULT_NAME = "default";

  private Endpoints() {
  }

  /** Create a server with the provided URL. */
  public static Endpoint newFixedEndpoint(String url) {
    return new FixedEndpoint(url, DEFAULT_NAME);
  }

  /** Create an endpoint with the provided URL and name. */
  public static Endpoint newFixedEndpoint(String url, String name) {
    return new FixedEndpoint(url, name);
  }

  private static class FixedEndpoint implements Endpoint {
    private final String apiUrl;
    private final String name;

    FixedEndpoint(String apiUrl, String name) {
      this.apiUrl = apiUrl;
      this.name = name;
    }

    @Override public String getUrl() {
      return apiUrl;
    }

    @Override public String getName() {
      return name;
    }
  }
}
