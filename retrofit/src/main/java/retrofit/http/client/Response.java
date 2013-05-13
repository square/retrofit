// Copyright 2013 Square, Inc.
package retrofit.http.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import retrofit.http.mime.TypedInput;

/** An HTTP response. */
public final class Response {
  private final int status;
  private final String reason;
  private final List<Header> headers;
  private final TypedInput body;

  public Response(int status, String reason, List<Header> headers, TypedInput body) {
    if (status < 200) {
      throw new IllegalArgumentException("Invalid status code: " + status);
    }
    if (reason == null) {
      throw new IllegalArgumentException("reason == null");
    }
    if (headers == null) {
      throw new IllegalArgumentException("headers == null");
    }

    this.status = status;
    this.reason = reason;
    this.headers = Collections.unmodifiableList(new ArrayList<Header>(headers));
    this.body = body;
  }

  /** Status line code. */
  public int getStatus() {
    return status;
  }

  /** Status line reason phrase. */
  public String getReason() {
    return reason;
  }

  /** An unmodifiable collection of headers. */
  public List<Header> getHeaders() {
    return headers;
  }

  /** Response body. May be {@code null}. */
  public TypedInput getBody() {
    return body;
  }
}
