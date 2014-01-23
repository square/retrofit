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
  public static Endpoint newFixedEndpoint(String url) {
    return new Server(url);
  }

  /** Create an endpoint with the provided URL and name. */
  public static Endpoint newFixedEndpoint(String url, String name) {
    return new Server(url, name);
  }

}
