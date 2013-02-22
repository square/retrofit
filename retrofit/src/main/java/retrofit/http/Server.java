// Copyright 2010 Square, Inc.
package retrofit.http;

/**
 * Represents an API endpoint URL and associated name. Callers should always consult the instance
 * for the latest values rather than caching the returned values.
 *
 * @author Bob Lee (bob@squareup.com)
 * @see ChangeableServer
 */
public class Server {
  public static final String DEFAULT_NAME = "default";

  private final String apiUrl;
  private final String type;

  /** Create a server with the provided URL and default name. */
  public Server(String apiUrl) {
    this(apiUrl, DEFAULT_NAME);
  }

  /** Create a server with the provided URL and name. */
  public Server(String apiUrl, String type) {
    this.apiUrl = apiUrl;
    this.type = type;
  }

  /** The base API URL. */
  public String getUrl() {
    return apiUrl;
  }

  /** A name for differentiating between multiple API URLs. */
  public String getName() {
    return type;
  }
}
