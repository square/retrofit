// Copyright 2010 Square, Inc.
package retrofit.http;

import org.apache.http.HttpMessage;

/**
 * HTTP header setting strategy.
 *
 * @author Eric Burke (eric@squareup.com)
 */
public interface Headers {

  /** Sets headers on the given message, with the specified MIME type */
  void setOn(HttpMessage message, String mimeType);
}
