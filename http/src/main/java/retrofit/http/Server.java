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
  private final String webUrl;
  private final String type;
  private final boolean ignoreSslWarnings;

  public Server(String apiUrl, String webUrl) {
    this(apiUrl, webUrl, false);
  }

  public Server(String apiUrl, String webUrl, boolean ignoreSslWarnings) {
    this(apiUrl, webUrl, DEFAULT_TYPE, ignoreSslWarnings);
  }

  public Server(String apiUrl, String webUrl, String type, boolean ignoreSslWarnings) {
    if (!apiUrl.endsWith("/")) {
      apiUrl += "/";
    }
    this.apiUrl = apiUrl;

    if (!webUrl.endsWith("/")) {
      webUrl += "/";
    }
    this.webUrl = webUrl;

    this.type = type;
    this.ignoreSslWarnings = ignoreSslWarnings;
  }

  /**
   * Gets the base API url. Includes a trailing '/'.
   */
  public String apiUrl() {
    return apiUrl;
  }

  /**
   * Gets the base URL for Square's web site. Includes a trailing '/'.
   */
  public String webUrl() {
    return webUrl;
  }

  /**
   * Gets a human-readable server type for differentiating between multiple instances.
   */
  public String type() {
    return type;
  }

  /**
   * Returns true if we should ignore SSL warnings. Returns false by default.
   * Ignored for development servers.
   */
  public boolean ignoreSslWarnings() {
    return ignoreSslWarnings;
  }
}
