package retrofit;

/**
 * Represents an API endpoint URL and associated name. Callers should always consult the instance
 * for the latest values rather than caching the returned values.
 *
 * @author Matt Hickman (mhickman@palantir.com)
 * @see Server, ChangeableServer
 */
public interface IServer {

  /** The base API URL. */
  public String getUrl();

  /** A name for differentiating between multiple API URLs. */
  public String getName();

}
