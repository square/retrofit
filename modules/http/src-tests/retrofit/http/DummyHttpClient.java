// Copyright 2010 Square, Inc.
package retrofit.http;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Support for dummy HttpClients.
 *
 * @author Bob Lee (bob@squareup.com)
 */
class DummyHttpClient implements HttpClient {
  public <T> T execute(HttpUriRequest request,
      ResponseHandler<? extends T> responseHandler)
      throws IOException, ClientProtocolException {
    throw new UnsupportedOperationException();
  }
  public HttpParams getParams() { throw new UnsupportedOperationException(); }
  public ClientConnectionManager getConnectionManager() {
    throw new UnsupportedOperationException();
  }
  public HttpResponse execute(HttpUriRequest request) {
    throw new UnsupportedOperationException();
  }
  public HttpResponse execute(HttpUriRequest request, HttpContext context) {
    throw new UnsupportedOperationException();
  }
  public HttpResponse execute(HttpHost target, HttpRequest request) {
    throw new UnsupportedOperationException();
  }
  public HttpResponse execute(HttpHost target, HttpRequest request,
      HttpContext context) {
    throw new UnsupportedOperationException();
  }
  public <T> T execute(HttpUriRequest request,
      ResponseHandler<? extends T> responseHandler, HttpContext context) {
    throw new UnsupportedOperationException();
  }
  public <T> T execute(HttpHost target, HttpRequest request,
      ResponseHandler<? extends T> responseHandler) {
    throw new UnsupportedOperationException();
  }
  public <T> T execute(HttpHost target, HttpRequest request,
      ResponseHandler<? extends T> responseHandler, HttpContext context) {
    throw new UnsupportedOperationException();
  }
}
