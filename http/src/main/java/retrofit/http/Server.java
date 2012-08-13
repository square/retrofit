// Copyright 2010 Square, Inc.
package retrofit.http;

import java.util.Properties;

/**
 * Server information. Applications may extend this class and return different URLs over time.
 * Callers should always consult the Server instance for the latest values rather than caching URLs.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public class Server {

  private final String apiUrl;
  private final String webUrl;
  private final boolean ignoreSslWarnings;

  protected Server(String apiUrl, String webUrl, boolean ignoreSslWarnings) {
    this.apiUrl = apiUrl;
    this.webUrl = webUrl;
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

  public String getType() {
    return "production";
  }

  /**
   * Returns true if we should ignore SSL warnings. Returns false by default.
   * Ignored for development servers.
   */
  public boolean ignoreSslWarnings() {
    return ignoreSslWarnings;
  }

  /**
   * Creates a Server instance from Properties. Requires "api.url" and
   * "web.url". "disable.ssl.warnings" is optional.
   */
  public static Server from(Properties properties) {
    String apiUrl = properties.getProperty("api.url");
    if (apiUrl == null) throw new NullPointerException("Missing api.url.");
    String webUrl = properties.getProperty("web.url");
    if (webUrl == null) throw new NullPointerException("Missing web.url.");
    String disableSslWarnings = properties.getProperty("disable.ssl.warnings",
        "false");
    return new Server(apiUrl, webUrl, Boolean.valueOf(disableSslWarnings));
  }
}
