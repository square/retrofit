// Copyright 2010 Square, Inc.
package retrofit.http;

import org.apache.http.HttpMessage;

/**
 * HTTP header setting strategy.
 *
 * @author Eric Burke (eric@squareup.com)
 */
public interface Headers {

  /** Sets headers on the given message */
  void setOn(HttpMessage message);
}
