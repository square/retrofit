// Copyright 2013 Square, Inc.
package retrofit.http;

import java.util.Collections;
import java.util.List;

/** Manages headers for each request. */
public interface HeaderPairs {
  /**
   * Get a list of headers for a request. This method will be called once for each request allowing
   * you to change the list as the state of your application changes.
   */
  List<HeaderPair> get();

  /** Empty header list. */
  HeaderPairs NONE = new HeaderPairs() {
    @Override public List<HeaderPair> get() {
      return Collections.emptyList();
    }
  };
}
