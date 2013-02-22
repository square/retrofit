package retrofit.http;

/** A {@link Server} whose URL and name can be changed at runtime. */
public class ChangeableServer extends Server {
  private String url;
  private String name;

  /** Create a changeable server with the provided URL and default name. */
  public ChangeableServer(String url) {
    super(url);
    this.url = url;
    this.name = DEFAULT_NAME;
  }

  /** Create a changeable server with the provided URL and name. */
  public ChangeableServer(String url, String name) {
    super(url, name);
    this.url = url;
    this.name = name;
  }

  /** Update the URL returned by {@link #getUrl()}. */
  public void update(String url) {
    this.url = url;
  }

  /** Update the URL and name returned by {@link #getUrl()} and {@link #getName()}, respetively. */
  public void update(String url, String name) {
    this.url = url;
    this.name = name;
  }

  @Override public String getUrl() {
    return url;
  }

  @Override public String getName() {
    return name;
  }
}
