package retrofit.http.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import retrofit.http.Header;
import retrofit.http.mime.TypedOutput;

/** Encapsulates all of the information necessary to make an HTTP request. */
public final class Request {
  private final String method;
  private final String url;
  private final List<Header> headers;
  private final TypedOutput body;

  public Request(String method, String url, List<Header> headers, TypedOutput body) {
    if (method == null) {
      throw new NullPointerException("Method must not be null.");
    }
    if (url == null) {
      throw new NullPointerException("URL must not be null.");
    }
    this.method = method;
    this.url = url;

    if (headers == null) {
      this.headers = Collections.emptyList();
    } else {
      this.headers = Collections.unmodifiableList(new ArrayList<Header>(headers));
    }

    this.body = body;
  }

  /** HTTP method verb. */
  public String getMethod() {
    return method;
  }

  /** Target URL. */
  public String getUrl() {
    return url;
  }

  /** Returns an unmodifiable list of headers.empty, never {@code null}. */
  public List<Header> getHeaders() {
    return headers;
  }

  /** Returns the request body or {@code null}. */
  public TypedOutput getBody() {
    return body;
  }
}
