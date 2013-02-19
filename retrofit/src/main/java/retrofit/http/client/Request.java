package retrofit.http.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import retrofit.http.Header;
import retrofit.http.mime.TypedOutput;

/** Encapsulates all of the information necessary to make an HTTP request. */
public final class Request {
  private final String method;
  private final String url;
  private final List<Header> headers;
  private final boolean isMultipart;
  private final TypedOutput body;
  private Map<String, TypedOutput> bodyParameters;

  public Request(String method, String url, List<Header> headers, boolean isMultipart,
      TypedOutput body, Map<String, TypedOutput> bodyParameters) {
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
      this.headers = Collections.unmodifiableList(headers);
    }

    this.isMultipart = isMultipart;
    this.body = body;

    if (bodyParameters != null) {
      bodyParameters = Collections.unmodifiableMap(bodyParameters);
    }
    this.bodyParameters = bodyParameters;
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

  /** {@code true} if the request body is multipart. */
  public boolean isMultipart() {
    return isMultipart;
  }

  /**
   * Returns the request body for non-multipart requests, or {@code null} if the request has no
   * body.
   */
  public TypedOutput getBody() {
    return body;
  }

  /** Unmodifiable map of additional body parameters for multipart requests. */
  public Map<String, TypedOutput> getBodyParameters() {
    return bodyParameters;
  }
}
