// Copyright 2010 Square, Inc.
package retrofit.http;

/**
 * Thrown if the server behaves unexpectedly. Typically, this indicates an
 * intermittent problem; the request can be retried again later.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public class ServerException extends Exception {

  public ServerException(String detailMessage) {
    super(detailMessage);
  }

  public ServerException(Throwable throwable) {
    super(throwable);
  }
}
