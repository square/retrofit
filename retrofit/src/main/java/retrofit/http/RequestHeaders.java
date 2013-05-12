// Copyright 2013 Square, Inc.
package retrofit.http;

import java.util.Collections;
import java.util.List;
import retrofit.http.client.Header;

/** Manages headers for each request. */
public interface RequestHeaders {
  /**
   * Get a list of headers for a request. This method will be called once for each request allowing
   * you to change the list as the state of your application changes.
   */
  List<retrofit.http.client.Header> get();

  /** Empty header list. */
  RequestHeaders NONE = new RequestHeaders() {
    @Override public List<Header> get() {
      return Collections.emptyList();
    }
  };
}
