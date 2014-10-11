package retrofit;

/**
 * Represents an API endpoint URL and associated name. Callers should always consult the instance
 * for the latest values rather than caching the returned values.
 *
 * @author Matt Hickman (mhickman@palantir.com)
 */
public interface Endpoint {
  /** The base API URL. */
  String getUrl();
}
