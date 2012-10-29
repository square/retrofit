// Copyright 2012 Square, Inc.
package retrofit.http;

import java.io.IOException;

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

public abstract class RestException extends RuntimeException {
  private final String url;

  protected RestException(String url, String message) {
    super(message);
    this.url = url;
  }

  protected RestException(String url, Throwable t) {
    super(t);
    this.url = url;
  }

  protected RestException(String url, String message, Throwable t) {
    super(message, t);
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  /** An exception that is the result of an HTTP response. */
  public abstract static class HttpException extends RestException {
    private final int status;
    private final Object response;

    protected HttpException(String url, int status, String message, Object response) {
      super(url, message);
      this.status = status;
      this.response = response;
    }

    protected HttpException(String url, int status, String message, ConversionException cause) {
      super(url, message, cause);
      this.status = status;
      this.response = null;
    }

    public int getStatus() {
      return status;
    }

    public Object getResponse() {
      return response;
    }
  }

  /**
   * The server returned a client error. In most cases, this is a programming error, but it can also signify a user
   * input error.
   */
  public static class ClientHttpException extends HttpException {
    public ClientHttpException(String url, int status, String message, Object response) {
      super(url, status, message, response);
    }
  }

  /**
   * We reached the server, but it encountered an error (5xx) or its response was unparseable. Please try again later.
   */
  public static class ServerHttpException extends HttpException {
    public ServerHttpException(String url, int status, String message, Object response) {
      super(url, status, message, response);
    }

    public ServerHttpException(String url, int status, String message, ConversionException cause) {
      super(url, status, message, cause);
    }
  }

  /** The session expired or the account has been disabled. Prompt the user to log in again. */
  public static class UnauthorizedHttpException extends HttpException {
    public UnauthorizedHttpException(String url, String message, Object response) {
      super(url, SC_UNAUTHORIZED, message, response);
    }
  }

  /** Couldn't reach the server. Check network settings and try again. */
  public static class NetworkException extends RestException {
    public NetworkException(String url, IOException e) {
      super(url, e);
    }
  }

  /**
   * An unexpected error occurred. Called if the framework throws an unexpected exception or if the server returns a 400
   * (Bad Request) error. In either case, the client software likely contains a bug; otherwise, the error would have
   * been caught sooner. The user should try updating their client.
   */
  public static class UnexpectedException extends RestException {
    public UnexpectedException(String url, Throwable t) {
      super(url, t);
    }
  }
}
