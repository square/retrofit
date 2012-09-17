// Copyright 2010 Square, Inc.
package retrofit.http;

/**
 * Server information. Applications may extend this class and return different URLs over time.
 * Callers should always consult the Server instance for the latest values rather than caching URLs.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public class Server {

  public static final String DEFAULT_TYPE = "production";

  private final String apiUrl;
  private final String type;

  public Server(String apiUrl) {
    this(apiUrl, DEFAULT_TYPE);
  }

  public Server(String apiUrl, String type) {
    if (!apiUrl.endsWith("/")) {
      apiUrl += "/";
    }
    this.apiUrl = apiUrl;
    this.type = type;
  }

  /** Gets the base API url. Includes a trailing '/'. */
  public String apiUrl() {
    return apiUrl;
  }

  /** Gets a human-readable server type for differentiating between multiple instances. */
  public String type() {
    return type;
  }
}
